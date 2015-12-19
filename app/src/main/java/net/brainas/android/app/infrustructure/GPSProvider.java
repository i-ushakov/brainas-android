package net.brainas.android.app.infrustructure;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import net.brainas.android.app.BrainasApp;

/**
 * Created by Kit Ushakov on 12/2/2015.
 */
public class GPSProvider implements LocationListener {
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 0;
    private static final long MIN_TIME_BW_UPDATES = 1000 * 1;

    private boolean canGetGPSLocation = false;
    private Location location = null;

    private double latitude;
    private double longtitude;
    private boolean isGPSEnabled = false;
    private boolean isNetworkEnabled = false;

    protected LocationManager locationManager;

    public GPSProvider() {
        BrainasApp app = ((BrainasApp) BrainasApp.getAppContext());
        locationManager = (LocationManager) app.getSystemService(Context.LOCATION_SERVICE);
        initGPS();
    }

    public Location initGPS() {
        isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (isGPSEnabled) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES,
                    MIN_TIME_BW_UPDATES,
                    this);

            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            return location;
        } else {
            return null;
        }
    }

    private Location initNetwork() {
        isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

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
        if (!isGPSEnabled) {
            location = initGPS();
            if (location == null) {
                if(!isNetworkEnabled) {
                    location = initNetwork();
                } else {
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
            }
        } else {
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
        return location;
    }

    @Override
    public void onLocationChanged(Location location) {
        this.location = location;
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


}
