package net.brainas.android.app.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.infrustructure.synchronization.SynchronizationManager;

/**
 * Created by kit on 10/6/2016.
 */
public class ServiceMustBeAliveReceiver extends BroadcastReceiver {

    static String SYNC_INTENT = "net.brainas.android.app.services.intent.SYNC_SERVICE_HAVE_TO_BE_RESTORE";
    static String ACT_INTENT = "net.brainas.android.app.services.intent.ACVTIVATION_SERVICE_HAVE_TO_BE_RESTORE";

    @Override
    public void onReceive(Context context, final Intent intent) {

        final String serviceClass = intent.getExtras().getString("serviceClass");
        if (serviceClass == null) {
            return;
        }

        Log.i("#SERVICES", "Got intent to restore " + serviceClass);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                restoreService(serviceClass, intent);
            }
        }, 2000);
    }

    private void restoreService(String serviceClass, Intent intent) {
        Log.i("#SERVICES", "We must to check is service " + serviceClass + " alive");

        Class sClass = null;
        switch (serviceClass) {
            case "SynchronizationService" : sClass = SynchronizationService.class;
                break;
            case "ActivationService" : sClass = ActivationService.class;
                break;
        }

        BrainasApp app = ((BrainasApp) BrainasApp.getAppContext());

        if (!app.isMyServiceRunning(sClass)) {
            Log.i("#SERVICES", "The service " + serviceClass + " is dead. We must to return bring it to life");
            switch (serviceClass) {
                case "SynchronizationService" :
                    String accountName = intent.getExtras().getString("accountName");
                    app.getSynchronizationManager().startSynchronizationService(accountName);
                    break;
                case "ActivationService" : sClass = ActivationService.class;
                    Integer accountId = intent.getExtras().getInt("accountId");
                    app.getActivationManager().startActivationService(accountId);
                    break;
            }
        } else {
            Log.i("#SERVICES", "The service " + serviceClass + " is sill alive. Fine!");
        }
    }
}