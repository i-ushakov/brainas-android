package net.brainas.android.app.activities.edittask;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
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
import net.brainas.android.app.UI.views.taskedit.ConditionEditView;
import net.brainas.android.app.activities.EditTaskActivity;
import net.brainas.android.app.activities.EditTaskDescriptionActivity;
import net.brainas.android.app.domain.models.Condition;
import net.brainas.android.app.domain.models.Task;
import net.brainas.android.app.fragments.MapFragment;

import java.util.ArrayList;


/**
 * Created by Kit Ushakov on 28/02/2016.
 */
public class ConditionActivity extends EditTaskActivity
        implements GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener, OnMapReadyCallback {

    private static String TAG = "ConditionActivity";
    int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;

    private Toolbar toolbar;
    private BrainasApp app;
    private GoogleMap googleMap;

    private Task task = null;

    LinearLayout conditionsPanel;
    private Marker myMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_condition);
        app = (BrainasApp) (BrainasApp.getAppContext());

        //long taskLocalId = getIntent().getLongExtra("taskLocalId", 0);
        //task = ((BrainasApp)BrainasApp.getAppContext()).getTasksManager().getTaskByLocalId(taskLocalId);
        
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


        Spinner typeOfEventsSpinner = (Spinner)findViewById(R.id.typeOfEventSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.types_of_events, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeOfEventsSpinner.setAdapter(adapter);

        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        try {
            Intent intent =
                    new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                            .build(this);
            startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
        } catch (GooglePlayServicesRepairableException e) {
            // TODO: Handle the error.
        } catch (GooglePlayServicesNotAvailableException e) {
            // TODO: Handle the error.
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map1);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                LatLng latLng = place.getLatLng();
                CameraUpdate center=
                        CameraUpdateFactory.newLatLng(latLng);

                CameraUpdate zoom=CameraUpdateFactory.zoomTo(15);
                googleMap.moveCamera(center);
                googleMap.animateCamera(zoom);
                // TODO: Get info about the selected place.
                Log.i(TAG, "Place: " + place.getName());
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i(TAG, "An error occurred: " + status);
            }
        });


        mapFragment.getMapAsync(this);
        googleMap = mapFragment.getMap();
        googleMap.setOnMarkerClickListener(ConditionActivity.this);
        googleMap.setOnMapClickListener(ConditionActivity.this);
        googleMap.setOnMapLongClickListener(ConditionActivity.this);

        //conditionsPanel = (LinearLayout) findViewById(R.id.taskConditionsPanel);


    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        renderContent();
    }

    public void addCondition(View view) {

    }

    public void saveTask(View view) {
        task.setStatus(Task.STATUSES.WAITING);
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
        /*ArrayList<Condition> conditions = task.getConditions();
        for (Condition condition : conditions) {
            conditionsPanel.addView(new ConditionEditView(this, condition));
        }*/

        //LatLng gpsCoordinates = getGPSCoordinates(condition.getEvents());
        LatLng gpsCoordinates = new LatLng(30,50);
        int mapId = getResources().getIdentifier("mapContainer2", "id", this.getPackageName());
        if (gpsCoordinates != null) {
            //MapFragment mapFragment = MapFragment.newInstance(this, mapId, gpsCoordinates);


            SupportMapFragment myMapFragment=new SupportMapFragment(){
                @Override
                public void onActivityCreated(Bundle savedInstanceState){

                }
            };

        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        LatLng latLng = new LatLng(11,11);
        myMarker = googleMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title("My Spot")
                .snippet("This is my spot!")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        return false;
    }

    @Override
    public void onMapClick(LatLng point) {
        //tvLocInfo.setText(point.toString());
       /// myMap.animateCamera(CameraUpdateFactory.newLatLng(point));
    }

    @Override
    public void onMapLongClick(LatLng location) {
        googleMap.addMarker(new MarkerOptions()
                .position(location)
                .title("Hello world")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        //tvLocInfo.setText("New marker added@" + point.toString());
        //myMap.addMarker(new MarkerOptions().position(point).title(point.toString()));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(this, data);
                Log.i(TAG, "Place: " + place.getName());
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
                // TODO: Handle the error.
                Log.i(TAG, status.getStatusMessage());

            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
        }
    }
}

