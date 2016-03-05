package net.brainas.android.app.activities.taskedit;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.R;
import net.brainas.android.app.activities.EditTaskActivity;
import net.brainas.android.app.activities.EditTaskDescriptionActivity;
import net.brainas.android.app.domain.models.Condition;
import net.brainas.android.app.domain.models.Event;
import net.brainas.android.app.domain.models.EventGPS;
import net.brainas.android.app.domain.models.Task;

import org.w3c.dom.Text;


/**
 * Created by Kit Ushakov on 28/02/2016.
 */
public class EventActivity extends EditTaskActivity
        implements GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener, OnMapReadyCallback {

    private static String TAG = "EventActivity";

    private Toolbar toolbar;
    private BrainasApp app;
    private Task task = null;
    private Event event = null;
    private boolean editMode = false;

    private GoogleMap googleMap;
    private Marker currentMarker;
    private LatLng markerLocation;

    private LinearLayout saveEventBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);
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

        addClickListenerToSaveBtn();
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderContent();
    }

    public void saveEvent() {
        if (editMode) {

        } else {
            Condition newCondition = new Condition();
            newCondition.addEvent(event);
            task.addCondition(newCondition);
        }

        task.save();
        showTaskErrorsOrWarnings(task);
        finish();
    }

    public void back(View view) {
        task.setStatus(Task.STATUSES.WAITING);
        task.save();
        Intent intent = new Intent(this, EditTaskDescriptionActivity.class);
        intent.putExtra("taskLocalId", task.getId());
        startActivity(intent);
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
        // Location Autocomplete
        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                LatLng location = place.getLatLng();
                CameraUpdate center =
                        CameraUpdateFactory.newLatLng(location);
                CameraUpdate zoom=CameraUpdateFactory.zoomTo(15);
                googleMap.moveCamera(center);
                googleMap.animateCamera(zoom);
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
    public boolean onMarkerClick(Marker marker) {
        /*LatLng latLng = new LatLng(11,11);
        locationMarker = googleMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title("My Spot")
                .snippet("This is my spot!")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));*/
        return false;
    }

    @Override
    public void onMapClick(LatLng point) {
       // TODO
    }

    @Override
    public void onMapLongClick(LatLng location) {
        markerLocation = location;
        if (currentMarker != null ) {
            currentMarker.remove();
        }

        currentMarker = googleMap.addMarker(new MarkerOptions()
                .position(markerLocation)
                .title("Hello world")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.googleMap = map;
        this.googleMap.setOnMarkerClickListener(EventActivity.this);
        this.googleMap.setOnMapClickListener(EventActivity.this);
        this.googleMap.setOnMapLongClickListener(EventActivity.this);
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

    private void addClickListenerToSaveBtn() {
        final LinearLayout saveEventBtn = (LinearLayout) findViewById(R.id.saveEventBtn);
        final ViewGroup saveEventBtnInner = (ViewGroup) findViewById(R.id.saveEventBtnInner);
        final TextView saveTaskBtnLabel = (TextView) findViewById(R.id.saveTaskBtnLabel);

        View.OnClickListener onClickListener = new View.OnClickListener() {
            public void onClick(View v) {
                saveEventBtn.setOnClickListener(null);
                saveEventBtnInner.setOnClickListener(null);
                saveTaskBtnLabel.setOnClickListener(null);
                saveEvent();
            }
        };

        saveEventBtn.setOnClickListener(onClickListener);
        saveEventBtnInner.setOnClickListener(onClickListener);
        saveTaskBtnLabel.setOnClickListener(onClickListener);
    }
}

