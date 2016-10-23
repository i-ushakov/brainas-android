package net.brainas.android.app.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.BrainasAppSettings;
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

import io.fabric.sdk.android.Fabric;

/**
 * Created by innok on 3/28/2016.
 */
public class ActivationService extends Service implements Task.ActivationConditionProvider {
    public static final String BROADCAST_ACTION_ACTIVATION = "net.brainas.android.app.services.activation";
    public static final String BROADCAST_ACTION_STOP_ACTIVATION = "net.brainas.android.app.services.stopactivation";

    private static String TAG = "#ACTIVATION_SERVICE";
    public static final String SERVICE_NAME = "activation";

    private boolean isBrainasAppVisible = false;

    private Integer accountId;

    private BrainasApp app;
    private TaskDbHelper taskDbHelper;
    private TaskChangesDbHelper taskChangesDbHelper;
    private TasksManager tasksManager;
    private LocationProvider locationProvider;
    private NotificationController notificationManager;
    private Timer timer;
    private TimerTask task;

    private final Handler handler = new Handler();


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Fabric.with(getApplicationContext(), new Crashlytics());
        app = ((BrainasApp)BrainasApp.getAppContext());
        Crashlytics.log(Log.ERROR, TAG, "onStartCommand");
        initialiseSyncService(intent);
        return Service.START_STICKY;
    }

    @Override
    // this solution from http://stackoverflow.com/questions/3072173/how-to-call-a-method-after-a-delay-in-android
    public void onTaskRemoved(Intent rootIntent) {
        Log.i(TAG, "Activation Service: onTaskRemoved");
        /*Intent restartIntent = new Intent(this, ServiceMustBeAliveReceiver.class);
        restartIntent.putExtra("serviceClass", "ActivationService");
        restartIntent.putExtra("accountId", accountId);

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        PendingIntent pi = PendingIntent.getService(this, 2001, restartIntent,
                PendingIntent.FLAG_ONE_SHOT);
        if (android.os.Build.VERSION.SDK_INT >= 19) {
            am.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 3000, pi);
        } else {
            am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 3000, pi);
        }*/

        Intent restartIntent = new Intent();
        restartIntent.putExtra("serviceClass", "ActivationService");
        restartIntent.putExtra("accountId",accountId);
        restartIntent.setAction(ServiceMustBeAliveReceiver.ACT_INTENT);
        app.sendBroadcast(restartIntent);
    }

    private void initialiseSyncService(Intent intent) {
        taskDbHelper = app.getTaskDbHelper();
        taskChangesDbHelper = app.getTasksChangesDbHelper();
        locationProvider = app.getLocationProvider();
        notificationManager = new NotificationController();
        this.registerReceiver(broadcastReceiver, new IntentFilter(BrainasApp.BROADCAST_ACTION_APP_VISABILITY_WAS_CHANGED));
        if (intent != null) {
            accountId = intent.getExtras().getInt("accountId");
        } else {
            accountId = app.getLastUsedAccount().getId();
        }
        tasksManager = new TasksManager(taskDbHelper, taskChangesDbHelper, accountId);

        Log.i(TAG, "ActivationService was started for user with account id = " + accountId);
        initCheckConditionsInWL();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    public void stopService() {
        accountId = null;
        locationProvider.disconnect();
        this.unregisterReceiver(broadcastReceiver);
        stopCheckConditionsInWL();
        notifyAboutServiceMustBeStopped();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopService();
        //app.getActivationManager().removeServiceAlarm();
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
        timer.schedule(task, BrainasAppSettings.getCheckConditionsStartTime(), BrainasAppSettings.getCheckConditionsInterval());
    }

    public void stopCheckConditionsInWL() {
        timer.cancel();
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

    @Override
    public Location getCurrentLocation() {
        return locationProvider.getCurrentLocation();
    }

    private void notifyAboutActivation(ArrayList<Long> activatedTasksIds) {
        Intent  intent = new Intent(BROADCAST_ACTION_ACTIVATION);
        intent.putExtra("activatedTasksIds", TextUtils.join(", ", activatedTasksIds));
        Log.i(TAG, "Notify About Activation");
        sendBroadcast(intent);
    }

    private void notifyAboutServiceMustBeStopped() {
        Intent intent = new Intent(BROADCAST_ACTION_STOP_ACTIVATION);
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

