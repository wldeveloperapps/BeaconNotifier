package com.example.beaconnotifier;

/*
gradle
dependencies {
implementation 'no.nordicsemi.android.support.v18:scanner:1.4.2'
implementation 'com.neovisionaries:nv-bluetooth:1.8'

}
manifest
    <uses-permission android:name="android.permission.BLUETOOTH" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
    <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

https://github.com/TakahikoKawasaki/nv-bluetooth
 */

import android.os.CountDownTimer;
import android.util.Log;

import androidx.annotation.NonNull;

import com.neovisionaries.bluetooth.ble.advertising.ADPayloadParser;
import com.neovisionaries.bluetooth.ble.advertising.ADStructure;
import com.neovisionaries.bluetooth.ble.advertising.EddystoneEID;
import com.neovisionaries.bluetooth.ble.advertising.EddystoneTLM;
import com.neovisionaries.bluetooth.ble.advertising.EddystoneUID;
import com.neovisionaries.bluetooth.ble.advertising.EddystoneURL;
import com.neovisionaries.bluetooth.ble.advertising.IBeacon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

public class BeaconScanner {
    protected static final String TAG = "BeaconScanner";
    private double KF_R = 0.065;
    private double KF_Q = 1.4;
    private double KF_A = 1.0;
    private double KF_B = 0.0;
    private double KF_C = 1.0;
    private final static long TIMEOUT_RECONNECT = 1500000;//25 minutes
    private KalmanFilter KFInitial = new KalmanFilter(KF_R, KF_Q, KF_A, KF_B, KF_C);
    private Map<String, KalmanFilter> beaconKFilter = new HashMap<>();
    private BluetoothLeScannerCompat scanner;
    private List<ScanFilter> filters = new ArrayList<>();
    private ScanSettings settings;
    private boolean scanRunning = false;
    private boolean conFiltro=true;
    private CountDownTimer countDownTimerReconnect;
    private BeaconScanner.OnScanListener scanListener=null;
    private Beacon lastBeacon =new Beacon();

    public class Beacon{
        public int filteredRssi;
        public ScanResult beaconInfo;
    }

    //******************************************************************************************************************************************
    public interface OnScanListener {
        void onScan(Beacon b);
    }

    //********************************************************************************************************************
    private class KalmanFilter {
        private double A = 1;
        private double B = 0;
        private double C = 1;
        private double R;
        private double Q;
        private double cov = Double.NaN;
        private double x = Double.NaN;
        /**
         * Constructor
         *
         * @param R Process noise (0.008)
         * @param Q Measurement noise (deviation standard,3 or 4 con movimiento 20)
         * @param A State vector .How a previous state effects a new state
         * @param B Control vector. Here B is the control parameter and is multiplied by the control/action on each filter step
         * @param C Measurement vector
         *          R models the process noise and describes how noisy our system internally is
         *          Or, in other words, how much noise can we expect from the system itself
         *          As our system is constant we can set this to a (very) low value.
         *          Q resembles the measurement noise; how much noise is caused by our measurements
         *          As we expect that our measurements will contain most of the noise,
         *          it makes sense to set this parameter to a high number (especially in comparison to the process noise).
         *          The lower Q, the faster the system responds to changes (as it trusts the measurement more) but the more noisy it is.
         */
        public KalmanFilter(double R, double Q, double A, double B, double C) {
            this.R = R;
            this.Q = Q;

            this.A = A;
            this.B = B;
            this.C = C;

            this.cov = Double.NaN;
            this.x = Double.NaN; // estimated signal without noise
        }
        /**
         * Constructor
         *
         * @param R Process noise
         * @param Q Measurement noise
         */
        public KalmanFilter(double R, double Q) {
            this.R = R;
            this.Q = Q;

        }
        /**
         * Filters a measurement
         *
         * @param measurement The measurement value to be filtered
         * @param u           The controlled input value
         * @return The filtered value
         */
        public double filter(double measurement, double u) {
            try {
                if (Double.isNaN(this.x)) {
                    this.x = (1 / this.C) * measurement;
                    this.cov = (1 / this.C) * this.Q * (1 / this.C);
                } else {
                    double predX = (this.A * this.x) + (this.B * u);
                    double predCov = ((this.A * this.cov) * this.A) + this.R;
                    // Kalman gain
                    double K = predCov * this.C * (1 / ((this.C * predCov * this.C) + this.Q));
                    // Correction
                    this.x = predX + K * (measurement - (this.C * predX));
                    this.cov = predCov - (K * this.C * predCov);
                }
            } catch (Exception e) {
                throw e;
            }
            return this.x;
        }

