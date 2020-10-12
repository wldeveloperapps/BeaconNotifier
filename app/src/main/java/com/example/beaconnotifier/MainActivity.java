package com.example.beaconnotifier;
//https://altbeacon.github.io/android-beacon-library/


import android.Manifest;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.service.ArmaRssiFilter;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/*
    ALTBEACON	        m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25
    EDDYSTONE  TLM	    x,s:0-1=feaa,m:2-2=20,d:3-3,d:4-5,d:6-7,d:8-11,d:12-15
    EDDYSTONE  UID	    s:0-1=feaa,m:2-2=00,p:3-3:-41,i:4-13,i:14-19
    EDDYSTONE  URL	    s:0-1=feaa,m:2-2=10,p:3-3:-41,i:4-20v
    IBEACON	            m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24
*/
//****************************************************************************************************************
public class MainActivity extends AppCompatActivity implements RangeNotifier, BeaconConsumer {
    protected static final String TAG = "LCF";
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_BACKGROUND_LOCATION = 2;
    private static final int MAX_VECES_INZONE=2;
    private static final double RSSI_MINIMO=-80.0;
    private static final double AJUSTE_MTS=5.0;

    private BeaconManager beaconManager;
    private List<String> filtros = new ArrayList<String>();
    private String IBEACON_MASK = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";
    //private String IBEACON_MASK = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24,m:27-28=5749";
    FragmentTransaction transaction;
    Fragment fragmentLocation;
    private ShakeListener mShaker = null;
    Map<String, Integer> mapbeacons = new HashMap<String, Integer>();
    private TextView textViewAviso;
    private ToggleButton toggleButtonAlarmaProximidad;
    private ImageButton imageButtonPanic;
    //*****************************************************************************************************************
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setEntorno();
        setComponents();
        verifyBluetooth();
        checkPermisosBluetooth();
        setBeacons();
        setFragments();
        muestraAviso("Llame al Centro de Control a la mayor brevedad");
    }

    //*****************************************************************************************************************
    private void setEntorno() {
        getSupportActionBar().hide();
        if (Build.VERSION.SDK_INT > 16) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    //*****************************************************************************************************************
    private void setFragments(){
        fragmentLocation=new FragmentLocation();
        getSupportFragmentManager().beginTransaction().add(R.id.contenedorFraments,fragmentLocation).commit();

    }
    //*****************************************************************************************************************
    private void setComponents() {
        textViewAviso=(TextView) findViewById(R.id.textViewAviso);
        textViewAviso.setMovementMethod(new ScrollingMovementMethod());
        toggleButtonAlarmaProximidad=(ToggleButton) findViewById(R.id.toggleButtonAlarmaProximidad);
        // attach an OnClickListener
        toggleButtonAlarmaProximidad.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                muestraAviso("");
            }
        });
        mShaker = new ShakeListener(this);
        mShaker.setOnShakeListener(new ShakeListener.OnShakeListener() {
            public void onShake() {
                Tools.sound(getApplicationContext(), RingtoneManager.TYPE_ALARM, 2000);
            }
        });
        imageButtonPanic=(ImageButton)findViewById(R.id.imageButtonPanic);
        imageButtonPanic.setOnClickListener(new ImageButton.OnClickListener(){
            @Override
            public void onClick(View v) {
                panic();
            }
        });
    }

    //*****************************************************************************************************************
    private void panic() {
    }


    //*****************************************************************************************************************
    private void muestraAviso(String str){
        if(textViewAviso!=null){
            textViewAviso.setText(str);

        }
    }
    //*****************************************************************************************************************
    private void setBeacons() {
        beaconManager = (BeaconManager) org.altbeacon.beacon.BeaconManager.getInstanceForApplication(this);
        beaconManager.setForegroundScanPeriod(1000L);
        beaconManager.setForegroundBetweenScanPeriod(0L);

        filtros.add(IBEACON_MASK);
        setFilterBeacon(filtros);
        beaconManager.setDebug(false);
    }

    //****************************************************************************************************************
    /*
     * This filter calculates its rssi on base of an auto regressive moving average (ARMA)
     * It needs only the current value to do this; the general formula is  n(t) = n(t-1) - c * (n(t-1) - n(t))
     * where c is a coefficient, that denotes the smoothness - the lower the value, the smoother the average
     * Note: a smoother average needs longer to "settle down"
     * Note: For signals, that change rather frequently (say, 1Hz or faster) and tend to vary more a recommended
     *       value would be 0,1 (that means the actual value is changed by 10% of the difference between the
     *       actual measurement and the actual average)
     *       For signals at lower rates (10Hz) a value of 0.25 to 0.5 would be appropriate
     * private static double DEFAULT_ARMA_SPEED = 0.1;     //How likely is it that the RSSI value changes?
                                                        //Note: the more unlikely, the higher can that value be
                                                        //      also, the lower the (expected) sending frequency,
                                                        //      the higher should that value be
     */
    private void setFilterBeacon(List<String> beacons) {
        try {
            /*
            BeaconManager.setRssiFilterImplClass(RunningAverageRssiFilter.class);
            RunningAverageRssiFilter.setSampleExpirationMilliseconds(5000l);
            BeaconManager.setRssiFilterImplClass(ArmaRssiFilter.class);

             */
            BeaconManager.setRssiFilterImplClass(ArmaRssiFilter.class);
            ArmaRssiFilter.setDEFAULT_ARMA_SPEED(0.1);
            //RunningAverageRssiFilter.setSampleExpirationMilliseconds(5000L);
            beaconManager.getBeaconParsers().clear();
            for (String b : beacons) {
                beaconManager.getBeaconParsers().add(new BeaconParser().
                        setBeaconLayout(b));
            }
        } catch (Exception e) {
            Log.d(TAG, "setFilterBeacon:" + e.toString());
        }
    }

    //****************************************************************************************************************
    private void checkPermisosBluetooth() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (this.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                        if (!this.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                            Tools.alerta(this, "This app needs background location access", "Please grant location access so this app can detect beacons in the background.");
                        } else {
                            Tools.alerta(this, "Funcionalidad limitada", "Since background location access has not been granted, this app will not be able to discover beacons in the background.  Please go to Settings -> Applications -> Permissions and grant background location access to this app.");
                        }
                    }
                }
            } else {
                if (!this.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                            PERMISSION_REQUEST_FINE_LOCATION);
                } else {
                    Tools.alerta(this, "Funcionalidad limitada", "Since location access has not been granted, this app will not be able to discover beacons.  Please go to Settings -> Applications -> Permissions and grant location access to this app.");
                }

            }
        }
    }

    //****************************************************************************************************************
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_FINE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "fine location permission granted");
                } else {
                    Tools.alerta(this, "Funcionalidad limitada", "Hasta que la localización no este permitida esta aplicación no podrá funcionar.");
                }
                return;
            }
            case PERMISSION_REQUEST_BACKGROUND_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "background location permission granted");
                } else {
                    Tools.alerta(this, "Funcionalidad limitada", "Hasta que la localización en background no este permitida esta aplicación no podrá funcionar.");
                }
                return;
            }
        }
    }

    //****************************************************************************************************************
    private void StopMandown() {
        if (mShaker != null) mShaker.pause();
    }

    //****************************************************************************************************************
    private void StartMandown() {
        if (mShaker != null) mShaker.resume();
    }

    //****************************************************************************************************************
    private void StopScanner() {
        try {
            beaconManager.removeAllRangeNotifiers();
            beaconManager.unbind(this);
        } catch (Exception e) {
            Log.d(TAG, "Stop:" + e.toString());
        }
    }

    //****************************************************************************************************************
    private void StartScanner() {
        try {
            beaconManager.addRangeNotifier(this);
            beaconManager.bind(this);
        } catch (Exception e) {
            Log.d(TAG, "Start:" + e.toString());
        }
    }

    //****************************************************************************************************************
    @Override
    public void onResume() {
        super.onResume();
        StartScanner();
        StartMandown();
    }

    @Override
    public void onPause() {
        super.onPause();
        StopScanner();
        StopMandown();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    //****************************************************************************************************************
    @Override
    public void onBeaconServiceConnect() {
        try {
            beaconManager.startRangingBeaconsInRegion(new Region("ZonaDeteccion", null, null, null));
        } catch (RemoteException e) {
            Log.d(TAG, "onBeaconServiceConnect:" + e.toString());
            throw new RuntimeException(e);
        }
    }

    //****************************************************************************************************************
    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        try {
            if (beacons.size() > 0) {
                Iterator itr = beacons.iterator();
                while (itr.hasNext()) {
                    Beacon beacon = (Beacon) itr.next();
                    Log.d(TAG, beacon.getBluetoothName()+","+beacon.getRunningAverageRssi()+","+beacon.getRssi()+","+beacon.getBluetoothAddress());
                    if(toggleButtonAlarmaProximidad.isChecked())
                        checkProximity(beacon, RSSI_MINIMO);
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "onBeaconServiceConnect:" + e.toString());
        }
    }

    //****************************************************************************************************************
    private void checkProximity(Beacon beacon, double rssi) {
        if (beacon.getRunningAverageRssi() >= rssi) {
            if (mapbeacons.containsKey(beacon.toString())) {
                Integer cont = mapbeacons.get(beacon.toString()) + 1;
                mapbeacons.put(beacon.toString(), cont);
                if (cont >= MAX_VECES_INZONE) {
                    mapbeacons.clear();
                    setAlarmaProximidad();
                }
            }else {
                mapbeacons.put(beacon.toString(), 0);
            }
        }else{
            if (mapbeacons.containsKey(beacon.toString())){
                mapbeacons.put(beacon.toString(), 0);
            }
        }
    }

    //****************************************************************************************************************
    private void setAlarmaProximidad() {
        Tools.vibra(this, 200);
        Tools.sound(getApplicationContext(), RingtoneManager.TYPE_RINGTONE, 1000);
        muestraAviso("Vehículo en proximidad!!!!!");

        }


    //****************************************************************************************************************
    private void verifyBluetooth() {
        try {
            if (!Tools.bluetoothAvailable()) {
                Tools.alerta(this, "Bluetooth no accesible", "Por favor, active el Bluetooth y reinicie la App.");
            }
        } catch (RuntimeException e) {
            Tools.alerta(this, "Bluetooth LE no implementado", "Imposible ejecutar la App");
        }

    }

}