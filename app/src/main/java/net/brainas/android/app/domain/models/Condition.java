package net.brainas.android.app.domain.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by innok on 11/27/2015.
 */
public class Condition {
    private int id;
    private int taskId;
    private int globalId;
    private ArrayList<Event> events = new ArrayList<>();
    public Condition() {}

    public Condition(Integer id, Integer globalId, Integer taskId) {
        if (id != null) {
            setId(id);
        }
        if (globalId != null) {
            setGlobalId(globalId);
        }

        setTaskId(taskId);
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public int getTaskId(){
        return taskId;
    }

    public void setGlobalId(int globalId) {
        this.globalId = globalId;
    }

    public int getGlobalId(){
        return globalId;
    }

    public void addEvent(Event event) {
        events.add(event);
    }

    public void addEvents(ArrayList<Event> events) {
        this.events.addAll(events);
    }

    public ArrayList<Event> getEvents() {
        return events;
    }
}
