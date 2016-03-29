package net.brainas.android.app.activities.taskedit;

import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
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
import net.brainas.android.app.R;
import net.brainas.android.app.UI.UIHelper;
import net.brainas.android.app.domain.helpers.GoogleApiHelper;
import net.brainas.android.app.domain.models.Condition;
import net.brainas.android.app.domain.models.Event;
import net.brainas.android.app.domain.models.EventGPS;
import net.brainas.android.app.domain.models.Task;
import net.brainas.android.app.infrustructure.GPSProvider;


/**
 * Created by Kit Ushakov on 28/02/2016.
 */
public class EditEventActivity extends EditTaskActivity
        implements GoogleMap.OnMapLongClickListener, GoogleMap.OnMapClickListener, OnMapReadyCallback {

    private static String TAG = "EditEventActivity";
    private BrainasApp app;
    private boolean editMode = false;
    private boolean initLoad = true;

    private Toolbar toolbar;

    private Task task = null;
    private Event event = null;

    private GoogleMap googleMap;
    private Marker currentMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_event);
        app = (BrainasApp) (BrainasApp.getAppContext());

        Long taskLocalId = getIntent().getLongExtra("taskLocalId", 0);
        task = app.getTasksManager().getTaskByLocalId(taskLocalId);

        Long eventId = getIntent().getLongExtra("eventId", 0);
        if (task != null && eventId != 0) {
            editMode = true;
            event = app.getTasksManager().retriveEventFromTaskById(task,eventId);
        } else {
            editMode = false;
            event = new EventGPS();
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderContent();
    }

    public void cancel(View view) {
        UIHelper.addClickEffectToButton(view, this);
        finish();
    }

    private void renderContent() {
        Event.TYPES eventType = event.getType();
        switch (eventType.getLabel(this)) {
            case "GPS" :
                renderLocationContent();
                break;
        }
    }

    private void renderLocationContent() {

        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                LatLng location = place.getLatLng();
                LatLngBounds latLngBounds = place.getViewport();
                changeLocation(location);

                //CameraUpdate center =
                        //CameraUpdateFactory.newLatLng(location);
               // CameraUpdate zoom = CameraUpdateFactory.zoomTo(15);
                //googleMap.moveCamera(center);
                //googleMap.animateCamera(zoom);

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
        if (initLoad) {
            getCurrentUserLocationAsync();
            initLoad = false;
        }
    }

    public void saveEventHandler(View view) {
        if (UIHelper.safetyBtnClick(view, EditEventActivity.this)) {
            if (validateEvent()) {
                ((View) findViewById(R.id.saveEventBtn)).setOnClickListener(null);
                saveEvent();
            } else {
                Toast.makeText(EditEventActivity.this, "You have to set location on the map!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void changeLocation(LatLng location) {
        setMarker(location);
        event.setParams(location.latitude, location.longitude, null, null);
        GoogleApiHelper googleApiHelper = ((BrainasApp)BrainasApp.getAppContext()).getGoogleApiHelper();
        googleApiHelper.setAddressByLocation((EventGPS)event, false);

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

    private void setTypeSpinner() {
        Spinner typeOfEventsSpinner = (Spinner)findViewById(R.id.typeOfEventSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.types_of_events, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeOfEventsSpinner.setAdapter(adapter);

        Event.TYPES eventType = event.getType();
        switch (eventType.getLabel(this)) {
            case "GPS" :
                typeOfEventsSpinner.setSelection(adapter.getPosition("Location"));
                break;
        }
    }

    private void saveEvent() {
        if (!editMode) {
            Condition newCondition = new Condition();
            newCondition.addEvent(event);
            event.setParent(newCondition);
            task.addCondition(newCondition);
        }
        tasksManager.saveTask(task);
        showTaskErrorsOrWarnings(task);
        finish();
    }

    private boolean validateEvent() {
        switch (event.getType().getLabel(this)) {
            case "GPS":
                if (((EventGPS) event).getLat() != null &&
                        ((EventGPS) event).getLng() != null) {
                    return true;
                }
                break;
        }
        return false;
    }

    private void getCurrentUserLocationAsync() {
         new AsyncTask<Void, Void, LatLng>() {
            @Override
            protected LatLng doInBackground(Void... params) {
                LatLng latLng = null;
                GPSProvider gpsProvider = ((BrainasApp)BrainasApp.getAppContext()).getGpsProvider();
                Location location = gpsProvider.getLocation();
                if (location != null) {
                    latLng = new LatLng(location.getLatitude(), location.getLongitude());
                }
                return latLng;
            }

            protected void onPostExecute(LatLng latLng) {
                if (latLng != null) {
                    CameraUpdate center = CameraUpdateFactory.newLatLngZoom(latLng, 13);
                    //CameraUpdate zoom = CameraUpdateFactory.zoomTo(15);
                    googleMap.moveCamera(center);
                    //googleMap.animateCamera(zoom);
                }
            }
        }.execute();
    }
}

