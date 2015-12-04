package net.brainas.android.app.domain.models;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.domain.helpers.ActivationManager;
import net.brainas.android.app.infrustructure.TaskDbHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by innok on 11/9/2015.
 */
public class Task {
    private int id;
    private int globalId;
    private String message = null;
    private String description = null;
    private boolean haveImage = false;
    private STATUSES status = null;
    private ArrayList<Condition> conditions = new ArrayList<>();

    public enum STATUSES {
        ACTIVE,
        WAITING,
        DONE,
        DISABLED
    }

    public Task(String message) {
        this.message = message;
    }

    public Task(int id, String message) {
        this.id = id;
        this.message = message;
    }

    public Task(int id, int globalId, String message) {
        this.id = id;
        this.globalId = globalId;
        this.message = message;
    }

    public int getId() {
        return id;
    }

    public void setGlobalId(int globalId) { this.globalId = globalId; }

    public int getGlobalId() { return this.globalId; }

    public int getExternalId() {
        return globalId;
    }

    public String getMessage() {
        return message;
    }

    public void setDescription(String description){
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setImage(boolean haveImage) {
        this.haveImage = haveImage;
    }

    public boolean haveImage() {
        return haveImage;
    }

    public void setStatus(String status){
        if (status == null) {
            setStatus(STATUSES.DISABLED);
            return;
        }
        switch (status) {
            case "ACTIVE" :
                setStatus(STATUSES.ACTIVE);
                break;
            case "WAITING" :
                setStatus(STATUSES.WAITING);
                break;

            default:
                setStatus(STATUSES.DISABLED);
                break;
        }
    }

    public void setStatus(STATUSES status){
        this.status = status;
    }

    public STATUSES getStatus() {
        return status;
    }

    public void addCondition(Condition condition) {
        conditions.add(condition);
    }

    public void addConditions(ArrayList<Condition> conditions) {
        this.conditions.addAll(conditions);
    }

    public ArrayList<Condition> getConditions() {
        return conditions;
    }

    public boolean isConditionsSatisfied(ActivationManager activationManager) {
        for(Condition condition : conditions) {
            ArrayList<Event> events = condition.getEvents();
            boolean haveToBeActivated = false;
            for(Event evnet : events) {
                if (evnet.isTriggered(activationManager)) {
                    haveToBeActivated = true;
                } else {
                    haveToBeActivated = false;
                    break;
                }
            }
            if (haveToBeActivated) {
                return true;
            }
        }
        return false;
    }

    public void changeStatus(STATUSES newStatus) {
        if (this.status == STATUSES.WAITING && newStatus == STATUSES.ACTIVE) {
            notifyAboutTask();
        }
        setStatus(newStatus);
        save();
    }

    public void save(){
        TaskDbHelper taskDbHelper = ((BrainasApp)BrainasApp.getAppContext()).getTaskDbHelper();
        taskDbHelper.addOrUpdateTask(this);
    }


    private void notifyAboutTask() {
        // TODO notivication of User  (NotificationManager.class /TaskManager.class)
    }
}
