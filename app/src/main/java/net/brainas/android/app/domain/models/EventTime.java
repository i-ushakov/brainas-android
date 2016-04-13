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
        super();
    }

    public EventTime(Long id, Integer globalId, Condition condition){
        super(id, globalId, condition);

        sdf = new SimpleDateFormat("dd-mm-yyyy HH:mm:ss");
    }

    public TYPES getType() {
        return TYPES.TIME;
    }

    public String getEventName() {
        return EVENT_NAME;
    }

    @Override
    public void fillInParamsFromXML(Element eventEl) {
        String datetimeStr = eventEl.getElementsByTagName("datetime").item(0).getTextContent();
        datetime = getCalendarFromString(datetimeStr);
        offset = Integer.parseInt(eventEl.getElementsByTagName("offset").item(0).getTextContent());
    }

    @Override
    public void fillInParamsFromJSONString(String paramsJSONStr) {
        try {
            JSONObject params = new JSONObject(paramsJSONStr);
            datetime  = getCalendarFromString( params.getString("datetime"));
            offset  = params.getInt("offset");
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

    public void setParams(String datetime, Integer offset) {
        this.datetime = getCalendarFromString(datetime);
        this.offset = offset;
    }

    @Override
    public boolean isTriggered(ActivationService activationService) {
        Calendar currentTime = Calendar.getInstance();
        if (datetime != null) {
            if (datetime.before(currentTime.getTime())) {
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

    public String getDatetimeFromatedStr() {
        return sdf.format(datetime.getTime());
    }

    public Integer getOffset() {
        return this.offset;
    }

    public boolean isExecutable() {
        return true;
    }

    private Calendar getCalendarFromString(String datetimeStr) {
        Calendar calendar = Calendar.getInstance();
        try {
            calendar.setTime(sdf.parse(datetimeStr));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return calendar;
    }
}
