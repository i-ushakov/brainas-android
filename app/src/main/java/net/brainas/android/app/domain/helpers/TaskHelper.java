package net.brainas.android.app.domain.helpers;

import net.brainas.android.app.domain.models.Condition;
import net.brainas.android.app.domain.models.Event;
import net.brainas.android.app.domain.models.EventGPS;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by innok on 3/1/2016.
 */
public class TaskHelper {
    public static String getEventInfo(Event event) {
        String info = "";
        Event.TYPES eventType = event.getType();

        switch (eventType.name()) {
            case "GPS" :
                String address = ((EventGPS) event).getAddress();
                if (address != null && !address.equals("")) {
                    info = "Location: " + ((EventGPS) event).getAddress();
                } else {
                    info = "Location: " + "{lng:" + String.format("%.5f", ((EventGPS) event).getLng()) +
                            ", lat:" + String.format("%.5f", ((EventGPS) event).getLat()) + ", rad:" + ((EventGPS) event).getRadius() + "}";
                }
                break;
        }

        return info;
    }
}
