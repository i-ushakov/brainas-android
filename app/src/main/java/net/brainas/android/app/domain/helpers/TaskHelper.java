package net.brainas.android.app.domain.helpers;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.R;
import net.brainas.android.app.Utils;
import net.brainas.android.app.domain.models.Condition;
import net.brainas.android.app.domain.models.Event;
import net.brainas.android.app.domain.models.EventGPS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by innok on 3/1/2016.
 */
public class TaskHelper {
    public TaskHelper() {}
    public String getEventInfo(Event event) {
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
                    GoogleApiHelper googleApiHelper = ((BrainasApp)BrainasApp.getAppContext()).getGoogleApiHelper();
                    googleApiHelper.setAddressByLocation((EventGPS)event, true);
                }
                break;
        }

        return info;
    }

    public LinearLayout getImagesBlockForConditions(CopyOnWriteArrayList<Condition> conditions, Context context) {
        HashMap<Event.TYPES, Integer> eventTypesOccurrence = new HashMap<>();
        LinearLayout imagesBlock = new LinearLayout(context);
        imagesBlock.setOrientation(LinearLayout.HORIZONTAL);
        ArrayList<Event> events;
        Event.TYPES type;
        for (Condition condition : conditions) {
            events = condition.getEvents();
            for (Event event : events) {
                type = event.getType();
                if (eventTypesOccurrence.containsKey(type)) {
                    eventTypesOccurrence.put(type, eventTypesOccurrence.get(type) + 1);
                } else {
                    eventTypesOccurrence.put(type, 1);
                }
            }
        }
        if (eventTypesOccurrence.containsKey(Event.TYPES.GPS)) {
            ImageView eventTypeImage = Utils.createImageView(R.drawable.gps_icon_in_circle, 100, context);
            imagesBlock.addView(eventTypeImage);
        }
        return imagesBlock;
    }
}
