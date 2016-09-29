package net.brainas.android.app.domain.helpers;

import android.util.Log;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.domain.models.*;
import net.brainas.android.app.infrustructure.TaskChangesDbHelper;
import net.brainas.android.app.infrustructure.TaskDbHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Kit Ushakov on 11/9/2015.
 */
public class TasksManager {
    private static String TASK_MANAGER_TAG = "TASK_MANAGER";

    private BrainasApp app;
    private TaskDbHelper taskDbHelper;
    private TaskChangesDbHelper taskChangesDbHelper;
    private HashMap<Long, Task> tasksHashMap = new HashMap<>();
    private Integer accountId = null;

    public enum GROUP_OF_TASKS {
        ALL,
        ACTIVE,
        WAITING,
        TODO,
        USED
    }

    private ArrayList<Task> waitingList = new ArrayList<>();
    private ArrayList<Task> activeList = new ArrayList<>();

    public TasksManager(TaskDbHelper taskDbHelper, TaskChangesDbHelper taskChangesDbHelper, Integer accountId) {
        this.accountId = accountId;
        this.taskDbHelper = taskDbHelper;
        this.taskChangesDbHelper = taskChangesDbHelper;
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

    public ArrayList<Task> getAllTasksFromHeap() {
        ArrayList<Task> tasks = new ArrayList<>();
        for (long key: tasksHashMap.keySet()) {
            tasks.add(tasksHashMap.get(key));
        }
        return tasks;
    }

    /*
     * Getting tasks derectly from Database
     */
    public ArrayList<Task> getAllTasks() {
        ArrayList<Task> tasks = getTasksFromDB(null, null);
        return tasks;
    }

    public ArrayList<Task> getTasksFromDB(HashMap<String, Object> params, Integer accountId) {
        if (accountId == null) {
            accountId = this.accountId;
        }
        //BrainasApp app = (BrainasApp)BrainasApp.getAppContext();
        //TaskDbHelper taskDbHelper = app.getTaskDbHelper();
        ArrayList<Task> tasks = taskDbHelper.getTasks(params, accountId);
        tasks = objectsMapping(tasks);
        /* TODO */
        //Task task1 = new Task(1,1,"test1");
        //task1.setPicture(true);
        //tasksHashMap.add(task1);
        //tasksHashMap.add(new Task(2, 2, "test2"));
        //tasksHashMap.add(new Task(3, 3, "test3 big big big so big test3 big big big so big1 1234 1234")); //60
        //tasksHashMap.add(new Task(4, 4, "test4"));
        //taskDbHelper.close();
        return tasks;
    }

    public Task getTaskByLocalId(long id) {
        // We cannot get tasks if we don't know current user account id
        if (accountId == null) {
            return null;
        }
        HashMap<String,Object> params = new HashMap<>();
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
        HashMap<String,Object> params = new HashMap<>();
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
        HashMap<String,Object> params = new HashMap<>();
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
        HashMap<String,Object> params = new HashMap<>();
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
                ((BrainasApp) BrainasApp.getAppContext()).getTasksChangesDbHelper().loggingChanges(task, accountId, "DELETED");
            }
            if(tasksHashMap.containsKey(task.getId())) {
                tasksHashMap.remove(task.getId());
            }
            CopyOnWriteArrayList<Condition> conditions = task.getConditions();
            Iterator<Condition> iterator = conditions.iterator();
            while (iterator.hasNext()) {
                Condition condition =iterator.next();
                ArrayList<Event> events = condition.getEvents();
                Event event = events.get(0);
                event.setParent(null);
                events.clear();
                condition.setParent(null);
            }
            conditions.clear();
            return true;
        } else {
            return false;
        }
    }

    public boolean restoreTask(long takId) {
        Task task = getTaskByLocalId(takId);
        if (task != null) {
            if (task.getConditions().size() == 0) {
                this.changeStatus(task, Task.STATUSES.TODO);
                return true;
            }
            if (conditionsValidation(task.getConditions())) {
                this.changeStatus(task, Task.STATUSES.WAITING);
                return true;
            }
        }
        return false;
    }

    public boolean conditionsValidation(CopyOnWriteArrayList<Condition> conditions) {
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

    public void deleteTaskByLocalId(int taskId) {
        //BrainasApp app = (BrainasApp)BrainasApp.getAppContext();
        //TaskDbHelper taskDbHelper = app.getTaskDbHelper();
        taskDbHelper.deleteTaskById(taskId);
        //taskDbHelper.close();
    }

    public void deleteTaskByGlobalId(int taskGlobalId) {
        HashMap<String,Object> params = new HashMap<>();
        params.put("TASK_GLOBAL_ID", taskGlobalId);
        List<Task> tasks = taskDbHelper.getTasks(params, accountId);
        if (tasks.size() > 0) {
            Task task = taskDbHelper.getTasks(params, accountId).get(0);
            long localIdOfDeletedTask = task.getId();
            removeTask(task);
            taskChangesDbHelper.deleteTaskChangesById(localIdOfDeletedTask);
            Log.i(TASK_MANAGER_TAG, "Task with loacal id " + localIdOfDeletedTask + "was removed by request from server for task with global id " + taskGlobalId);
        }
    }

    public Event retriveEventFromTaskById(Task task, long eventId) {
        CopyOnWriteArrayList<Condition> conditions = task.getConditions();
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

    public void addToMappedTasks(Task task) {
        ArrayList<Task> oneTaskArray = new ArrayList<Task>();
        oneTaskArray.add(task);
        objectsMapping(oneTaskArray);
    }

    public void changeStatus(Task task, Task.STATUSES newStatus) {
        task.setStatus(newStatus);
        saveTask(task);
    }

    public void saveTask(Task task) {
        saveTask(task, true, true);
    }

    public void saveTask(Task task, boolean needToNotify, boolean needToLoggingChanges){
        task.checkStatus();
        //TaskDbHelper taskDbHelper = ((BrainasApp)BrainasApp.getAppContext()).getTaskDbHelper();
        long taskId = taskDbHelper.addOrUpdateTask(task);
        task.setId(taskId);
        addToHeap(task);

        if (needToLoggingChanges) {
            //TaskChangesDbHelper taskChangesDbHelper = ((BrainasApp)BrainasApp.getAppContext()).getTasksChangesDbHelper();
            taskChangesDbHelper.loggingChanges(task, this.accountId);
        }
        if (needToNotify) {
            task.notifyAllObservers();
        }
    }

    public void saveCondition(Condition condition) {
        if (condition.getParent()!= null) {
            this.saveTask(condition.getParent());
        }
    }

    public void saveEvent(Event event) {
        if (event.getParent() != null) {
            this.saveCondition(event.getParent());
        }
    }

    public int getAccpuntId() {
        return this.accountId;
    }

    public void prepareToCloseApp() {
        Iterator it = tasksHashMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            Task task = (Task)pair.getValue();
            task.detachAllObservers();
        }
        for(Task task : activeList) {
            task.detachAllObservers();
        }
    }

    private ArrayList<Task> objectsMapping(ArrayList<Task> tasks) {
        ArrayList<Task> mappedTasks = new ArrayList<> ();
        long taskId;
        Task mappedTask;
        for (Task task : tasks){
            taskId = task.getId();
            if(tasksHashMap.containsKey(taskId)) {
                mappedTask = tasksHashMap.get(taskId);
                mappedTask = refreshTaskObject(mappedTask, task);
                mappedTasks.add(mappedTask);
            } else {
                mappedTasks.add(task);
                tasksHashMap.put(taskId, task);
            }
        }
        //tasksHashMap.clear();
        //for(Task mappedTask : mappedTasks) {
            //tasksHashMap.put(mappedTask.getId(), mappedTask);
        //}
        return mappedTasks;
    }

    private Task refreshTaskObject(Task heapTask, Task freshTask) {
        if (!heapTask.equals(freshTask)) {
            heapTask.setGlobalId(freshTask.getGlobalId());
            heapTask.setMessage(freshTask.getMessage());
            heapTask.setDescription(freshTask.getDescription());
            heapTask.setConditions(freshTask.getConditions());
            heapTask.setPicture(freshTask.getPicture());
            heapTask.setStatus(freshTask.getStatus());
        }
        return heapTask;
    }

    private void addToHeap(Task task) {
        long taskId = task.getId();
        if(!tasksHashMap.containsKey(taskId)) {
            tasksHashMap.put(taskId, task);
        } else {
            refreshTaskObject(tasksHashMap.get(taskId), task);
        }
    }
}
