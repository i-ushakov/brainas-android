package net.brainas.android.app.domain.helpers;

import android.widget.ArrayAdapter;

import net.brainas.android.app.AccountsManager;
import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.domain.models.*;
import net.brainas.android.app.infrustructure.TaskChangesDbHelper;
import net.brainas.android.app.infrustructure.TaskDbHelper;
import net.brainas.android.app.infrustructure.UserAccount;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Kit Ushakov on 11/9/2015.
 */
public class TasksManager implements AccountsManager.SingInObserver {

    private BrainasApp app;
    private TaskDbHelper taskDbHelper;
    private HashMap<Long, Task> tasksHashMap = new HashMap<>();
    private Integer accountId = null;

    public enum GROUP_OF_TASKS {
        ALL,
        ACTIVE,
        WAITING,
        USED
    }

    private ArrayList<Task> waitingList = new ArrayList<>();
    private ArrayList<Task> activeList = new ArrayList<>();

    public TasksManager(TaskDbHelper taskDbHelper) {
        ((BrainasApp)BrainasApp.getAppContext()).getAccountsManager().attach(this);
        app = ((BrainasApp)BrainasApp.getAppContext());
        accountId = app.getAccountsManager().getCurrenAccountId();

        this.taskDbHelper = taskDbHelper;
        //fillInWLFromDB();
        //fiilInALFromDB();
    }

    public void addTasksToWaitingList(List<Task> tasks) {
        if (waitingList != null) {
            synchronized (waitingList) {
                waitingList.addAll(tasks);
            }
        }
    }

    public void addTasksToActiveList(List<Task> tasks) {
        if (activeList != null) {
            synchronized (activeList) {
                activeList.addAll(tasks);
            }
        }
    }

    public void cleanActiveList() {
        if (activeList != null) {
            synchronized (activeList) {
                activeList.clear();
            }
        }
    }

    public void cleanWaitingList() {
        if (waitingList != null) {
            synchronized (waitingList) {
                waitingList.clear();
            }
        }
    }

    public ArrayList<Task> getWaitingList() {
        waitingList = fillInWLFromDB();
        return waitingList;
    }

    public ArrayList<Task> getActiveList() {
        activeList = fiilInALFromDB();
        return activeList;
    }

    public ArrayList<Task> getTasksFromDB(Map<String, Object> params, int accountId) {
        BrainasApp app = (BrainasApp)BrainasApp.getAppContext();
        TaskDbHelper taskDbHelper = app.getTaskDbHelper();
        ArrayList<Task> tasks = taskDbHelper.getTasks(params, accountId);
        tasks = objectsMapping(tasks);
        /* TODO */
        //Task task1 = new Task(1,1,"test1");
        //task1.setImage(true);
        //tasksHashMap.add(task1);
        //tasksHashMap.add(new Task(2, 2, "test2"));
        //tasksHashMap.add(new Task(3, 3, "test3 big big big so big test3 big big big so big1 1234 1234")); //60
        //tasksHashMap.add(new Task(4, 4, "test4"));
        //taskDbHelper.close();
        return tasks;
    }

    public Task getTaskByLocalId(long id) {
        accountId = app.getAccountsManager().getCurrenAccountId();
        // We cannot get tasks if we don't know current user account id
        if (accountId == null) {
            return null;
        }
        Map<String,Object> params = new HashMap<>();
        params.put("TASK_ID", id);
        Task task = null;
        List<Task> tasks = getTasksFromDB(params, accountId);
        if (tasks.size() > 0) {
            task = tasks.get(0);
        }
        return task;
    }

    public Task getTaskByGlobalId(long globalId) {
        // We cannot get tasks if we don't know current user account id
        if (accountId == null) {
            return null;
        }
        Map<String,Object> params = new HashMap<>();
        params.put("TASK_GLOBAL_ID", globalId);
        Task task = null;
        List<Task> tasks = getTasksFromDB(params, accountId);
        if (tasks.size() > 0) {
            task = tasks.get(0);
        } else {
            // TODO (For example, situation when task , was deleted through a web interface, and was removed from android database after synchronization)
        }
        return task;
    }

