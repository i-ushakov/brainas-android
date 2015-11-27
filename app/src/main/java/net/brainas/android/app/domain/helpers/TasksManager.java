package net.brainas.android.app.domain.helpers;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.domain.models.*;
import net.brainas.android.app.infrustructure.TaskDbHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Kit Ushakov on 11/9/2015.
 */
public class TasksManager {
    public enum GROUP_OF_TASKS {
        ALL,
        ACTIVE,
        USED
    }

    public TasksManager() {}

    public List<Task> getTasksFromDB(Map<String, Object> params) {
        BrainasApp app = (BrainasApp)BrainasApp.getAppContext();
        TaskDbHelper taskDbHelper = app.getTaskDbHelper();
        List<Task> tasks = taskDbHelper.getTasks(params);

        /* TODO */
        Task task1 = new Task(1,1,"test1");
        task1.setImage(true);
        tasks.add(task1);
        tasks.add(new Task(2,2,"test2"));
        tasks.add(new Task(3,3,"test3 big big big so big test3 big big big so big1 1234 1234")); //60
        tasks.add(new Task(4,4,"test4"));

        return tasks;
    }
}
