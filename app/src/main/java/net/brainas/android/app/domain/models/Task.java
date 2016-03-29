package net.brainas.android.app.domain.models;

import android.content.Context;
import android.content.res.Resources;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.domain.helpers.ActivationManager;
import net.brainas.android.app.infrustructure.TaskChangesDbHelper;
import net.brainas.android.app.infrustructure.TaskDbHelper;
import net.brainas.android.app.services.ActivationService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by innok on 11/9/2015.
 */
public class Task {
    private long id;
    private Integer accountId = null;
    private long globalId;
    private String message = null;
    private String description = null;
    private boolean haveImage = false;
    private STATUSES status = null;
    private CopyOnWriteArrayList<Condition> conditions = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<TaskChangesObserver> observers = new CopyOnWriteArrayList<TaskChangesObserver>();
    private Object lock = new Object();
    private HashMap<String, String> warnings = new HashMap();

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

    public Task(int user, String message) {
        this.accountId = user;
        this.message = message;
    }

    public Task(int id, int user, String message) {
        this.id = id;
        this.accountId = user;
        this.message = message;
    }

    /*public Task(int id, int globalId, int accountId, String message) {
        this.id = id;
        this.accountId = accountId;
        this.globalId = globalId;
        this.message = message;
    }*/

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public Integer getAccountId() {
        return this.accountId;
    }

    public void setGlobalId(long globalId) { this.globalId = globalId; }

    public long getGlobalId() { return this.globalId; }

    public long getExternalId() {
        return globalId;
    }

    public void setMessage(String message) {
        this.message = message;
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
            this.status = STATUSES.WAITING;
            return;
        }
        switch (status) {
            case "ACTIVE" :
                this.status = STATUSES.ACTIVE;
                break;
            case "WAITING" :
                this.status = STATUSES.WAITING;
                break;
            case "DONE" :
                this.status = STATUSES.DONE;
                break;
            case "CANCELED" :
                this.status = STATUSES.CANCELED;
                break;

            default:
                this.status = STATUSES.DISABLED;
                break;
        }
    }

    public void setStatus(STATUSES status){
        this.status = status;
        checkStatus();
    }

    public STATUSES getStatus() {
        return status;
    }

    public HashMap<String, String> getWarnings() {
        return warnings;
    }

    public void addCondition(Condition condition) {
        conditions.add(condition);
    }

    public void setConditions(CopyOnWriteArrayList<Condition> conditions) {
        this.conditions.clear();
        this.conditions.addAll(conditions);
    }

    public void removeCondition(Condition conditionForRemove) {
        long conditionId = conditionForRemove.getId();
        Condition condition = null;
        Iterator<Condition> i = conditions.iterator();
        while (i.hasNext()) {
            condition = i.next();
            if (condition.getId() == conditionId) {
                this.conditions.remove(condition);
            }
        }
    }

    public CopyOnWriteArrayList<Condition> getConditions() {
        return conditions;
    }

    public boolean isConditionsSatisfied(ActivationService activationService) {
        for(Condition condition : conditions) {
            ArrayList<Event> events = condition.getEvents();
            boolean haveToBeActivated = false;
            for(Event evnet : events) {
                if (evnet.isTriggered(activationService)) {
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

    public boolean checkActualityOfConditions() {
        if (this.conditions.size() == 0) {
            warnings.put("no_conditions", "The task was set in disabled status, because it dosn't have conditions");
            return false;
        }
        return true;
    }

    public void checkStatus() {
        if (this.status == null || this.status == STATUSES.WAITING || this.status == STATUSES.DISABLED) {
            if (!checkActualityOfConditions()) {
                this.status = STATUSES.DISABLED;
            } else {
                this.status = STATUSES.WAITING;
            }
        }
    }

    public void notifyAllObservers() {
        Iterator<TaskChangesObserver> it = observers.listIterator();
        while (it.hasNext()) {
            TaskChangesObserver observer = it.next();
            observer.updateAfterTaskWasChanged();
        }
    }

    private void notifyAboutTask() {
        // TODO notivication of User  (NotificationManager.class /TaskManager.class)
    }
}
