package net.brainas.android.app.domain.helpers;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;

import com.google.android.gms.maps.model.LatLng;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.domain.models.EventLocation;
import net.brainas.android.app.infrustructure.NetworkHelper;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by innok on 3/20/2016.
 */
public class GoogleApiHelper {
    Context context;
    TasksManager tasksManager;
    EventLocation locationEvent;
    boolean needSavingTheEvent;

    public GoogleApiHelper(Context context) {
        this.context = context;
    }

    public void setAddressByLocation(EventLocation gpsEvent, boolean needSavingTheEvent) {
        tasksManager = ((BrainasApp)BrainasApp.getAppContext()).getTasksManager();
        this.locationEvent = gpsEvent;
        this.needSavingTheEvent = needSavingTheEvent;
        if (NetworkHelper.isNetworkActive()) {
            new GettingAddressByLocation().execute(gpsEvent);
        }
    }

    private class GettingAddressByLocation extends AsyncTask<EventLocation, Void, String> {
        @Override
        protected String doInBackground(EventLocation... gpsEvents) {
            String address = null;

            EventLocation gpsEvent = gpsEvents[0];
            Double lng = gpsEvent.getLng();
            Double lat = gpsEvent.getLat();
            LatLng location = new LatLng(lat, lng);
            address = getAddressByLocation(location);
            return address;
        }

        protected void onPostExecute(String address) {
            locationEvent.setAddress(address);
            if (needSavingTheEvent) {
                tasksManager.saveEvent(locationEvent);
            }
        }
    }


    private String getAddressByLocation(LatLng location) {
        String addressStr = null;
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());

        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(
                    location.latitude,
                    location.longitude, 1);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        if (addresses != null && addresses.size() > 0) {
            Address address = addresses.get(0);
            addressStr = address.getAddressLine(0) + ", " + address.getAddressLine(1);
        }
        return addressStr;
    }
}
