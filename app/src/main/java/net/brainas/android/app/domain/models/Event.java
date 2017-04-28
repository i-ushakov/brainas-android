package net.brainas.android.app.domain.models;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.IntegerRes;

import net.brainas.android.app.domain.helpers.ActivationManager;
import net.brainas.android.app.services.ActivationService;

import org.w3c.dom.Element;

/**
 * Created by innok on 11/27/2015.
 */
public abstract class Event {
    Condition parent = null;

    private long id;
    private int globalId;
    private long conditionId;
    private int globalConditionId;
    protected boolean active;

    public enum TYPES {
        LOCATION,
        TIME;
        // May be we don't neeed this (we need only name())
        public String getLabel(Context context) {
            Resources res = context.getResources();
            int resId = res.getIdentifier(this.name(), "string", context.getPackageName());
            if (0 != resId) {
                return (res.getString(resId));
            }
            return (name());
        }
    }
    Event() {}

    Event(Long id, Integer globalId, Condition condition) {
        if (id != null) {
            this.setId(id);
        }

        if (globalId != null) {
            this.setGlobalId(globalId);
        }

        if (condition != null) {
            this.conditionId = condition.getId();
            this.parent = condition;
        }
    }

    abstract public void fillInParamsFromXML(Element xmlParams);

    abstract public void fillInParamsFromJSONString(String params);

    abstract public String getJSONStringWithParams();

    abstract public TYPES getType();

    abstract public String getEventName();

    abstract public boolean isTriggered(Task.ActivationConditionProvider activationService);

    abstract public int getIconDrawableId();

    abstract public int getIconDrawableId(String colorName);

    abstract public int getBackgroundColor();

    abstract public int getTextColor();

    abstract public boolean isExecutable();

    public void setId(Long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public Event setGlobalId(int globalId) {
        this.globalId = globalId;
        return this;
    }

    public int getGlobalId() {
        return globalId;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean getActive() {
        return active;
    }

    public void setConditionId(int conditionId){
        this.conditionId = conditionId;
    }

    public long getConditionId() {
        return conditionId;
    }

    public Event setParent(Condition condition) {
        this.parent = condition;
        return this;
    }

    public Condition getParent() {
        return this.parent;
    }

}
