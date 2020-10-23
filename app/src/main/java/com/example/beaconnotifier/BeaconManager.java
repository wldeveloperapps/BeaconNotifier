package com.example.beaconnotifier;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BeaconManager implements RangeNotifier, BeaconConsumer {

    private Context ctx=null;
    private OnBeaconListener mBeaconListener;
    private org.altbeacon.beacon.BeaconManager beaconManager;
    private List<Region> lregion=new ArrayList<Region>();

    //******************************************************************************************************************************************
    public interface OnBeaconListener {
        public void onBeaconListener(Collection<Beacon> collection, String regionId);
    }

    //******************************************************************************************************************************************
    public void setOnBeaconListener(BeaconManager.OnBeaconListener listener) {
        mBeaconListener = listener;
    }

    public void addBeaconShortRange(String regionId,String beacon){
        Collection<Region> col=beaconManager.getRangedRegions();



    }

    public BeaconManager(Context c){
        ctx=c;
        beaconManager = (org.altbeacon.beacon.BeaconManager) org.altbeacon.beacon.BeaconManager.getInstanceForApplication(ctx);
    }

      @Override
    public void onBeaconServiceConnect() {

    }

    @Override
    public Context getApplicationContext() {
        return ctx;
    }

    @Override
    public void unbindService(ServiceConnection serviceConnection) {
        ctx.unbindService(serviceConnection);
    }

    @Override
    public boolean bindService(Intent intent, ServiceConnection serviceConnection, int i) {
        return ctx.bindService(intent, serviceConnection, i);
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> collection, Region region) {
        mBeaconListener.onBeaconListener(collection,region.getUniqueId());

    }
}
