package net.brainas.android.app.domain.models;

import org.w3c.dom.Element;

/**
 * Created by innok on 11/27/2015.
 */
public abstract class Event {
    public enum TYPES {
        GPS
    }
    Event() {}

    abstract public void fillInParamsFromXML(Element xmlParams);
}
