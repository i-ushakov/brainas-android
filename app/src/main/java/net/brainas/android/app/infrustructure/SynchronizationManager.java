package net.brainas.android.app.infrustructure;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import net.brainas.android.app.AccountsManager;
import net.brainas.android.app.BrainasApp;
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

    //static String serverUrl = "https://192.168.1.101/backend/web/connection/";
    static String serverUrl = "https://brainas.net/backend/web/connection/";

    static String TAG = "SYNCHRONIZATION";

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
        Intent synchronizationService = new Intent(app.getBaseContext(), SynchronizationService.class);
        synchronizationService.putExtra("accountName", accountName);
        app.getBaseContext().startService(synchronizationService);
        registerSynchronizationServiceReceivers();
    }

    public void registerSynchronizationServiceReceivers() {
        app.getBaseContext().registerReceiver(syncWasDoneReceiver, new IntentFilter(SynchronizationService.BROADCAST_ACTION_SYNCHRONIZATION));
        app.getBaseContext().registerReceiver(syncMustBeStoppedReceiver, new IntentFilter(SynchronizationService.BROADCAST_ACTION_SYNCHRONIZATION_MUST_BE_STOPPED));
    }

    public void stopSynchronizationService() {
        Intent activationService = new Intent(app.getBaseContext(), SynchronizationService.class);
        app.getBaseContext().stopService(activationService);
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
        }
    };
}
