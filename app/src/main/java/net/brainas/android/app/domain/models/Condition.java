package net.brainas.android.app.domain.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by innok on 11/27/2015.
 */
public class Condition {
    private Task parent;

    private long id;
    private Long taskId;
    private int globalId;
    private ArrayList<Event> events = new ArrayList<>();


    public Condition() {}

    public Condition(Integer id, Integer globalId, Long taskId) {
        if (id != null) {
            setId(id);
        }
        if (globalId != null) {
            setGlobalId(globalId);
        }

        if (taskId != null) {
            setTaskId(taskId);
        }
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getTaskId(){
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

    public boolean isValid() {
        if (events.size() == 0) {
            return false;
        }
        for(Event event : events) {
            if(!event.isExecutable()) {
                return false;
            }
        }
        return true;
    }

    public void setParent(Task task) {
        this.parent = task;
    }

    public Task getParent() {
        return this.parent;
    }
}
