package net.brainas.android.app.domain.helpers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;

import net.brainas.android.app.AccountsManager;
import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.infrustructure.GPSProvider;
import net.brainas.android.app.infrustructure.UserAccount;
import net.brainas.android.app.services.ActivationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by innok on 12/1/2015.
 */
public class ActivationManager implements AccountsManager.SingInObserver {
    static final String TAG = "ActivationManager";
    public static final int CHECK_CONDITIONS_START_TIME = 20000;
    public static final int CHECK_CONDITIONS_INTERVAL = 20000;
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

    public ActivationManager() {
        app = ((BrainasApp)BrainasApp.getAppContext());
    }

    @Override
    public void updateAfterSingIn(UserAccount userAccount) {
        Integer accountId = userAccount.getLocalAccountId();
        startActivationService(accountId);
    }

    @Override
    public void updateAfterSingOut() {}

    public void startActivationService(Integer accountId) {
        Intent activationService = new Intent(app.getBaseContext(), ActivationService.class);
        activationService.putExtra("accountId", accountId);
        app.getBaseContext().startService(activationService);
        app.getBaseContext().registerReceiver(broadcastReceiver, new IntentFilter(ActivationService.BROADCAST_ACTION_ACTIVATION));
    }

    /*private void addTaskToActiveList(List<Task> tasks) {
        BrainasApp app = ((BrainasApp) BrainasApp.getAppContext());
        NotificationManager notificationManager = app.getNotificationManager();
        synchronized (lock) {
            for (int i = 0; i < tasks.size(); i++) {
                Task task = tasks.get(i);
                notificationManager.notifyAboutTask(task); //TODO move to notifyAllObservers
                notifyAllObservers(); // TODO broadcast (Reminderscreen, TasksActivity, TaskCards)
            }
        }
    }*/

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
}
