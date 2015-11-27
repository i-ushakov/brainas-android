package net.brainas.android.app.domain.models;

import org.w3c.dom.Element;

/**
 * Created by innok on 11/27/2015.
 */
public class EventGPS extends Event {
    double lat, lng, radius;

    public EventGPS(){
        super();
    }

    @Override
    public void fillInParamsFromXML(Element eventEl) {
        this.lat = Double.parseDouble(eventEl.getElementsByTagName("lat").item(0).getTextContent());
        this.lng = Double.parseDouble(eventEl.getElementsByTagName("lng").item(0).getTextContent());
        this.radius = Double.parseDouble(eventEl.getElementsByTagName("radius").item(0).getTextContent());
    }
}
