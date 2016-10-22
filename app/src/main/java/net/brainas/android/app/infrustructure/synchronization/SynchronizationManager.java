package net.brainas.android.app.infrustructure.synchronization;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import net.brainas.android.app.AccountsManager;
import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.infrustructure.UserAccount;
import net.brainas.android.app.services.ServiceMustBeAliveReceiver;
import net.brainas.android.app.services.SynchronizationService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

/**
 * Created by Kit Ushakov on 11/15/2015.
 *
 *This class is responsible for synchronization of data
 * on mobile device and web server. It's periodically doing synchronization
 * in background, sends all changes to server and accepts changes from server side.
 */

public class SynchronizationManager implements AccountsManager.SingInObserver {
    private static SynchronizationManager instance = null;

    //public static String serverUrl = "https://192.168.1.101/backend/web/connection/";
    public static String serverUrl = "https://brainas.net/backend/web/connection/";

    final static String TAG = "#SYNC_MANAGER";
    final static Integer ALARM_CODE = 1102;

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);

    private ScheduledFuture<?> syncThreadHandle = null;

    public BrainasApp app;

    private List<TaskSyncObserver> observers = new ArrayList<>();

    public interface TaskSyncObserver {
        void updateAfterSync();
    }

    public void attach(TaskSyncObserver observer){
        observers.add(observer);
    }

    public void detach(TaskSyncObserver observer){
        observers.remove(observer);
    }

    public void detachAllObservers() {
        observers.clear();
    }

    public SynchronizationManager() {app = ((BrainasApp)BrainasApp.getAppContext());}

    @Override
    public void updateAfterSingIn(UserAccount userAccount) {
        String accountName = userAccount.getAccountName();
        startSynchronizationService(accountName);
    }


    @Override
    public void updateAfterSingOut() {
        stopSynchronizationService();
    }

    public void startSynchronizationService(String accountName) {
        if (app.isMyServiceRunning(SynchronizationService.class)) {
            registerSynchronizationServiceReceivers();
            Log.i(TAG, "Service " + SynchronizationService.class + " already running");
            return;
        }
        Log.i(TAG, "Trying to start  sync service");
        Intent synchronizationService = new Intent(app.getBaseContext(), SynchronizationService.class);
        synchronizationService.putExtra("accountName", accountName);
        app.getBaseContext().startService(synchronizationService);
        registerSynchronizationServiceReceivers();
        //createServiceAlarm(accountName);
    }

    public void registerSynchronizationServiceReceivers() {
        app.getBaseContext().registerReceiver(syncWasDoneReceiver, new IntentFilter(SynchronizationService.BROADCAST_ACTION_SYNCHRONIZATION));
        app.getBaseContext().registerReceiver(syncMustBeStoppedReceiver, new IntentFilter(SynchronizationService.BROADCAST_ACTION_SYNCHRONIZATION_MUST_BE_STOPPED));
    }

    public void unregisterSynchronizationServiceReceivers() {
        try {
            app.getBaseContext().unregisterReceiver(syncWasDoneReceiver);
            app.getBaseContext().unregisterReceiver(syncMustBeStoppedReceiver);
        } catch (IllegalArgumentException e) {;}
    }

    public void stopSynchronizationService() {
        Intent activationService = new Intent(app.getBaseContext(), SynchronizationService.class);
        app.getBaseContext().stopService(activationService);
        unregisterSynchronizationServiceReceivers();
        //removeServiceAlarm();
    }

    private void notifyAllObservers() {
        for (TaskSyncObserver observer : observers) {
            observer.updateAfterSync();
        }
    }

    private BroadcastReceiver syncWasDoneReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            notifyAllObservers();
            Log.i(TAG, "Got notification about synchronization");
        }
    };

    private BroadcastReceiver syncMustBeStoppedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Got notification about synchronization must be stopped");
            stopSynchronizationService();
            final Bundle extras = intent.getExtras();
            if (extras != null && extras.getBoolean("restartSync") != false) {
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "Try to restart synchronization again");
                        startSynchronizationService(extras.getString("accountName"));
                    }
                }, 20*1000);
            }
        }
    };

    private void createServiceAlarm(String accountName) {
        Log.i(TAG, "Create alarm to check is service still alive");
        AlarmManager alarmManager = (AlarmManager) app.getSystemService(app.ALARM_SERVICE);
        Intent intent = new Intent(app, ServiceMustBeAliveReceiver.class);
        intent.putExtra("serviceClass", "SynchronizationService");
        intent.putExtra("accountName", accountName);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(app, ALARM_CODE, intent, 0);
        alarmManager.cancel(pendingIntent);
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0, 30 * 1000, pendingIntent);
    }

    public void removeServiceAlarm() {
        Log.i(TAG, "Remove alarm that is checking service still alive");
        Intent intent = new Intent(app, ServiceMustBeAliveReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(app, ALARM_CODE, intent, 0);
        AlarmManager alarmManager = (AlarmManager) app.getSystemService(app.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
    }
}
