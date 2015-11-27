package net.brainas.android.app.domain.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by innok on 11/27/2015.
 */
public class Condition {
    List<Event> events = new ArrayList<>();
    public Condition() {}

    public void addEvent(Event event) {
        events.add(event);
    }
}
