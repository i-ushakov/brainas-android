package net.brainas.android.app.domain.models;

import net.brainas.android.app.R;
import net.brainas.android.app.services.ActivationService;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Element;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by innok on 11/27/2015.
 */
public class EventTime extends Event {
    static String EVENT_NAME = "Time";
    Calendar datetime = null;
    Integer offset = null;
    SimpleDateFormat sdf = null;

    public EventTime(){
        this(null, null, null);
    }

    public EventTime(Long id, Integer globalId, Condition condition){
        super(id, globalId, condition);
        sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    }

    public TYPES getType() {
        return TYPES.TIME;
    }

    public String getEventName() {
        return EVENT_NAME;
    }

    @Override
    public void fillInParamsFromXML(Element eventEl) {
        offset = Integer.parseInt(eventEl.getElementsByTagName("offset").item(0).getTextContent());
        String datetimeStr = eventEl.getElementsByTagName("datetime").item(0).getTextContent();
        datetime = getCalendarFromString(datetimeStr, offset);
    }

    @Override
    public void fillInParamsFromJSONString(String paramsJSONStr) {
        try {
            JSONObject params = new JSONObject(paramsJSONStr);
            offset  = params.getInt("offset");
            datetime  = getCalendarFromString( params.getString("datetime"), offset);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getJSONStringWithParams() {
        JSONObject params= new JSONObject();
        try {
            params.put("datetime",  sdf.format(datetime.getTime()));
            params.put("offset", offset);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return params.toString();
    }

    public void setDatetime(Calendar datetime) {
        this.datetime = datetime;
        this.offset = datetime.getTimeZone().getOffset(datetime.getTime().getTime()) / (1000 * 60);
    }

    @Override
    public boolean isTriggered(Task.ActivationConditionProvider activationConditionProvider) {
        Calendar currentTime = Calendar.getInstance();
        if (datetime != null) {
            if (datetime.before(currentTime)) {
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public int getIconDrawableId() {
        return getIconDrawableId(null);
    }

    @Override
    public int getIconDrawableId(String colorName) {
        if (colorName != null) {
            switch (colorName) {
                case "WHITE" :
                    return R.drawable.ic_alarm_on_white_48dp;
                default:
                    return R.drawable.ic_alarm_on_blue_48dp;
            }
        } else {
            return R.drawable.ic_alarm_on_blue_48dp;
        }
    }

    @Override
    public int getBackgroundColor() {
        return R.color.colorForTimeEvent;
    }

    @Override
    public int getTextColor() {
        return R.color.textColorForTimeEvent;
    }

    public Calendar getDatetime() {
        return datetime;
    }

    public String getDatetimeFromatedStr(Calendar datetime) {
        return sdf.format(datetime.getTime());
    }

    public Integer getOffset() {
        return this.offset;
    }

    public boolean isExecutable() {
        return true;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null) return false;
        if (object == this) return true;
        if (!(object instanceof EventTime)) return false;
        EventTime event = (EventTime) object;

        // check type
        if (!event.getType().equals(this.getType())) {
            return false;
        }

        // check params
        if (!event.getOffset().equals(this.offset)) {
            return false;
        }
        if (!event.getDatetime().equals(this.datetime)) {
            return false;
        }
        return true;
    }

    public Calendar getCalendarFromString(String datetimeStr, Integer offset) {
        // TODO Using offset to work with time zone
        Calendar calendar = Calendar.getInstance();
        try {
            calendar.setTime(sdf.parse(datetimeStr));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return calendar;
    }
}
