package net.brainas.android.app.activities.taskedit;

import android.app.PendingIntent;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.CLog;
import net.brainas.android.app.R;
import net.brainas.android.app.UI.UIHelper;
import net.brainas.android.app.domain.helpers.GoogleApiHelper;
import net.brainas.android.app.domain.helpers.TaskHelper;
import net.brainas.android.app.domain.models.Condition;
import net.brainas.android.app.domain.models.Event;
import net.brainas.android.app.domain.models.EventLocation;
import net.brainas.android.app.domain.models.EventTime;
import net.brainas.android.app.domain.models.Task;
import net.brainas.android.app.infrustructure.GeofenceTransitionsIntentService;
import net.brainas.android.app.infrustructure.LocationProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * Created by Kit Ushakov on 28/02/2016.
 */
public class EditEventActivity extends EditTaskActivity
        implements GoogleMap.OnMapLongClickListener, GoogleMap.OnMapClickListener, OnMapReadyCallback,
        LocationProvider.LocationChangedObserver, ResultCallback<Status>, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static String TAG = "EditEventActivity";
    private BrainasApp app;
    private LocationProvider locationProvider;
    private boolean editMode = false;
    private boolean initLoad = true;

    private Toolbar toolbar;

    private Long taskLocalId = null;
    private Task task = null;

    private Event event = null;
    private EventLocation eventLocation = null;
    private EventTime eventTime = null;

    private boolean initLocation = false;
    private boolean initTime = false;

    private GoogleMap googleMap;
    private Marker currentMarker;
    private Marker userLocationMarker;
    private LatLng currentUserLocation = null;

    private String validationMessage = "Validation of event is failed";
    private GoogleApiClient mGoogleApiClient;
    private PendingIntent mGeofencePendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_event);
        app = (BrainasApp) (BrainasApp.getAppContext());
        locationProvider = ((BrainasApp)BrainasApp.getAppContext()).getLocationProvider();
        locationProvider.attachObserver(this);

        taskLocalId = getIntent().getLongExtra("taskLocalId", 0);
        task = getTask(taskLocalId);
        if (task == null) {
            finish();
        }
        if (task != null) {
            retrieveAndSetEvent(task);
        } else {
            finish();
        }

        if (event != null) {
            editMode = true;
        } else {
            editMode = false;
            eventLocation = new EventLocation();
            eventTime = new EventTime();
            event = eventLocation;
        }
        
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        setTypeSpinner();

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                    .addApi(Places.GEO_DATA_API)
                    .addApi(Places.PLACE_DETECTION_API)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        mGoogleApiClient.connect();
        Log.i(TAG, "Try to connect mGoogleApiClient ...");
    }

    @Override
    protected void onResume() {
        super.onResume();
        //renderContent();
    }

    public void cancel(View view) {
        UIHelper.addClickEffectToButton(view, this);
        finish();
    }

    private void retrieveAndSetEvent(Task task) {
        Long eventId = getIntent().getLongExtra("eventId", 0);
        if (eventId != 0) {
            event = app.getTasksManager().retriveEventFromTaskById(task, eventId);
            if (event != null) {
                if (event instanceof EventLocation) {
                    eventLocation = (EventLocation) event;
                } else if (event instanceof EventTime) {
                    eventTime = (EventTime) event;
                }
            }
        }
    }

    private void renderContent() {
        findViewById(R.id.eventLocationPanel).setVisibility(View.GONE);
        findViewById(R.id.eventTimePanel).setVisibility(View.GONE);

        Event.TYPES eventType = event.getType();
        switch (eventType.getLabel(this)) {
            case "GPS" :
                renderLocationEventContent();
                break;

            case "TIME" :
                renderTimeEventContent();
                break;
        }
    }

    private void renderLocationEventContent() {
        findViewById(R.id.eventLocationPanel).setVisibility(View.VISIBLE);

        if(!initLocation) {
            PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                    getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

            autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(Place place) {
                    LatLng location = place.getLatLng();
                    LatLngBounds latLngBounds = place.getViewport();
                    changeLocation(location);
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 13));
                    if (latLngBounds != null) {
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 2));
                    }

                    Log.i(TAG, "Place: " + place.getName());
                }

                @Override
                public void onError(Status status) {
                    // TODO: Handle the error.
                    Log.i(TAG, "An error occurred: " + status);
                }
            });

            // Google Map
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.eventMapContainer);

            mapFragment.getMapAsync(this);
        }
    }

    private void renderTimeEventContent() {
        findViewById(R.id.eventTimePanel).setVisibility(View.VISIBLE);
        if (eventTime.getDatetime() == null) {
            eventTime.setDatetime(Calendar.getInstance());
        }
        DatePicker datePicker = (DatePicker) findViewById(R.id.pickerdate);
        datePicker.init(
                eventTime.getDatetime().get(Calendar.YEAR),
                eventTime.getDatetime().get(Calendar.MONTH),
                eventTime.getDatetime().get(Calendar.DAY_OF_MONTH),
                new DatePicker.OnDateChangedListener() {
                    @Override
                    public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        eventTime.getDatetime().set(Calendar.YEAR, year);
                        eventTime.getDatetime().set(Calendar.MONTH, monthOfYear);
                        eventTime.getDatetime().set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        eventTime.getDatetime().set(Calendar.SECOND, 0);
                    }
                }
        );
        datePicker.getCalendarView().setFirstDayOfWeek(Calendar.getInstance().getFirstDayOfWeek());
        datePicker.getCalendarView().setShowWeekNumber(false);

        TimePicker timePicker = (TimePicker) findViewById(R.id.timePicker);
        timePicker.setIs24HourView(true);
        int hour = eventTime.getDatetime().get(Calendar.HOUR_OF_DAY);
        int minute = eventTime.getDatetime().get(Calendar.MINUTE);
        if (Build.VERSION.SDK_INT >= 23) {
            timePicker.setHour(hour);
            timePicker.setMinute(minute);
        }
        else {
            timePicker.setCurrentHour(hour);
            timePicker.setCurrentMinute(minute);
        }

        timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                eventTime.getDatetime().set(Calendar.HOUR_OF_DAY, hourOfDay);
                eventTime.getDatetime().set(Calendar.MINUTE, minute);
                eventTime.getDatetime().set(Calendar.SECOND, 0);
            }
        });
    }

    @Override
    public void onMapClick(LatLng location) {
        changeLocation(location);
    }

    @Override
    public void onMapLongClick(LatLng location) {
        changeLocation(location);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.googleMap = map;
        this.googleMap.setOnMapClickListener(EditEventActivity.this);
        this.googleMap.setOnMapLongClickListener(EditEventActivity.this);
        if (initLoad && !editMode) {
            initLoad = false;
        }
        if (editMode && eventLocation != null) {
            LatLng location = new LatLng (eventLocation.getLat(), eventLocation.getLng());
            setMarker(location);
            moveAndZoomCamera(location);

        }
        getCurrentUserLocationAsync(editMode);
    }

    public void saveEventHandler(View view) {
        if (UIHelper.safetyBtnClick(view, EditEventActivity.this)) {
            if (event instanceof  EventLocation) {
                makeSureThatLocationIsSet();
                //setGeofence(eventLocation);
            }
            if (validateEvent()) {
                ((View) findViewById(R.id.saveEventBtn)).setOnClickListener(null);
                saveEvent();
            } else {
                Toast.makeText(EditEventActivity.this, validationMessage, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected  void onDestroy() {
        super.onDestroy();
        locationProvider.detachObserver(this);
    }

    private void changeLocation(LatLng location) {
        setMarker(location);
        eventLocation.setParams(location.latitude, location.longitude, null, null);
        GoogleApiHelper googleApiHelper = ((BrainasApp)BrainasApp.getAppContext()).getGoogleApiHelper();
        googleApiHelper.setAddressByLocation((EventLocation)eventLocation, false);

    }

    private void setMarker(LatLng location) {
        if (currentMarker != null ) {
            currentMarker.remove();
        }

        currentMarker = googleMap.addMarker(new MarkerOptions()
                .position(location)
                .title("")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
    }

    private void setUserLocationMarker(LatLng location) {
        if (userLocationMarker != null ) {
            userLocationMarker.remove();
        }

        if (googleMap != null) {
            userLocationMarker = googleMap.addMarker(new MarkerOptions()
                    .position(location)
                    .title("Yout current location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)));
        }
    }

    private void setTypeSpinner() {
        Spinner typeOfEventsSpinner = (Spinner)findViewById(R.id.typeOfEventSpinner);
        ArrayList eventTypeList = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.types_of_events)));
        final ArrayAdapter<CharSequence> adapter = new ArrayAdapter (
                this,
                android.R.layout.simple_spinner_item,
                eventTypeList
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeOfEventsSpinner.setAdapter(adapter);
        typeOfEventsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String eventType = adapter.getItem(position).toString();
                switch (eventType) {
                    case "Location":
                        event = eventLocation;
                        break;

                    case "Time":
                        event = eventTime;
                        break;
                }
                renderContent();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Nothing To Do
            }

        });

        Event.TYPES eventType = event.getType();
        switch (eventType.getLabel(this)) {
            case "GPS" :
                typeOfEventsSpinner.setSelection(adapter.getPosition("Location"));
                break;

            case "TIME" :
                typeOfEventsSpinner.setSelection(adapter.getPosition("Time"));
                break;
        }

        if (editMode) {
            typeOfEventsSpinner.setEnabled(false);
        } else if (TaskHelper.haveTaskATimeCondition(task)) {
            eventTypeList.remove(adapter.getPosition("Time"));
        }
    }

    private void saveEvent() {
        task = getTask(taskLocalId);
        if (!editMode) {
            Condition newCondition = new Condition();
            newCondition.addEvent(event);
            event.setParent(newCondition);
            newCondition.setParent(task);
            task.addCondition(newCondition);

        }
        tasksManager.saveTask(task, true, true, true);
        showTaskErrorsOrWarnings(task);
        task = getTask(taskLocalId);
        setGeofencesForAllEventsOfTask(task);
        finish();
    }

    private boolean validateEvent() {
        switch (event.getType().getLabel(this)) {
            case "GPS":
                if (((EventLocation) event).getLat() != null &&
                        ((EventLocation) event).getLng() != null) {
                    return true;
                } else {
                    validationMessage = "You have to set location on the map!";
                }
                break;

            case "TIME":
                if (((EventTime) event).getDatetime() != null &&
                        ((EventTime) event).getOffset() != null) {
                    return true;
                } else {
                    validationMessage = "You have to set date and time!";
                }
        }
        return false;
    }

    private void getCurrentUserLocationAsync(final boolean editMode) {
         new AsyncTask<Void, Void, LatLng>() {
            @Override
            protected LatLng doInBackground(Void... params) {
                LatLng latLng = null;
                Location location = locationProvider.getCurrentLocation();
                if (location != null) {
                    latLng = new LatLng(location.getLatitude(), location.getLongitude());
                }
                return latLng;
            }

            protected void onPostExecute(LatLng latLng) {
                if (latLng != null) {
                    currentUserLocation = latLng;
                    setUserLocationMarker(latLng);
                    if (!editMode) {
                        moveAndZoomCamera(latLng);
                    }
                }
            }
        }.execute();
    }

    private void moveAndZoomCamera(LatLng latLng) {
        CameraUpdate center = CameraUpdateFactory.newLatLngZoom(latLng, 13);
        googleMap.moveCamera(center);
    }

    @Override
    public void updateAfterLocationWasChanged(Location location) {
        setUserLocationMarker(new LatLng(location.getLatitude(), location.getLongitude()));
    }

    private void makeSureThatLocationIsSet () {
        if (!(event instanceof EventLocation)) {
            return;
        }
        EventLocation eventLocation = (EventLocation)event;
        if(((EventLocation)event).getLng() == null || ((EventLocation)event).getLat() == null) {
            if (currentUserLocation != null) {
                eventLocation.setLng(currentUserLocation.longitude);
                eventLocation.setLat(currentUserLocation.latitude);
                eventLocation.setRadius(150d);
                GoogleApiHelper googleApiHelper = ((BrainasApp)BrainasApp.getAppContext()).getGoogleApiHelper();
                googleApiHelper.setAddressByLocation(eventLocation, false);
            }
        }
    }

    private void setGeofencesForAllEventsOfTask(Task task) {
        if (task == null) {
            return;
        }
        CopyOnWriteArrayList<Condition> conditions = task.getConditions();
        for(Condition condition : conditions) {
            ArrayList<Event> events = condition.getEvents();
            Event event = events.get(0);
            if (event instanceof EventLocation) {
                setGeofence((EventLocation) event);
            }
        }
    }

    private boolean setGeofence (EventLocation eventLocation){
        CLog.i(TAG,"Adding geofence event with id = " + eventLocation.getId());
        if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()) {
            CLog.i(TAG,"Cannot set Geofence cause mGoogleApiClient == null or not connected");
            return false;
        }

        LocationServices.GeofencingApi.addGeofences(
                mGoogleApiClient,
                getGeofencingRequest(eventLocation.getLat(), eventLocation.getLng(), eventLocation.getId()),
                getGeofencePendingIntent(eventLocation.getId())
        ).setResultCallback(this);

        return true;

    }

    private GeofencingRequest getGeofencingRequest(Double latitude, Double longitude, long eventId) {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        List<Geofence> mGeofenceList = new ArrayList<>();
        mGeofenceList.add(new Geofence.Builder()
                // Set the request ID of the geofence. This is a string to identify this
                // geofence.
                .setRequestId(String.valueOf(eventId))

                .setCircularRegion(
                        latitude,
                        longitude,
                        75
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                        Geofence.GEOFENCE_TRANSITION_EXIT)
                .build());
        builder.addGeofences(mGeofenceList);
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent(long eventId) {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            CLog.i(TAG, "mGeofencePendingIntent != null");
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        CLog.i(TAG,"Getting PendingIntent with requestCode = " + (int)eventId + " for add geofence event");
        return PendingIntent.getService(getApplicationContext(), (int)eventId, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onResult(@NonNull Status status) {
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}

