package net.brainas.android.app;

import android.app.Application;
import android.content.Context;

import net.brainas.android.app.UI.logic.ReminderScreenManager;
import net.brainas.android.app.activities.MainActivity;
import net.brainas.android.app.domain.helpers.ActivationManager;
import net.brainas.android.app.domain.helpers.NotificationManager;
import net.brainas.android.app.domain.helpers.TasksManager;
import net.brainas.android.app.infrustructure.TaskDbHelper;

/**
 * Created by Kit Ushakov on 11/9/2015.
 */
public class BrainasApp extends Application {
    private static Context context;

    // Activities
    private MainActivity mainActivity;

    // Domain Layer Managers
    private TasksManager tasksManager;
    private ActivationManager activationManager;
    private NotificationManager notificationManager;

    // UI Layer Managers
    private ReminderScreenManager reminderScreenManager;

    // Infrastructure Layer Managers
    private TaskDbHelper taskDbHelper;

    public void onCreate() {
        super.onCreate();
        BrainasApp.context = getApplicationContext();
        tasksManager = new TasksManager();
        activationManager = new ActivationManager(tasksManager);
        notificationManager = new NotificationManager();
        taskDbHelper = new TaskDbHelper(context);
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

    public void setReminderScreenManager(ReminderScreenManager reminderScreenManager) {
        this.reminderScreenManager = reminderScreenManager;
    }

    public ReminderScreenManager getReminderScreenManager() {
        return reminderScreenManager;
    }

    public NotificationManager getNotificationManager(){ return this.notificationManager; }

    public TaskDbHelper getTaskDbHelper() {
        return this.taskDbHelper;
    }

}