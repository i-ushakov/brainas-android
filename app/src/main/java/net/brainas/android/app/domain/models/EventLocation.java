package net.brainas.android.app.domain.models;

import android.location.Location;

import net.brainas.android.app.CLog;
import net.brainas.android.app.R;
import net.brainas.android.app.services.ActivationService;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Element;

/**
 * Created by innok on 11/27/2015.
 */
public class EventLocation extends Event {
    static String EVENT_NAME = "Location";
    static final String EVENT_LOCATION_TAG = "#$#EVENT_LOCATION$#$";
    Double lat = null;
    Double lng = null;
    Double radius = null;
    String address = null;

    public EventLocation(){
        super();
    }

    public EventLocation(Long id, Integer globalId, Condition condition){
        super(id, globalId, condition);
    }

    public TYPES getType() {
        return TYPES.LOCATION;
    }

    public String getEventName() {
        return EVENT_NAME;
    }

    @Override
    public void fillInParamsFromXML(JSONObject jsonParams) throws JSONException {
        this.lat = jsonParams.getDouble("lat");
        this.lng = jsonParams.getDouble("lng");

        if (jsonParams.has("radius")) {
            this.radius = jsonParams.getDouble("radius");
        }

        if (jsonParams.has("address")) {
            this.address = jsonParams.getString("address");
        }
    }

    @Override
    public void fillInParamsFromJSONString(String paramsJSONStr) {
        try {
            JSONObject params = new JSONObject(paramsJSONStr);
            lat  = params.getDouble("lat");
            lng  = params.getDouble("lng");
            if (params.has("radius")) {
                radius = params.getDouble("radius");
            }
            if (params.has("address")) {
                address = params.getString("address");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getJSONStringWithParams() {
        JSONObject params= new JSONObject();
        try {
            params.put("lat", lat);
            params.put("lng", lng);
            params.put("radius", radius);
            params.put("address", address);
            params.put("address", address);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return params.toString();
    }

    public void setParams(double lat, double lng, Double radius , String address) {
        this.lat = lat;
        this.lng = lng;
        if (radius != null) {
            this.radius = radius;
        } else {
            this.radius = 100d;
        }
        this.address = address;
    }

    @Override
    public boolean isTriggered(Task.ActivationConditionProvider activationService) {
        CLog.i("ACTIVATION","isTriggered event " + this.getId() + " of task " + this.getParent().getParent().getId());
        /*if (active) {
            CLog.i(EVENT_LOCATION_TAG, "Location event with id " + this.getId() + " and params " + this.getJSONStringWithParams() + " was triggered by geofence system");
            return true;
        }*/

        Location location = activationService.getCurrentLocation();
        if (location != null) {
            double currentLat = location.getLatitude();
            double currentLng = location.getLongitude();
            Double distance = distance(lat, lng, currentLat, currentLng, "M");
            CLog.i("BA_ACTIVATION_SERVICE","currentLat = " + currentLat + ", currentLng = " + currentLng + ", lat = " + lat + ", lng = " + lng + ", distance =" + distance);
            if (radius == null || radius <= 200d) {
                radius = 200d;
            }
            if (distance <= radius) {
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public int getIconDrawableId() {
        return getIconDrawableId(null);
    }

    @Override
    public int getIconDrawableId(String colorName) {
        return R.drawable.gps_icon_100;
    }


    @Override
    public int getBackgroundColor() {
        return R.color.colorForLocationEvent;
    }

    @Override
    public int getTextColor() {
        return R.color.textColorForLocationEvent;
    }


    public EventLocation setLat(Double lat) {
        this.lat = lat;
        return this;
    }
    public Double getLat() {
        return this.lat;
    }

    public EventLocation setLng(Double lng) {
        this.lng = lng;
        return this;
    }

    public Double getLng() {
        return this.lng;
    }

    public EventLocation setRadius(Double radius) {
        this.radius = radius;
        return this;
    }

    public Double getRadius() {
        return this.radius;
    }

    public EventLocation setAddress(String address) {
        this.address = address;
        return this;
    }

    public String getAddress() {return  this.address;}

    public boolean isExecutable() {
        return true;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null) return false;
        if (object == this) return true;
        if (!(object instanceof EventLocation)) return false;
        EventLocation event = (EventLocation) object;

        // check type
        if (!event.getType().equals(this.getType())) {
            return false;
        }

        // check params
        if (event.getAddress() != null) {
            if (!event.getAddress().equals(this.address)) {
                return false;
            }
        } else {
            if (this.address != null) {
                return false;
            }
        }
        if (!event.getRadius().equals(this.radius)) {
            return false;
        }
        if (!event.getLng().equals(this.lng)) {
            return false;
        }
        if (!event.getLat().equals(this.lat)) {
            return false;
        }
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
