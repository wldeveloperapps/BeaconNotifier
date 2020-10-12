package com.example.beaconnotifier;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FragmentLocation#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentLocation extends Fragment implements OnMapReadyCallback {
    MapView mapView;
    GoogleMap map;

    public FragmentLocation() {
        // Required empty public constructor
    }


    public static FragmentLocation newInstance() {
        FragmentLocation fragment = new FragmentLocation();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_mandown, container, false);
        // Gets the MapView from the XML layout and creates it
        mapView = (MapView) v.findViewById(R.id.mapViewGps);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync((OnMapReadyCallback) this);
        mapView.onResume();

        return v;
    }


    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.map = googleMap;

        map.setMyLocationEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(true);
        //map.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 15));
        //
        //LatLng coffeys = new LatLng( 54.572720, -5.959151 );
        //map.addMarker( new MarkerOptions().position( coffeys ).title( "Coffey's Butchers" ) );
        //map.moveCamera( CameraUpdateFactory.newLatLngZoom( coffeys, 12 ) );
        //
    }
}


