package net.brainas.android.app.domain.models;

import android.location.Location;

import net.brainas.android.app.R;
import net.brainas.android.app.domain.helpers.ActivationManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Element;

/**
 * Created by innok on 11/27/2015.
 */
public class EventGPS extends Event {
    static String EVENT_NAME = "Location";
    double lat, lng, radius;

    public EventGPS(){
        super();
    }

    public EventGPS(Integer id, Integer globalId, Integer conditionId){
        super(id, globalId, conditionId);
    }

    public TYPES getType() {
        return TYPES.GPS;
    }

    public String getEventName() {
        return EVENT_NAME;
    }

    @Override
    public void fillInParamsFromXML(Element eventEl) {
        this.lat = Double.parseDouble(eventEl.getElementsByTagName("lat").item(0).getTextContent());
        this.lng = Double.parseDouble(eventEl.getElementsByTagName("lng").item(0).getTextContent());
        this.radius = Double.parseDouble(eventEl.getElementsByTagName("radius").item(0).getTextContent());
    }

    @Override
    public String getJSONStringWithParams() {
        JSONObject params= new JSONObject();
        try {
            params.put("lat",lat);
            params.put("lng",lng);
            params.put("radius",radius);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return params.toString();
    }

    @Override
    public void fillInParamsFromJSONString(String paramsJSONStr) {
        try {
            JSONObject params= new JSONObject(paramsJSONStr);
            lat  = params.getDouble("lat");
            lng  = params.getDouble("lng");
            radius  = params.getDouble("radius");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isTriggered(ActivationManager activationManager) {
        Location location = activationManager.getGPSLocation();
        if (location != null) {
            double currentLat = location.getLatitude();
            double currentLng = location.getLongitude();
            Double distance = distance(lat, lng, currentLat, currentLng, "M");
            if (distance <= radius) {
                return true;
            }
            return false;
        }
        return false;
    }

    public int getIconDrawableId() {
        return R.drawable.gps_icon;
    }

    public double getLat() {
        return this.lat;
    }

    public double getLng() {
        return this.lng;
    }

    public boolean isExecutable() {
        return true;
    }

    private static double distance(double lat1, double lon1, double lat2, double lon2, String unit) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        if (unit == "K") {
            dist = dist * 1.609344;
        } else if (unit == "N") {
            dist = dist * 0.8684;
        } else if (unit == "M") {
            dist = dist * 1.609344 * 1000;
        }

        return (dist);
    }

    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private static double rad2deg(double rad) {
        return (rad * 180 / Math.PI);
    }
}
