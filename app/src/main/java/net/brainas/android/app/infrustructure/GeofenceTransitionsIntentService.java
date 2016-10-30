package net.brainas.android.app.infrustructure;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.CLog;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kit on 10/26/2016.
 */
public class GeofenceTransitionsIntentService extends IntentService {
    final private String GEOFENCE_TAG = "#$#GEOFENCE#$#";

    Context context;
    AppDbHelper appDbHelper;
    TaskDbHelper taskDbHelper;

    public GeofenceTransitionsIntentService() {
        super("GeofenceTransitionsIntentService");
    }

    public void onCreate() {
        super.onCreate();
        CLog.init(this.getApplicationContext());
        appDbHelper = new AppDbHelper(this.getApplicationContext());
        taskDbHelper = new TaskDbHelper(appDbHelper);
    }

    public void onDestroy() {
        super.onDestroy();
        appDbHelper.getDbAccess().close();
    }

    protected void onHandleIntent(Intent intent) {
        CLog.i(GEOFENCE_TAG, "We have got geofence intend");
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
           CLog.e(GEOFENCE_TAG, "Geofance event has error with ERROR_CODE: " + String.valueOf(geofencingEvent.getErrorCode()), null);
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            List<Geofence>  triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            List<Long> eventsIds = retrieveActivatedEventIds(triggeringGeofences);
            CLog.i(GEOFENCE_TAG, "We entered into geofence zone of events with ids: " + TextUtils.join(", ", eventsIds));
            setActiveParamForEvents(eventsIds);
        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            // TODO
        } else {
            // TODO Log error
        }
    }

    private List<Long> retrieveActivatedEventIds(List<Geofence> triggeringGeofences) {
        List<Long> evetnsIds = new ArrayList<>();
        for (Geofence geofence: triggeringGeofences) {
            evetnsIds.add(Long.parseLong(geofence.getRequestId()));
        }
        return evetnsIds;
    }

    private void setActiveParamForEvents(List<Long> evetnsIds) {
        for(Long eventId : evetnsIds) {
            int r = taskDbHelper.setActiveStatusForEvent(eventId, true);
            if (r == 1) {
                CLog.i(GEOFENCE_TAG, "Set active state for event with id " + eventId);
            }
        }
    }
}