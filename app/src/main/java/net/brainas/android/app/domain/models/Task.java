package net.brainas.android.app.domain.models;

import android.content.Context;
import android.content.res.Resources;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.domain.helpers.ActivationManager;
import net.brainas.android.app.infrustructure.TaskChangesDbHelper;
import net.brainas.android.app.infrustructure.TaskDbHelper;

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
    private ArrayList<Condition> conditions = new ArrayList<>();
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
            case "CANCELED" :
                setStatus(STATUSES.CANCELED);
                break;

            default:
                setStatus(STATUSES.DISABLED);
                break;
        }
    }


    public void setStatus(STATUSES status){
        if (status == STATUSES.WAITING) {
            if (!checkActualityOfConditions()) {
                this.status = STATUSES.DISABLED;
                return;
            }
        }
        this.status = status;
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

    public void setConditions(ArrayList<Condition> conditions) {
        this.conditions.clear();
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

    public void save() {
        save(true, true);
    }

    public void save(boolean needToNotify, boolean needToLoggingChanges){
        TaskDbHelper taskDbHelper = ((BrainasApp)BrainasApp.getAppContext()).getTaskDbHelper();
        long taskId = taskDbHelper.addOrUpdateTask(this);
        this.setId(taskId);
        TaskChangesDbHelper taskChangesDbHelper = ((BrainasApp)BrainasApp.getAppContext()).getTasksChangesDbHelper();
        if (needToLoggingChanges) {
            taskChangesDbHelper.loggingChanges(this);
        }
        if (needToNotify) {
            notifyAllObservers();
        }
    }

    public boolean checkActualityOfConditions() {
        if (this.conditions.size() == 0) {
            warnings.put("no_conditions", "The task was set in disabled status, because it dosn't have conditions");
            return false;
        }
        return true;
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
