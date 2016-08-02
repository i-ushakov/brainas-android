package net.brainas.android.app.domain.helpers;

import android.content.Context;
import android.widget.ImageView;
import android.widget.LinearLayout;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.R;
import net.brainas.android.app.Utils;
import net.brainas.android.app.domain.models.Condition;
import net.brainas.android.app.domain.models.Event;
import net.brainas.android.app.domain.models.EventLocation;
import net.brainas.android.app.domain.models.EventTime;
import net.brainas.android.app.domain.models.Task;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by innok on 3/1/2016.
 */
public class TaskHelper {
    public TaskHelper() {}

    public static Element taskToXML(Document doc, Task task, String elementName) throws ParserConfigurationException {
        if (elementName ==  null) {
            elementName = "task";
        }
        Element taskEl = doc.createElement(elementName);
        taskEl.setAttribute("id", ((Long)task.getId()).toString());
        taskEl.setAttribute("globalId", ((Long) task.getGlobalId()).toString());
        // message
        Element messageEl = doc.createElement("message");
        messageEl.setTextContent(task.getMessage());
        taskEl.appendChild(messageEl);

        // description
        Element descriptionEl = doc.createElement("description");
        descriptionEl.setTextContent(task.getDescription());
        taskEl.appendChild(descriptionEl);

        // picture
        if (task.getPicture() != null) {
            Element pictureEl = doc.createElement("picture");
            Element pictureNameEl = doc.createElement("fileName");
            pictureNameEl.setTextContent(task.getPicture().getName());
            pictureEl.appendChild(pictureNameEl);
            if (task.getPicture().getResourceId() != null) {
                Element pictureResourceIdEl = doc.createElement("resourceId");
                pictureResourceIdEl.setTextContent(task.getPicture().getDriveId().getResourceId());
                pictureEl.appendChild(pictureResourceIdEl);
            }
            taskEl.appendChild(pictureEl);
        }

        // conditions
        CopyOnWriteArrayList<Condition> conditions = task.getConditions();
        Element conditionsEl = doc.createElement("conditions");
        if (conditions != null) {
            for (Condition condition : conditions) {
                Element conditionEl = doc.createElement("condition");
                conditionEl.setAttribute("localId", Long.toString(condition.getId()));
                conditionEl.setAttribute("globalId", Long.toString(condition.getGlobalId()));
                ArrayList<Event> events = condition.getEvents();
                Element eventsEl = doc.createElement("events");
                for (Event event : events) {
                    Element eventEl = doc.createElement("event");
                    eventEl.setAttribute("localId", Long.toString(event.getId()));
                    eventEl.setAttribute("globalId", Long.toString(event.getGlobalId()));
                    Element eventTypeEl = doc.createElement("type");
                    eventTypeEl.setTextContent(event.getType().toString());
                    eventEl.appendChild(eventTypeEl);
                    Element eventParamsEl = doc.createElement("params");
                    eventParamsEl.setTextContent(event.getJSONStringWithParams());
                    eventEl.appendChild(eventParamsEl);
                    eventsEl.appendChild(eventEl);
                }
                conditionEl.appendChild(eventsEl);
                conditionsEl.appendChild(conditionEl);
            }
        }
        taskEl.appendChild(conditionsEl);

        return taskEl;
    }

    public String getEventInfo(Event event) {
        String info = "";
        Event.TYPES eventType = event.getType();

        switch (eventType.name()) {
            case "GPS" :
                String address = ((EventLocation) event).getAddress();
                if (address != null && !address.equals("")) {
                    info = "Location: " + ((EventLocation) event).getAddress();
                } else {
                    info = "Location: " + "{lng:" + String.format("%.5f", ((EventLocation) event).getLng()) +
                            ", lat:" + String.format("%.5f", ((EventLocation) event).getLat()) + ", rad:" + ((EventLocation) event).getRadius() + "}";
                    GoogleApiHelper googleApiHelper = ((BrainasApp)BrainasApp.getAppContext()).getGoogleApiHelper();
                    googleApiHelper.setAddressByLocation((EventLocation)event, true);
                }
                break;

            case "TIME" :
                //String datetime = ((EventTime) event).getDatetimeFromatedStr();
                Calendar datetime = ((EventTime) event).getDatetime();
                Integer datetimeYear = datetime.get(Calendar.YEAR);
                Integer currentYear = Calendar.getInstance().get(Calendar.YEAR);
                if (datetimeYear.equals(currentYear)) {
                    SimpleDateFormat sdf = new SimpleDateFormat("d MMM HH:mm");
                    info = sdf.format(datetime.getTime());
                } else {
                    SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy HH:mm");
                    info = sdf.format(datetime.getTime());
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
            imagesBlock.addView(eventTypeImage(R.drawable.gps_icon_in_circle, context));
        }
        if (eventTypesOccurrence.containsKey(Event.TYPES.TIME)) {
            imagesBlock.addView(eventTypeImage(R.drawable.ic_alarm_on_blue_in_cirle, context));
        }
        return imagesBlock;
    }

    static public boolean haveTaskATimeCondition(Task task) {
        if (task != null) {
            CopyOnWriteArrayList<Condition> conditions = task.getConditions();
            for (Condition condition : conditions) {
                if (condition.getEvents().get(0).getType() == Event.TYPES.TIME) {
                    return true;
                }
            }
        }
        return false;
    }

    private ImageView eventTypeImage (int imageResId, Context context) {
        ImageView eventTypeImage = Utils.createImageView(imageResId, 70, context);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(5, 0, 5, 0);
        eventTypeImage.setLayoutParams(lp);
        return eventTypeImage;
    }
}
