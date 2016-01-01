package net.brainas.android.app.fragments;


import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;


import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Created by Kit Ushakov on 12/09/2015.
 */
public class MapFragment extends SupportMapFragment implements OnMapReadyCallback {

    private AppCompatActivity appCompatActivity;
    private int containerId;
    private LatLng lanLng;

    public MapFragment () {
        super();
    }

    public static MapFragment newInstance(AppCompatActivity appCompatActivity, int containerId, LatLng lanLng) {

        //appCompatActivity.getSupportFragmentManager();
        MapFragment mapFragment = new MapFragment();
        mapFragment.setAppCompatActivity(appCompatActivity);
        mapFragment.setContainerId(containerId);
        mapFragment.setLanLng(lanLng);
        mapFragment.addMapFragemntToContainer();
        mapFragment.getMapAsync(mapFragment);
        return mapFragment;
    }


    @Override
    public void onMapReady(GoogleMap map) {
        map.addMarker(new MarkerOptions().position(lanLng).title("Marker in Sydney"));

        map.setMyLocationEnabled(true);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(lanLng, 13));

        map.addMarker(new MarkerOptions()
                .title("Sydney")
                .snippet("The most populous city in Australia.")
                .position(lanLng));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        //No call for super(). Bug on API Level > 11.
    }


    private void setAppCompatActivity(AppCompatActivity appCompatActivity) {
        this.appCompatActivity = appCompatActivity;
    }

    private void setContainerId(int containerId) {
        this.containerId = containerId;
    }

    private void addMapFragemntToContainer(){
        final MapFragment self = this;
        new Handler().post(new Runnable() {
            public void run() {
                appCompatActivity.getSupportFragmentManager().beginTransaction().replace(self.containerId, self).commitAllowingStateLoss();
            }});
        }

    private void setLanLng(LatLng lanLng) {
        this.lanLng = lanLng;
    }
}