        /**
         * Filters a measurement
         *
         * @param measurement The measurement value to be filtered
         * @return The filtered value
         */
        public double filter(double measurement) {
            return filter(measurement, 0);
        }
        public double getLastCov() {
            return this.cov;
        }
        public double getLastX() {
            return this.x;
        }
        /**
         * Filters a measurement
         */
        public double filter(double measurement, double lastX, double lastCov) {
            this.x = lastX;
            this.cov = lastCov;
            return filter(measurement, 0);
        }

    }

    //******************************************************************************************************************************************
    public void setOnScanListener(BeaconScanner.OnScanListener listener) {
        scanListener = listener;
    }

    //********************************************************************************************************************
    public void startScanner(){
        try {
            if (!scanRunning) {
                beaconKFilter.clear();
                countDownTimerReconnect.start();
                scanRunning = true;
                scanner.startScan(filters, settings, mScanCallback);
            }
        } catch (Exception e) {
            throw e;
        }
    }

    //********************************************************************************************************************
    public void stopScanner(){
        try {
            if (scanRunning) {
                scanner.stopScan(mScanCallback);
                scanRunning = false;
                countDownTimerReconnect.cancel();
            }
        } catch (Exception e) {
            throw e;
        }
    }

    //********************************************************************************************************************
    private void initScanner(int scanMode,boolean withFilter){
        conFiltro=withFilter;
        scanner = BluetoothLeScannerCompat.getScanner();
        settings = new ScanSettings.Builder()
                .setScanMode(scanMode)
                .setReportDelay(0)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setUseHardwareBatchingIfSupported(true)
                .setUseHardwareFilteringIfSupported(true)
                .setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
                .setLegacy(false)
                .build();
        countDownTimerReconnect = new CountDownTimer(TIMEOUT_RECONNECT, TIMEOUT_RECONNECT / 2) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                scanner.stopScan(mScanCallback);
                scanner.startScan(filters, settings, mScanCallback);
                start();
            }
        };
    }

    //********************************************************************************************************************
    private static byte[] toBytes(short s) {
        return new byte[]{(byte)(s & 0x00FF),(byte)((s & 0xFF00)>>8)};
    }

    //********************************************************************************************************************
    public BeaconScanner(int scanMode,boolean cfiltro) {
        initScanner(scanMode,cfiltro);
    }

    //********************************************************************************************************************
    public BeaconScanner() {
        initScanner(ScanSettings.SCAN_MODE_LOW_LATENCY,conFiltro);
    }

    //********************************************************************************************************************
    public void setKalmanMeasurementNoise(double noise) {
        try {
            KF_Q = noise;
            KFInitial = new KalmanFilter(KF_R, KF_Q, KF_A, KF_B, KF_C);
            beaconKFilter.clear();
        } catch (Exception e) {
            throw e;
        }
    }

    //********************************************************************************************************************
    public void setKalmanProcessNoise(double noise) {
        try {
            KF_R = noise;
            KFInitial = new KalmanFilter(KF_R, KF_Q, KF_A, KF_B, KF_C);
            beaconKFilter.clear();
        } catch (Exception e) {
            throw e;
        }
    }

    //********************************************************************************************************************
    public void setMacFilter(List<String> macs) {
        for(String mac : macs){
            filters.add(new ScanFilter.Builder().setDeviceAddress(mac).build());
        }
    }

    //********************************************************************************************************************
    public void setibeaconMinorFilter(int manufacturer, int minor) {
        try {
            // Empty data
            byte[] manData = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            // Data Mask
            byte[] mask = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0};
            System.arraycopy(toBytes((short)minor), 0, manData, 20, 2);
            // Add data array to filters
            filters.add(new ScanFilter.Builder().setManufacturerData(manufacturer, manData, mask).build());
        } catch (Exception e) {
            throw  e;
        }
    }

    //********************************************************************************************************************
    public void setibeaconMajorFilter(int manufacturer, int major) {
        try {
            // Empty data
            byte[] manData = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            // Data Mask
            byte[] mask = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0};
            System.arraycopy(toBytes((short)major), 0, manData, 18, 2);
            // Add data array to filters
            filters.add(new ScanFilter.Builder().setManufacturerData(manufacturer, manData, mask).build());
        } catch (Exception e) {
            throw e;
        }
    }

    //********************************************************************************************************************
    public  void setibeaconMajorMinorFilter(int manufacturer, int major, int minor) {
        try {
            // Empty data
            byte[] manData = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            // Data Mask
            byte[] mask = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0};
            System.arraycopy(toBytes((short)major), 0, manData, 18, 2);
            System.arraycopy(toBytes((short)minor), 0, manData, 20, 2);
            // Add data array to filters
            filters.add(new ScanFilter.Builder().setManufacturerData(manufacturer, manData, mask).build());
        } catch (Exception e) {
            throw e;
        }
    }

    //********************************************************************************************************************
    public void setUUIDFilter(int manufacturer, String uuid) {
        try {
            // Empty data
            byte[] manData = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            // Data Mask
            byte[] mask = new byte[]{0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0};
            // Copy UUID into data array and remove all "-"
            System.arraycopy(Tools.hexStringToByteArray(uuid.replace("-", "")), 0, manData, 2, 16);
            // Add data array to filters
            filters.add(new ScanFilter.Builder().setManufacturerData(manufacturer, manData, mask).build());
        } catch (Exception e) {
            throw e;
        }
    }

    //********************************************************************************************************************
    public static IBeacon parseIBeacon(Beacon b){
        List<ADStructure> structures =
                ADPayloadParser.getInstance().parse(b.beaconInfo.getScanRecord().getBytes());
        for (ADStructure structure : structures) {
            // If the ADStructure instance can be cast to IBeacon.
            if (structure instanceof IBeacon) {
                // An iBeacon was found.
                return (IBeacon) structure;
            }
        }
        return null;
    }
    //********************************************************************************************************************
    public static EddystoneUID  parseEddystoneUID(Beacon b){
        List<ADStructure> structures =
                ADPayloadParser.getInstance().parse(b.beaconInfo.getScanRecord().getBytes());
        for (ADStructure structure : structures) {
            // If the ADStructure instance can be cast to IBeacon.
            if (structure instanceof EddystoneUID) {
                // An iBeacon was found.
                return (EddystoneUID ) structure;
            }
        }
        return null;
    }
    //********************************************************************************************************************
    public static EddystoneURL parseEddystoneURL (Beacon b){
        List<ADStructure> structures =
                ADPayloadParser.getInstance().parse(b.beaconInfo.getScanRecord().getBytes());
        for (ADStructure structure : structures) {
            // If the ADStructure instance can be cast to IBeacon.
            if (structure instanceof EddystoneUID) {
                // An iBeacon was found.
                return (EddystoneURL  ) structure;
            }
        }
        return null;
    }
    //********************************************************************************************************************
    public static EddystoneTLM parseEddystoneTLM (Beacon b){
        List<ADStructure> structures =
                ADPayloadParser.getInstance().parse(b.beaconInfo.getScanRecord().getBytes());
        for (ADStructure structure : structures) {
            // If the ADStructure instance can be cast to IBeacon.
            if (structure instanceof EddystoneUID) {
                // An iBeacon was found.
                return (EddystoneTLM   ) structure;
            }
        }
        return null;
    }
    //********************************************************************************************************************
    public static EddystoneEID  parseEddystoneEID  (Beacon b){
        List<ADStructure> structures =
                ADPayloadParser.getInstance().parse(b.beaconInfo.getScanRecord().getBytes());
        for (ADStructure structure : structures) {
            // If the ADStructure instance can be cast to IBeacon.
            if (structure instanceof EddystoneUID) {
                // An iBeacon was found.
                return (EddystoneEID) structure;
            }
        }
        return null;
    }

   //********************************************************************************************************************
    private final ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(final int callbackType, @NonNull final ScanResult result) {
            try {
               lastBeacon.filteredRssi=addRssiFilter(result.getDevice().getAddress(), result.getRssi());
               lastBeacon.beaconInfo=result;
               scanListener.onScan(lastBeacon);
            } catch (Exception e) {
               throw e;
            }
        }

        @Override
        public void onBatchScanResults(@NonNull final List<ScanResult> results) {
            try {
                for (int i = 0; i < results.size(); i++) {
                    lastBeacon.filteredRssi=addRssiFilter(results.get(i).getDevice().getAddress(), results.get(i).getRssi());
                    lastBeacon.beaconInfo=results.get(i);
                    scanListener.onScan(lastBeacon);
                }
            } catch (Exception e) {
                throw e;
            }
        }

        @Override
        public void onScanFailed(final int errorCode) {
            Log.e(TAG, "onScanFailed " + errorCode);
        }
    };

    //****************************************************************************************************************
    private int addRssiFilter(String addr, int rssi) {
        int filterMeasurement = rssi;
        try {
            if (conFiltro) {
                if (beaconKFilter.containsKey(addr)) {
                    KalmanFilter KF = beaconKFilter.get(addr);
                    filterMeasurement = (int) Math.round(KF.filter( rssi));
                    beaconKFilter.put(addr, KF);
                } else {
                    beaconKFilter.put(addr, KFInitial);
                }
                Log.d(TAG, "FILTER:" + addr + "," + rssi + "," + filterMeasurement);
            }
        } catch (Exception e) {
            throw  e;
        }
        return filterMeasurement;
    }

}
