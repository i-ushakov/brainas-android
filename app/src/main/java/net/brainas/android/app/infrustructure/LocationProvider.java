package net.brainas.android.app.infrustructure;

import android.app.Service;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.Utils;

/**
 * Created by Kit Ushakov on 12/2/2015.
 */
public class LocationProvider implements LocationListener {
    private static final String TAG = "LocationProvider";
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 0;
    private static final long MIN_TIME_BW_UPDATES = 0;

    private Context context;

    private boolean canGetGPSLocation = false;
    private Location location = null;

    private double latitude;
    private double longtitude;
    private boolean isGPSEnabled = false;
    private boolean isNetworkEnabled = false;

    protected LocationManager locationManager;

    public LocationProvider(Context context) {
        this.context = context;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        initNetwork();
    }

    public Location initGPS() {
        Log.i(TAG, "isGPSEnabled?");
        Utils.appendLog(TAG, "isGPSEnabled?");
        isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        Log.i(TAG, "->" + isGPSEnabled);
        Utils.appendLog(TAG, "->" + isGPSEnabled);
        if (isGPSEnabled) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES,
                    MIN_TIME_BW_UPDATES,
                    this);

            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Log.i(TAG, "We have gotten location (GPS) with accuracy: " + location.getAccuracy());
            Utils.appendLog(TAG, "We have gotten location (GPS) with accuracy: " + location.getAccuracy());
            return location;
        } else {
            return null;
        }
    }

    private Location initNetwork() {
        Log.i(TAG, "isNetworkEnabled?");
        Utils.appendLog(TAG, "isNetworkEnabled?");
        isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        Log.i(TAG, "->" + isNetworkEnabled);
        Utils.appendLog(TAG, "->" + isNetworkEnabled);

        if (isNetworkEnabled) {
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES,
                    MIN_TIME_BW_UPDATES,
                    this);

            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            return location;
        } else {
            return null;
        }
    }


    public Location getLocation() {
        if (!isNetworkEnabled) {
            location = initNetwork();
            if (location == null || location.getAccuracy() > 100) {
                if(!isGPSEnabled) {
                    location = initGPS();
                } else {
                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    Log.i(TAG, "We have gotten location (GPS) with lat: " + location.getLatitude() + " lng: " + location.getLongitude() + " accuracy: " + location.getAccuracy());
                    Utils.appendLog(TAG, "We have gotten location (GPS) with lat: " +  location.getLatitude() + " lng: " + location.getLongitude() + " accuracy: " + location.getAccuracy());
                }
            }
        } else {
            if (isGPSEnabled) {
                resetProvider();
            }
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            Log.i(TAG, "We have gotten location (NETWORK) with lat: " +  location.getLatitude() + " lng: " + location.getLongitude() + " accuracy: " + location.getAccuracy());
            Utils.appendLog(TAG, "We have gotten location (NETWORK) with lat: " +  location.getLatitude() + " lng: " + location.getLongitude() + " accuracy: " + location.getAccuracy());
        }
        if (location == null && !isGPSEnabled && !isNetworkEnabled) {
            if (context instanceof BrainasApp) {
                // TODO Notification from app (Dialog)
            } else if (context instanceof Service) {
                // TODO Notification from service (top of screen)
            }
            Log.w(TAG, "Location services not available");
            Utils.appendLog(TAG, "Location services not available");
        }
        return location;
    }

    @Override
    public void onLocationChanged(Location location) {
        this.location = location;
        Log.i(TAG, "Location was changed (lat: " + location.getLatitude() + " lng: " + location.getLongitude() + " accuracy: " + location.getAccuracy() + ")");
        Utils.appendLog(TAG, "Location was changed (lat: " + location.getLatitude() + " lng: " + location.getLongitude() + " accuracy: " + location.getAccuracy() + ")");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    public double getLatitude() {
        if (location != null) {
            latitude = location.getLatitude();
        }
        return latitude;
    }

    public double getLongitude() {
        if (location != null) {
            longtitude = location.getLongitude();
        }
        return longtitude;
    }

    public boolean canGetGPSLocation() {
        return isGPSEnabled;
    }

    public void showSettingAlert() {
        //TODO
    }


    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    public void resetProvider() {
        locationManager.removeUpdates(this);
        initNetwork();
        Log.i(TAG, "resetProvider");
        Utils.appendLog(TAG,"resetProvider");
    }
}
