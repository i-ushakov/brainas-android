package net.brainas.android.app.domain.helpers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import net.brainas.android.app.AccountsManager;
import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.infrustructure.UserAccount;
import net.brainas.android.app.services.ActivationService;
import net.brainas.android.app.services.ServiceMustBeAliveReceiver;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by innok on 12/1/2015.
 */
public class ActivationManager implements AccountsManager.SingInObserver {
    static final String TAG = "#ACTIVATION_MANAGER";
    final static Integer ALARM_CODE = 1101;
    private List<ActivationObserver> observers = new ArrayList<>();

    BrainasApp app;

    public interface ActivationObserver {
        void updateAfterActivation();
    }

    public void attach(ActivationObserver observer){
        observers.add(observer);
    }

    public void detach(ActivationObserver observer){
        observers.remove(observer);
    }

    public void detachAllObservers() {
        observers.clear();
    }

    public ActivationManager() {
        app = ((BrainasApp)BrainasApp.getAppContext());
    }

    @Override
    public void updateAfterSingIn(UserAccount userAccount) {
        Integer accountId = userAccount.getId();
        startActivationService(accountId);
    }

    @Override
    public void updateAfterSingOut() {
        stopActivationService();
    }

    public void startActivationService(Integer accountId) {
        Intent activationService = new Intent(app.getBaseContext(), ActivationService.class);
        activationService.putExtra("accountId", accountId);
        app.getBaseContext().startService(activationService);
        app.getBaseContext().registerReceiver(broadcastReceiver, new IntentFilter(ActivationService.BROADCAST_ACTION_ACTIVATION));
        app.getBaseContext().registerReceiver(serviceMustBeStoppedReceiver, new IntentFilter(ActivationService.BROADCAST_ACTION_STOP_ACTIVATION));
        //createServiceAlarm(accountId);
    }

    public void stopActivationService() {
        Intent activationService = new Intent(app.getBaseContext(), ActivationService.class);
        app.getBaseContext().stopService(activationService);
        unregisterServiceReceivers();
        //removeServiceAlarm();
    }

    public void unregisterServiceReceivers() {
        try {
            app.getBaseContext().unregisterReceiver(broadcastReceiver);
            app.getBaseContext().unregisterReceiver(serviceMustBeStoppedReceiver);
        } catch (IllegalArgumentException e) {;}
    }

    private void notifyAllObservers() {
        for (ActivationObserver observer : observers) {
            observer.updateAfterActivation();
        }
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            notifyAllObservers();
            Log.i(TAG, "Got notification about activation");
        }
    };

    private BroadcastReceiver serviceMustBeStoppedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Got notification about activation service must be stopped");
            stopActivationService();
        }
    };

    private void createServiceAlarm(Integer accountId) {
        Log.i(TAG, "Create alarm to check is service still alive");
        AlarmManager alarmManager = (AlarmManager) app.getSystemService(app.ALARM_SERVICE);
        Intent intent = new Intent(app, ServiceMustBeAliveReceiver.class);
        intent.putExtra("serviceClass", "ActivationService");
        intent.putExtra("accountId", accountId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(app, ALARM_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(pendingIntent);
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0, 30 * 1000, pendingIntent);
    }

    public void removeServiceAlarm() {
        Log.i(TAG, "Remove alarm that is checking service still alive");
        Intent intent = new Intent(app, ServiceMustBeAliveReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(app, ALARM_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) app.getSystemService(app.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
    }
}
