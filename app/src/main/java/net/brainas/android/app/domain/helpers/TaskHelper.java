package net.brainas.android.app.domain.helpers;

import net.brainas.android.app.domain.models.Condition;
import net.brainas.android.app.domain.models.Event;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by innok on 3/1/2016.
 */
public class TaskHelper {
    public static String getEventInfo(Event event) {
        String info = "GPS: {lng: 1.123400, lat: 2.123456, rad: 100}";
        String eventType = event.getEventName();
        String eventParams = event.getJSONStringWithParams();
        String lng = "";
        String lat = "";
        String radius = "";
        try {
            JSONObject eventParamsJSON = new JSONObject(eventParams);
            lng = eventParamsJSON.getString("lng");
            lat = eventParamsJSON.getString("lat");
            radius = eventParamsJSON.getString("radius");
        } catch (JSONException e) {
            e.printStackTrace();
            return "Cannot get event's info";
        }
        info = eventType + ": " + "{lng:" + String.format("%.5f", Float.parseFloat(lng)) +
                ", lat:" + String.format("%.5f", Float.parseFloat(lat)) + ", rad:" + radius + "}";
        return info;
    }
}
