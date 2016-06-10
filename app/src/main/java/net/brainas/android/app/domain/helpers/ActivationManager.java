package net.brainas.android.app.domain.helpers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import net.brainas.android.app.AccountsManager;
import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.infrustructure.UserAccount;
import net.brainas.android.app.services.ActivationService;

import java.util.ArrayList;
import java.util.List;

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
    }

    public void stopActivationService() {
        Intent activationService = new Intent(app.getBaseContext(), ActivationService.class);
        app.getBaseContext().stopService(activationService);
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
}
