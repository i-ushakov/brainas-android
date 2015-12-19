package net.brainas.android.app.domain.models;

import android.content.Context;
import android.content.res.Resources;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.R;
import net.brainas.android.app.domain.helpers.ActivationManager;
import net.brainas.android.app.infrustructure.TaskDbHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;

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
    private CopyOnWriteArrayList<TaskChangesObserver> observers = new CopyOnWriteArrayList<TaskChangesObserver>();
    private Object lock = new Object();

    public interface TaskChangesObserver {
        void updateAfterTaskWasChanged();
    }

    public void attachObserver(TaskChangesObserver observer){
        observers.add(observer);
    }

    public void detachObserver(TaskChangesObserver observer){
        observers.remove(observer);
    }

    public CopyOnWriteArrayList<TaskChangesObserver> getTaskChangesObservers() {
        return observers;
    }


    public enum STATUSES {
        ACTIVE,
        WAITING,
        DONE,
        CANCELED,
        DISABLED;

        public String getLabel(Context context) {
            Resources res = context.getResources();
            int resId = res.getIdentifier(this.name(), "string", context.getPackageName());
            if (0 != resId) {
                return (res.getString(resId));
            }
            return (name());
        }
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
            case "DONE" :
                setStatus(STATUSES.DONE);
                break;
            case "CANCLED" :
                setStatus(STATUSES.CANCELED);
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
        notifyAllObservers();
    }


    private void notifyAboutTask() {
        // TODO notivication of User  (NotificationManager.class /TaskManager.class)
    }

    private void notifyAllObservers() {
        Iterator<TaskChangesObserver> it = observers.listIterator();
        while (it.hasNext()) {
            TaskChangesObserver observer = it.next();
            observer.updateAfterTaskWasChanged();
        }
    }
}
