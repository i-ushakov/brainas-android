package net.brainas.android.app;

import android.app.Application;
import android.content.Context;
import android.os.Handler;

import net.brainas.android.app.UI.logic.TilesManager;
import net.brainas.android.app.activities.MainActivity;
import net.brainas.android.app.domain.helpers.ActivationManager;
import net.brainas.android.app.domain.helpers.NotificationManager;
import net.brainas.android.app.domain.helpers.TasksManager;
import net.brainas.android.app.domain.models.Task;
import net.brainas.android.app.infrustructure.TaskDbHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Kit Ushakov on 11/9/2015.
 */
public class BrainasApp extends Application {
    private static Context context;

    private MainActivity mainActivity;
    public TasksManager tasksManager;
    private TilesManager tilesManager;
    private NotificationManager notificationManager;
    private TaskDbHelper taskDbHelper;
    private ActivationManager activationManager;

    public void onCreate() {
        super.onCreate();
        BrainasApp.context = getApplicationContext();
        tasksManager = new TasksManager();
        notificationManager = new NotificationManager();
        taskDbHelper = new TaskDbHelper(context);
        tasksManager.fillInWLFromDB();
        tasksManager.fiilInALFromDB();
        activationManager = new ActivationManager(tasksManager);
        activationManager.initCheckConditionsInWL();
    }

    public static Context getAppContext() {
        return BrainasApp.context;
    }

    public void setMainActivity(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    public MainActivity getMainActivity() {
        return this.mainActivity;
    }

    public TasksManager getTasksManager() {
        return this.tasksManager;
    }

    public void setTilesManager(TilesManager tilesManager) {
        this.tilesManager = tilesManager;
    }

    public TilesManager getTilesManager() {
        return tilesManager;
    }

    public NotificationManager getNotificationManager(){ return this.notificationManager; }

    public TaskDbHelper getTaskDbHelper() {
        return this.taskDbHelper;
    }

}