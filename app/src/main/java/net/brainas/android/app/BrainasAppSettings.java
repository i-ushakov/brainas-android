package net.brainas.android.app;

import android.util.Log;

/**
 * Created by kit on 10/11/2016.
 */
public class BrainasAppSettings {
    private static String APP_GENERAL_SETTINGS = "#$#APP_GENERAL_SETTINGS";

    private static final int SYNCHRONIZATION_SART_TIME = 5; //sec
    private static final int SYNCHRONIZATION_INTERVALl = 30; //sec
    private static final int CHECK_CONDITIONS_START_TIME = 20000; //ms
    private static final int CHECK_CONDITIONS_INTERVAL = 20000; //ms

    public static int getSynchronizationSartTime() {
        Log.i(APP_GENERAL_SETTINGS, "Synchronization first start time is " + SYNCHRONIZATION_SART_TIME);
        return SYNCHRONIZATION_SART_TIME;
    };
    public static int getSynchronizationInterval() {
        Log.i(APP_GENERAL_SETTINGS, "Interval of Synchronization is " + SYNCHRONIZATION_INTERVALl);
        return SYNCHRONIZATION_INTERVALl;
    }
    public static int getCheckConditionsStartTime() {
        Log.i(APP_GENERAL_SETTINGS, "Activation start time is " + CHECK_CONDITIONS_START_TIME);
        return CHECK_CONDITIONS_START_TIME;
    }

    public static int getCheckConditionsInterval() {
        Log.i(APP_GENERAL_SETTINGS, "Activation interval is " + CHECK_CONDITIONS_INTERVAL);
        return CHECK_CONDITIONS_INTERVAL;
    }
}
