package net.brainas.android.app.domain.helpers;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;

import com.google.android.gms.maps.model.LatLng;

import net.brainas.android.app.domain.models.Event;
import net.brainas.android.app.domain.models.EventGPS;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by innok on 3/20/2016.
 */
public class GoogleApiHelper {
    Context context;
    EventGPS gpsEvent;
    boolean needSavingTheEvent;

    public GoogleApiHelper(Context context) {
        this.context = context;
    }

    public void setAddressByLocation(EventGPS gpsEvent, boolean needSavingTheEvent) {
        this.gpsEvent = gpsEvent;
        this.needSavingTheEvent = needSavingTheEvent;
        new GettingAddressByLocation().execute(gpsEvent);
    }

    private class GettingAddressByLocation extends AsyncTask<EventGPS, Void, String> {
        @Override
        protected String doInBackground(EventGPS... gpsEvents) {
            String address = null;

            EventGPS gpsEvent = gpsEvents[0];
            Double lng = gpsEvent.getLng();
            Double lat = gpsEvent.getLat();
            LatLng location = new LatLng(lat, lng);
            address = getAddressByLocation(location);
            return address;
        }

        protected void onPostExecute(String address) {
            gpsEvent.setAddress(address);
            if (needSavingTheEvent) {
                gpsEvent.save();
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
