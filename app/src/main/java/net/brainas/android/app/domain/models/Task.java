package net.brainas.android.app.domain.models;

import android.content.Context;
import android.content.res.Resources;
import android.location.Location;

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
    private Image picture;
    private CopyOnWriteArrayList<Condition> conditions = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<TaskChangesObserver> observers = new CopyOnWriteArrayList<TaskChangesObserver>();
    private Object lock = new Object();
    private HashMap<String, String> warnings = new HashMap();

    public interface TaskChangesObserver {
        void updateAfterTaskWasChanged();
    }

    public interface ActivationConditionProvider {
        Location getCurrentLocation();
    }

    public void attachObserver(TaskChangesObserver observer){
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void detachObserver(TaskChangesObserver observer){
        observers.remove(observer);
    }

    public void detachAllObservers() {
        observers.clear();
    }

    public CopyOnWriteArrayList<TaskChangesObserver> getTaskChangesObservers() {
        return observers;
    }


    public enum STATUSES {
        ACTIVE,
        WAITING,
        TODO,
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

    public boolean haveImage() {
        if (picture != null && picture.getBitmap() != null) {
            return true;
        }
        return false;
    }

    public void setStatus(String status){
        if (status == null) {
            this.status = STATUSES.TODO;
            return;
        }
        switch (status) {
            case "ACTIVE" :
                this.status = STATUSES.ACTIVE;
                break;
            case "WAITING" :
                this.status = STATUSES.WAITING;
                break;
            case "TODO" :
                this.status = STATUSES.TODO;
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

    public Image getPicture() {
        return this.picture;
    }

    public void setPicture(Image picture) {
        this.picture = picture;
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

    public boolean isConditionsSatisfied(ActivationConditionProvider activationService) {
        HashMap<Event.TYPES, Boolean> triggeredEventsMap = new HashMap<>();
        for(Condition condition : conditions) {
            ArrayList<Event> events = condition.getEvents();
            for(Event evnet : events) {
                Event.TYPES eventType = evnet.getType();
                switch (eventType.name()) {
                    case "GPS":
                        if ((!triggeredEventsMap.containsKey(Event.TYPES.GPS) || triggeredEventsMap.get(Event.TYPES.GPS) == false)) {
                            if (evnet.isTriggered(activationService)) {
                                triggeredEventsMap.put(Event.TYPES.GPS, true);
                            } else {
                                triggeredEventsMap.put(Event.TYPES.GPS, false);
                            }
                            break;
                        }

                    case "TIME" :
                        if (evnet.isTriggered(activationService)) {
                            triggeredEventsMap.put(Event.TYPES.TIME, true);
                        } else {
                            triggeredEventsMap.put(Event.TYPES.TIME, false);
                        }
                        break;
                }
            }
        }

        boolean haveToBeActivated = false;
        Iterator it = triggeredEventsMap.entrySet().iterator();
        while (it.hasNext()) {
            HashMap.Entry pair = (HashMap.Entry)it.next();
            if((Boolean)pair.getValue()) {
                haveToBeActivated = true;
            } else {
                haveToBeActivated = false;
                break;
            }
        }
        if (haveToBeActivated) {
            return true;
        }
        return false;
    }

    public boolean checkActualityOfConditions() {
        if (this.conditions.size() == 0) {
            return false;
        }
        return true;
    }

    public void checkStatus() {
        if (this.status == null || this.status == STATUSES.WAITING || this.status == STATUSES.DISABLED) {
            if (!checkActualityOfConditions()) {
                this.status = STATUSES.DISABLED;
                warnings.put("no_conditions", "The task was set in disabled status, because it dosn't have conditions");
            } else {
                this.status = STATUSES.WAITING;
            }
        } else if (this.status == STATUSES.TODO) {
            if (this.conditions.size() > 0) {
                if (!checkActualityOfConditions()) {
                    this.status = STATUSES.DISABLED;
                    warnings.put("no_conditions", "The task was set in disabled status, because it dosn't have conditions");
                } else {
                    this.status = STATUSES.WAITING;
                }
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

    @Override
    public boolean equals(Object object) {
        if (object == null) return false;
        if (object == this) return true;
        if (!(object instanceof Task))return false;
        Task task = (Task)object;

        // check id
        if (task.getId() != this.id) {
            return false;
        }
        // check globalId
        if (task.getGlobalId() != this.globalId) {
            return false;
        }

        // check message
        String objectMessage = task.getMessage();
        if ((objectMessage == null && this.message != null) || (objectMessage != null && this.message == null)) {
            return false;
        }
        if (objectMessage != null) {
            if (!objectMessage.equals(this.message)) {
                return false;
            }
        }

        // check description
        String objectDescription = task.getDescription();
        if ((objectDescription == null && this.description != null) || (objectDescription != null && this.description == null)) {
            return false;
        }
        if (objectDescription!=null) {
            if (!objectDescription.equals(this.description)) {
                return false;
            }
        }

        // check pictures
        Image objectPicture = task.getPicture();
        if ((objectPicture == null && this.picture != null) || (objectPicture != null && this.picture == null)) {
            return false;
        }
        if (objectPicture != null) {
            if (!objectPicture.equals(this.picture)) {
                return false;
            }
        }

        // check conditions
        CopyOnWriteArrayList<Condition> objectConditions = task.getConditions();
        if ((objectConditions == null && this.conditions != null) || (objectConditions != null && this.conditions == null)) {
            return false;
        }
        if (objectConditions.size() != this.conditions.size() ) {
            return false;
        }
        for(int i = 0; i < objectConditions.size();i++) {
            if(!this.conditions.get(i).equals(objectConditions.get(i))) {
                return false;
            }
        }

        // check status
        Task.STATUSES status = ((Task) object).getStatus();
        if ((status == null && this.status != null) || (status != null && this.status == null)) {
            return false;
        }
        if (!this.status.equals(status)) {
            return false;
        }

        return true;
    }

    private void notifyAboutTask() {
        // TODO notivication of User  (NotificationManager.class /TaskManager.class)
    }
}
