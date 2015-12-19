package net.brainas.android.app.domain.helpers;

import android.location.Location;
import android.os.Handler;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.UI.logic.ReminderScreenManager;
import net.brainas.android.app.activities.TasksActivity;
import net.brainas.android.app.domain.models.Task;
import net.brainas.android.app.infrustructure.GPSProvider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by innok on 12/1/2015.
 */
public class ActivationManager {
    static final int CHECK_CONDITIONS_START_TIME = 20000;
    static final int CHECK_CONDITIONS_INTERVAL = 20000;
    private Timer timer;
    private TimerTask task;
    private final Handler handler = new Handler();
    private Object lock = new Object();
    private TasksManager tasksManager;
    private GPSProvider gpsProvider;
    private List<ActivationObserver> observers = new ArrayList<>();

    public interface ActivationObserver {
        void updateAfterActivation();
    }

    public void attach(ActivationObserver observer){
        observers.add(observer);
    }

    public void detach(ActivationObserver observer){
        observers.remove(observer);
    }

    public ActivationManager(TasksManager tasksManager) {
        this.tasksManager = tasksManager;
        gpsProvider = new GPSProvider();
        initCheckConditionsInWL();
    }

    public void initCheckConditionsInWL() {
        timer = new Timer();

        task = new TimerTask() {
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        checkConditionsInWL();
                    }
                });
            }
        };
        timer.schedule(task, CHECK_CONDITIONS_START_TIME, CHECK_CONDITIONS_INTERVAL);
    }

    public Location getGPSLocation() {
        //if (gpsProvider.canGetGPSLocation()){
            return gpsProvider.getLocation();
        //}
        //return null;
    }

    public boolean canGPSLocation() {
        return gpsProvider.canGetGPSLocation();
    }



    private void checkConditionsInWL() {
        List<Task> activatedTasks = new ArrayList<>();
        ArrayList<Task> waitingList = tasksManager.getWaitingList();
        synchronized (lock) {
            Iterator<Task> iterator = waitingList.iterator();
            while (iterator.hasNext()) {
                Task task = iterator.next();
                if (task.isConditionsSatisfied(this)) {
                    task.changeStatus(Task.STATUSES.ACTIVE);
                    activatedTasks.add(task);
                }
            }
        }
        if (activatedTasks.size() > 0) {
            addTaskToActiveList(activatedTasks);
        }
    }

    private void addTaskToActiveList(List<Task> tasks) {
        BrainasApp app = ((BrainasApp) BrainasApp.getAppContext());
        NotificationManager notificationManager = app.getNotificationManager();
        ReminderScreenManager reminderScreenManager = app.getReminderScreenManager();
        synchronized (lock) {
            for (int i = 0; i < tasks.size(); i++) {
                Task task = tasks.get(i);
                notificationManager.notifyAboutTask(task); //TODO mov to notifyAllObservers
                notifyAllObservers();
            }
        }
    }

    private void notifyAllObservers() {
        for (ActivationObserver observer : observers) {
            observer.updateAfterActivation();
        }
    }
}