    public ArrayList<Task> fiilInALFromDB() {
        // We cannot get tasks if we don't know current user account id
        if (accountId == null) {
            return null;
        }
        Map<String,Object> params = new HashMap<>();
        params.put("GROUP_OF_TASKS", GROUP_OF_TASKS.ACTIVE);
        ArrayList<Task> activeTasks = this.getTasksFromDB(params, accountId);
        cleanActiveList();
        addTasksToActiveList(activeTasks);
        return activeTasks;
    }

    public ArrayList<Task> fillInWLFromDB() {
        // We cannot get tasks if we don't know current user account id
        if (accountId == null) {
            return null;
        }
        Map<String,Object> params = new HashMap<>();
        params.put("GROUP_OF_TASKS", TasksManager.GROUP_OF_TASKS.WAITING);
        ArrayList<Task> waitingTasks = this.getTasksFromDB(params, accountId);
        cleanWaitingList();
        addTasksToWaitingList(waitingTasks);
        return waitingTasks;
    }

    public boolean removeTask(Task task) {
        CopyOnWriteArrayList<Task.TaskChangesObserver> observers =  task.getTaskChangesObservers();
        if(taskDbHelper.deleteTaskById(task.getId())) {
            for (Task.TaskChangesObserver observer: observers) {
                observer.updateAfterTaskWasChanged();
            }
            if (task.getGlobalId() != 0) {
                ((BrainasApp) BrainasApp.getAppContext()).getTasksChangesDbHelper().loggingChanges(task, "DELETED");
            }
            task = null;
            return true;
        } else {
            return false;
        }
    }

    public boolean restoreTaskToWaiting(long takId) {
        Task task = getTaskByLocalId(takId);
        if (conditionsValidation(task.getConditions())) {
            task.changeStatus(Task.STATUSES.WAITING);
            return true;
        }
        return false;
    }

    public boolean conditionsValidation(ArrayList<Condition> conditions) {
        if (conditions.size() == 0) {
            return false;
        }
        for (Condition condition : conditions) {
            if(!condition.isValid()) {
                return false;
            }
        }
        return true;
    }

    public void deleteTaskById(int taskId) {
        BrainasApp app = (BrainasApp)BrainasApp.getAppContext();
        TaskDbHelper taskDbHelper = app.getTaskDbHelper();
        taskDbHelper.deleteTaskById(taskId);
        //taskDbHelper.close();
    }

    public void deleteTaskByGlobalId(int taskGlobalId) {
        BrainasApp app = (BrainasApp)BrainasApp.getAppContext();
        TaskDbHelper taskDbHelper = app.getTaskDbHelper();
        TaskChangesDbHelper taskChangesDbHelper = app.getTasksChangesDbHelper();
        long localIdOfDeletedTask = taskDbHelper.deleteTaskByGlobalId(taskGlobalId);
        taskChangesDbHelper.deleteTaskChangesById(localIdOfDeletedTask);
    }

    public void updateAfterSingIn(UserAccount userAccount) {
        accountId = userAccount.getLocalAccountId();
    }

    public void updateAfterSingOut() {
        accountId = null;
    }

    public Event retriveEventFromTaskById(Task task, long eventId) {
        ArrayList<Condition> conditions = task.getConditions();
        for(Condition condition : conditions) {
            ArrayList<Event> events = condition.getEvents();
            for(Event event : events) {
                if (event.getId() == eventId){
                    return event;
                }
            }
        }
        return null;
    }

    private ArrayList<Task> objectsMapping(ArrayList<Task> dbTasks) {
        ArrayList<Task> mappedTasks = new ArrayList<> ();
        for (Task dbTask : dbTasks){
            long taskId = dbTask.getId();
            if(tasksHashMap.containsKey(taskId)) {
                Task refreshedTask = tasksHashMap.get(taskId);
                refreshedTask = refreshTaskObject(refreshedTask, dbTask);
                mappedTasks.add(refreshedTask);
            } else {
                mappedTasks.add(dbTask);
                tasksHashMap.put(taskId, dbTask);
            }
        }
        //tasksHashMap.clear();
        //for(Task mappedTask : mappedTasks) {
            //tasksHashMap.put(mappedTask.getId(), mappedTask);
        //}
        return mappedTasks;
    }

    private Task refreshTaskObject(Task heapTask, Task dbTask) {
        heapTask.setMessage(dbTask.getMessage());
        heapTask.setDescription(dbTask.getDescription());
        heapTask.setConditions(dbTask.getConditions());
        return heapTask;
    }
}
