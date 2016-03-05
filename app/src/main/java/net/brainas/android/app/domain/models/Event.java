package net.brainas.android.app.domain.models;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.IntegerRes;

import net.brainas.android.app.domain.helpers.ActivationManager;

import org.w3c.dom.Element;

/**
 * Created by innok on 11/27/2015.
 */
public abstract class Event {
    private int id;
    private int globalId;
    private int conditionId;
    private int globalConditionId;

    public enum TYPES {
        GPS;
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

    Event(Integer id, Integer globalId, Integer conditionId) {
        if (id != null) {
            this.setId(id);
        }

        if (globalId != null) {
            this.setGlobalId(globalId);
        }

        if (conditionId != null) {
            this.conditionId = conditionId;
        }
    }

    abstract public void fillInParamsFromXML(Element xmlParams);

    abstract public void fillInParamsFromJSONString(String params);

    abstract public String getJSONStringWithParams();

    abstract public TYPES getType();

    abstract public String getEventName();

    abstract public boolean isTriggered(ActivationManager activationManager);

    abstract public int getIconDrawableId();

    abstract public boolean isExecutable();

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }
    public void setGlobalId(int globalId) {
        this.globalId = globalId;
    }

    public int getGlobalId() {
        return globalId;
    }

    public void setConditionId(int conditionId){
        this.conditionId = conditionId;
    }

    public int getConditionId() {
        return conditionId;
    }
}
