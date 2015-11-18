package net.brainas.android.app;

import android.app.Application;
import android.content.Context;

import net.brainas.android.app.activities.MainActivity;
import net.brainas.android.app.domain.helpers.TasksManager;
import net.brainas.android.app.infrustructure.TaskDbHelper;

/**
 * Created by Kit Ushakov on 11/9/2015.
 */
public class BrainasApp extends Application {
    private static Context context;

    private MainActivity mainActivity;
    private TasksManager tasksManager;
    private TaskDbHelper taskDbHelper;

    public void onCreate() {
        super.onCreate();
        BrainasApp.context = getApplicationContext();
        this.tasksManager = new TasksManager();
        this.taskDbHelper = new TaskDbHelper(context);
    }

    public static Context getAppContext() {
        return BrainasApp.context;
    }

    public void setMainActivity(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    public MainActivity getMainActivity() {return this.mainActivity;}

    public TasksManager getTasksManager(){
        return this.tasksManager;
    }

    public TaskDbHelper getTaskDbHelper() {return this.taskDbHelper;}
}
