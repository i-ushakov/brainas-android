package net.brainas.android.app.infrustructure;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import net.brainas.android.app.BrainasApp;

/**
 * Created by innok on 1/19/2016.
 */
public class NetworkHelper {
    public static boolean isNetworkActive() {
        ConnectivityManager cm =
                (ConnectivityManager)BrainasApp.getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        return isConnected;
    }
}
