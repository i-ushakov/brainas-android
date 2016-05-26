package net.brainas.android.app.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.UI.NotificationController;
import net.brainas.android.app.domain.helpers.ActivationManager;
import net.brainas.android.app.domain.helpers.TasksManager;
import net.brainas.android.app.domain.models.Task;
import net.brainas.android.app.infrustructure.AppDbHelper;
import net.brainas.android.app.infrustructure.LocationProvider;
import net.brainas.android.app.infrustructure.ServicesDbHelper;
import net.brainas.android.app.infrustructure.TaskChangesDbHelper;
import net.brainas.android.app.infrustructure.TaskDbHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by innok on 3/28/2016.
 */
public class ActivationService extends Service {
    public static final String BROADCAST_ACTION_ACTIVATION = "net.brainas.android.app.services.activation";

    private static String TAG = "ActivationService";
    public static final String SERVICE_NAME = "activation";

    private boolean isBrainasAppVisible = false;

    private Integer accountId;

    private AppDbHelper appDbHelper;
    private TaskDbHelper taskDbHelper;
    private TaskChangesDbHelper taskChangesDbHelper;
    private TasksManager tasksManager;
    private ServicesDbHelper servicesDbHelper;
    private LocationProvider locationProvider;
    private NotificationController notificationManager;
    private Timer timer;
    private TimerTask task;

    private final Handler handler = new Handler();


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        appDbHelper = new AppDbHelper(this);
        taskDbHelper = new TaskDbHelper(appDbHelper);
        servicesDbHelper = new ServicesDbHelper(appDbHelper);
        taskChangesDbHelper = new TaskChangesDbHelper(appDbHelper);
        locationProvider = new LocationProvider(this);
        notificationManager = new NotificationController();
        this.registerReceiver(broadcastReceiver, new IntentFilter(BrainasApp.BROADCAST_ACTION_APP_VISABILITY_WAS_CHANGED));
        if (intent != null) {
            accountId = intent.getExtras().getInt("accountId");
            JSONObject serviceParamsJSON = new JSONObject();
            try {
                serviceParamsJSON.put("accountId", accountId);
                servicesDbHelper.saveServiceParams(SERVICE_NAME, serviceParamsJSON.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            try {
                JSONObject serviceParamsJSON = new JSONObject(servicesDbHelper.getServiceParams(SERVICE_NAME));
                accountId = serviceParamsJSON.getInt("accountId");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        tasksManager = new TasksManager(taskDbHelper, taskChangesDbHelper, accountId);

        Log.i(TAG, "ActivationService was started for user with account id = " + accountId);
        initCheckConditionsInWL();
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onDestroy() {
        accountId = null;
        locationProvider.disconnect();
        Log.i(TAG, "ActivationService was destroyed");
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
            if (!isBrainasAppVisible) {
                notificationManager.createActiveNotification(this, activatedTasksIds.size());
            }
            notifyAboutActivation(activatedTasksIds);
        }
    }

    public Location getCurrentLocation() {
        return locationProvider.getCurrentLocation();
    }

    private void notifyAboutActivation(ArrayList<Long> activatedTasksIds) {
        Intent  intent = new Intent(BROADCAST_ACTION_ACTIVATION);
        intent.putExtra("activatedTasksIds", TextUtils.join(", ", activatedTasksIds));
        Log.i(TAG, "Notify About Activation");
        sendBroadcast(intent);
    }


    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            isBrainasAppVisible = intent.getBooleanExtra("appVisible", false);
            Log.i(TAG, "Got notification about that main App visible is " + isBrainasAppVisible);
        }
    };
}

