package net.brainas.android.app.infrustructure;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.LocationListener;

import net.brainas.android.app.CLog;
import net.brainas.android.app.activities.MainActivity;

import java.text.DateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Kit Ushakov on 12/2/2015.
 */
public class LocationProvider implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private static final String LOCATION_PROVIDER_TAG = "#$#LOCATION_PROVIDER#$#";

    private Context context;

    private GoogleApiClient mGoogleApiClient;
    private Location mCurrentLocation;
    private LocationRequest mLocationRequest;
    private String mLastUpdateTime;
    private boolean mRequestingLocationUpdates = true; //http://developer.android.com/training/location/receive-location-updates.html#save-state
    private CopyOnWriteArrayList<LocationChangedObserver> observers = new CopyOnWriteArrayList<LocationChangedObserver>();

    public interface LocationChangedObserver {
        void updateAfterLocationWasChanged(Location location);
    }

    public void attachObserver(LocationChangedObserver observer){
        observers.add(observer);
    }

    public void detachObserver(LocationChangedObserver observer){
        observers.remove(observer);
    }

    public CopyOnWriteArrayList<LocationChangedObserver> getTaskChangesObservers() {
        return observers;
    }

    public LocationProvider(Context context) {
        this.context = context;
        initConnection(context);
        createLocationRequest();
    }

    public void initConnection(Context context) {
        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        mGoogleApiClient.connect();
    }

    public void disconnect() {
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mCurrentLocation != null) {
            Log.i(LOCATION_PROVIDER_TAG, "Last location from FusedLocationApi lat = " + mCurrentLocation.getLatitude() + " lng: " + mCurrentLocation.getLongitude());
        }

        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.w(LOCATION_PROVIDER_TAG, "Google Location Services connection is failed");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(LOCATION_PROVIDER_TAG, "onConnectionSuspended");
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        notifyAllObservers(location);
        CLog.i(LOCATION_PROVIDER_TAG, "onLocationChanged: mCurrentLocation = " + mCurrentLocation + "mLastUpdateTime" + mLastUpdateTime);
    }

    public Location getCurrentLocation() {
        return mCurrentLocation;
    }

    public void stopUpdates() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            disconnect();
        }
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(15000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
                        builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult locationSettingsResult) {
                final Status status = locationSettingsResult.getStatus();
                final LocationSettingsStates locationSettingsStates = locationSettingsResult.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can
                        // initialize location requests here.
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        if (context instanceof MainActivity) {
                            try {
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                status.startResolutionForResult(
                                        (Activity) context,
                                        MainActivity.REQUEST_CHECK_SETTINGS);
                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            }
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way
                        // to fix the settings so we won't show the dialog.
                        break;
                }
            }
        });
    }

    private void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        mRequestingLocationUpdates = false;
    }

    private void notifyAllObservers(Location location) {
        Iterator<LocationChangedObserver> it = observers.listIterator();
        while (it.hasNext()) {
            LocationChangedObserver observer = it.next();
            observer.updateAfterLocationWasChanged(location);
        }
    }
}
