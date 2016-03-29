package net.brainas.android.app.services;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import net.brainas.android.app.domain.helpers.ActivationManager;
import net.brainas.android.app.domain.helpers.TasksManager;
import net.brainas.android.app.domain.models.Task;
import net.brainas.android.app.infrustructure.AppDbHelper;
import net.brainas.android.app.infrustructure.GPSProvider;
import net.brainas.android.app.infrustructure.TaskDbHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by innok on 3/28/2016.
 */
public class ActivationService extends Service {
    public static final String BROADCAST_ACTION_ACTIVATION = "net.brainas.android.app.services.activation";

    private static String TAG = "ActivationService";

    private AppDbHelper appDbHelper;
    private TaskDbHelper taskDbHelper;
    private TasksManager tasksManager;
    private GPSProvider gpsProvider;
    private Timer timer;
    private TimerTask task;

    private final Handler handler = new Handler();


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Integer accountId = intent.getExtras().getInt("accountId");
        appDbHelper = new AppDbHelper(this);
        taskDbHelper = new TaskDbHelper(appDbHelper);
        tasksManager = new TasksManager(taskDbHelper, accountId);
        gpsProvider = new GPSProvider(this);
        Log.i(TAG, "Service was started for user with account id = " + accountId);
        initCheckConditionsInWL();
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onDestroy() {
        Log.i(TAG, "Service was destroyed");
    }

    private void notifyAboutActivation(ArrayList<Long> activatedTasksIds) {
        Intent  intent = new Intent(BROADCAST_ACTION_ACTIVATION);
        intent.putExtra("activatedTasksIds", TextUtils.join(", ", activatedTasksIds));
        Log.i(TAG, "Notify About Activation");
        sendBroadcast(intent);
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
        timer.schedule(task, ActivationManager.CHECK_CONDITIONS_START_TIME, ActivationManager.CHECK_CONDITIONS_INTERVAL);
    }

    private void checkConditionsInWL() {
        ArrayList<Long> activatedTasksIds = new ArrayList<>();
        ArrayList<Task> waitingList = tasksManager.getWaitingList();
        if (waitingList != null) {
            Iterator<Task> iterator = waitingList.iterator();
            while (iterator.hasNext()) {
                Task task = iterator.next();
                if (task.isConditionsSatisfied(this)) {
                    tasksManager.changeStatus(task, Task.STATUSES.ACTIVE);
                    activatedTasksIds.add(task.getId());
                }
            }
        }

        Log.i(TAG, "Checked condition in Waiting List and " + activatedTasksIds.size() + " tasks was activated");

        if (activatedTasksIds.size() > 0) {
            notifyAboutActivation(activatedTasksIds);
        }
    }

    public Location getGPSLocation() {
        return gpsProvider.getLocation();
    }
}

