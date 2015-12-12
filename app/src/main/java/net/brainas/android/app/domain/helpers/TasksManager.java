package net.brainas.android.app.domain.helpers;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.domain.models.*;
import net.brainas.android.app.infrustructure.TaskDbHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Kit Ushakov on 11/9/2015.
 */
public class TasksManager {

    private TaskDbHelper taskDbHelper;

    public enum GROUP_OF_TASKS {
        ALL,
        ACTIVE,
        WAITING,
        USED
    }

    private ArrayList<Task> waitingList = new ArrayList<>();
    private ArrayList<Task> activeList = new ArrayList<>();

    public TasksManager(TaskDbHelper taskDbHelper) {
        this.taskDbHelper = taskDbHelper;
        fillInWLFromDB();
        fiilInALFromDB();
    }

    public void addTasksToWaitingList(List<Task> tasks) {
        synchronized (waitingList) {
            waitingList.addAll(tasks);
        }
    }

    public void addTasksToActiveList(List<Task> tasks) {
        synchronized (activeList) {
            activeList.addAll(tasks);
        }
    }

    public void cleanActiveList() {
        synchronized (activeList) {
            activeList.clear();
        }
    }

    public void cleanWaitingList() {
        synchronized (waitingList) {
            waitingList.clear();
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

    public ArrayList<Task> getTasksFromDB(Map<String, Object> params) {
        BrainasApp app = (BrainasApp)BrainasApp.getAppContext();
        TaskDbHelper taskDbHelper = app.getTaskDbHelper();
        ArrayList<Task> tasks = taskDbHelper.getTasks(params);

        /* TODO */
        //Task task1 = new Task(1,1,"test1");
        //task1.setImage(true);
        //tasks.add(task1);
        //tasks.add(new Task(2, 2, "test2"));
        //tasks.add(new Task(3, 3, "test3 big big big so big test3 big big big so big1 1234 1234")); //60
        //tasks.add(new Task(4, 4, "test4"));

        return tasks;
    }

    public Task getTaskById(long id) {
        Map<String,Object> params = new HashMap<>();
        params.put("TASK_ID", id);
        Task task = getTasksFromDB(params).get(0);
        return task;
    }

    public ArrayList<Task> fiilInALFromDB() {
        Map<String,Object> params = new HashMap<>();
        params.put("GROUP_OF_TASKS", GROUP_OF_TASKS.ACTIVE);
        ArrayList<Task> activeTasks = this.getTasksFromDB(params);
        cleanActiveList();
        addTasksToActiveList(activeTasks);
        return activeTasks;
    }

    public ArrayList<Task> fillInWLFromDB() {
        Map<String,Object> params = new HashMap<>();
        params.put("GROUP_OF_TASKS", TasksManager.GROUP_OF_TASKS.WAITING);
        ArrayList<Task> waitingTasks = this.getTasksFromDB(params);
        cleanWaitingList();
        addTasksToWaitingList(waitingTasks);
        return waitingTasks;
    }
}
