package net.brainas.android.app.domain.helpers;

import android.location.Location;
import android.os.Handler;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.UI.logic.TilesManager;
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
    private Timer timer;
    private TimerTask task;
    private final Handler handler = new Handler();
    private Object lock = new Object();
    private TasksManager tasksManager;
    private GPSProvider gpsProvider;

    public ActivationManager(TasksManager tasksManager) {
        this.tasksManager = tasksManager;
        gpsProvider = new GPSProvider();
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
        timer.schedule(task, 20000, 20000);
    }

    public Location getGPSLocation() {
        if (gpsProvider.canGetGPSLocation()){
            return gpsProvider.getLocation();
        }
        return null;
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
        TilesManager tilesManager = app.getTilesManager();
        //ArrayList<Task> waitingList = app.getWaitingList();
        //ArrayList<Task> activeList = app.getActiveList();
        synchronized (lock) {
            for (int i = 0; i < tasks.size(); i++) {
                Task task = tasks.get(i);
                //waitingList.remove(task);
                //activeList.add(task);
                notificationManager.notifyAboutTask(task);
                tilesManager.updateAfterSync();
            }
        }
    }

}
