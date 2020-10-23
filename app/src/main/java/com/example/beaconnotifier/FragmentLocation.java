package com.example.beaconnotifier;

import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FragmentLocation#newInstance} factory method to
 * create an instance of this fragment.
 */


public class FragmentLocation extends Fragment implements OnMapReadyCallback {

    protected static final String TAG = "FragmentLocation";
    MapView mapView;
    GoogleMap map;
    FusedLocationProviderClient mFusedLocationClient=null;
    LocationRequest mLocationRequest=null;
    LocationCallback mLocationCallback=null;
    Location lastLocation;
    int gpsInterval;
    FragmentLocationInterface FragmentLocationInterfaceListener=null;

    private long lastInterval=0;
    private long lastFastInterval=0;
    private int lastPrority=0;

    //******************************************************************************************************************************************
    public interface FragmentLocationInterface {
        void onFragmentLocation(Location location);
    }

    //******************************************************************************************************************************************
    public void setFragmentLocationInterface(FragmentLocation.FragmentLocationInterface listener) {
        FragmentLocationInterfaceListener = listener;
    }

    //******************************************************************************************************************************************
    public FragmentLocation() {
        // Required empty public constructor
    }

    //******************************************************************************************************************************************
    public static FragmentLocation newInstance() {
        FragmentLocation fragment = new FragmentLocation();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if(args!=null)
         gpsInterval = args.getInt("gps_interval", 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_mandown, container, false);
        // Gets the MapView from the XML layout and creates it
        try {
            mapView = (MapView) v.findViewById(R.id.mapViewGps);
            mapView.onCreate(savedInstanceState);
            mapView.getMapAsync((OnMapReadyCallback) this);
            mapView.onResume();
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getContext());
        } catch (Exception e) {
            Log.e(TAG, "onCreateView:" + e.toString());
        }
        return v;
    }

    @SuppressLint("MissingPermission")
    public boolean setLocationParam(long interval, long fastInterval, int priority) {
        try {
            if(mFusedLocationClient==null ||mLocationRequest==null || mLocationCallback==null)return false;
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            if (interval>0){
                mLocationRequest.setInterval(lastInterval=interval);
            }
            if (fastInterval>0){
                mLocationRequest.setFastestInterval(lastFastInterval=fastInterval);
            }
            if (priority>0){
                mLocationRequest.setPriority(lastPrority=priority);
            }
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            return true;
        } catch (Exception e) {
            Log.e(TAG, "setLocationParam:" + e.toString());
        }
        return false;
    }


    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        try {
            this.map = googleMap;
            lastFastInterval=lastInterval=gpsInterval;
            lastPrority=LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
            mLocationRequest = new LocationRequest();
            mLocationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    Location location = null;
                    LatLng latLng;
                    location = locationResult.getLastLocation();
                    latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    CameraPosition cameraPosition = new CameraPosition.Builder().target(new LatLng(latLng.latitude, latLng.longitude)).zoom(16).build();
                    map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                    if(FragmentLocationInterfaceListener!=null)FragmentLocationInterfaceListener.onFragmentLocation(location);
                    lastLocation=location;
                }
            };
            setLocationParam(gpsInterval,gpsInterval,LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
            map.setMyLocationEnabled(true);
            map.getUiSettings().setMyLocationButtonEnabled(true);
        } catch (Exception e) {
            Log.e(TAG, "onMapReady:" + e.toString());
        }
    }

}