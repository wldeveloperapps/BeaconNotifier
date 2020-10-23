//package org.altbeacon.beacon.service;
package com.example.beaconnotifier;

import android.os.SystemClock;
import android.util.Log;

import org.altbeacon.beacon.service.RssiFilter;

import java.util.ArrayList;

/**
 * This filter calculates its rssi on base of an kalman
 */
public class KalmanRssiFilter implements RssiFilter {

    private static final String TAG = "RunningKalmanRssiFilter";
    public static final double INITIAL_VARIANCE = 0.5;
    public static final double INITIAL_NOISE = 0.008;
    public static final long DEFAULT_SAMPLE_EXPIRATION_MILLISECONDS = 20000; /* 20 seconds */
    private static long sampleExpirationMilliseconds = DEFAULT_SAMPLE_EXPIRATION_MILLISECONDS;
    private static double initVariance = INITIAL_VARIANCE;
    private static double noise = INITIAL_NOISE;
    private ArrayList<Measurement> mMeasurements = new ArrayList<>();
    private double lastRssiValue = 0.0;

    //***********************************************************************************************************
    @Override
    public void addMeasurement(Integer rssi) {
        Measurement measurement = new Measurement();
        measurement.rssi = rssi;
        measurement.timestamp = SystemClock.elapsedRealtime();
        mMeasurements.add(measurement);
    }

    //***********************************************************************************************************
    @Override
    public boolean noMeasurementsAvailable() {
        return mMeasurements.size() == 0;
    }

    //***********************************************************************************************************
    @Override
    public int getMeasurementCount() {
        return mMeasurements.size();
    }

    //***********************************************************************************************************
    @Override
    public double calculateRssi() {
        try {
            refreshMeasurements();
            int size = mMeasurements.size();
            double kalmanGain;
            double variance = initVariance;
            double measurementNoise;
            double mean = mMeasurements.get(0).rssi;
            if (mMeasurements.size() > 1) {
                measurementNoise = variance(mMeasurements);
                for (Measurement value : mMeasurements) {
                    variance = variance + noise;
                    kalmanGain = variance / ((variance + measurementNoise));
                    mean = mean + kalmanGain * (value.rssi - mean);
                    variance = variance - (kalmanGain * variance);
                }
                lastRssiValue = mean;
            } else {
                mean = lastRssiValue;
            }
            Log.d(TAG, "Kakman filter size:" + size + ",rssi:" + mean);
            return Math.round(mean);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            throw e;
        }
    }

    //***********************************************************************************************************
    private synchronized void refreshMeasurements() {
        ArrayList<Measurement> newMeasurements = new ArrayList<>();
        for (Measurement measurement : mMeasurements) {
            if (SystemClock.elapsedRealtime() - measurement.timestamp < sampleExpirationMilliseconds)  newMeasurements.add(measurement);
        }
        mMeasurements = newMeasurements;
    }

    //***********************************************************************************************************
    private static class Measurement  {
        Integer rssi;
        long timestamp;
    }

    //***********************************************************************************************************
    public static void setKalmanFilterValues(double _variance, double _noise) {
        initVariance = _variance;
        noise = _noise;
    }

    //***********************************************************************************************************
    public static void setSampleExpirationMilliseconds(long newSampleExpirationMilliseconds) {
        sampleExpirationMilliseconds = newSampleExpirationMilliseconds;
    }

    //***********************************************************************************************************
    private static double variance(ArrayList<Measurement> values) {
        double sum = 0.0;
        double mean = 0.0;
        int tamano = values.size();
        for (Measurement num : values) sum += num.rssi;
        mean = sum / tamano;
        sum = 0.0;
        for (Measurement num : values) sum += Math.pow(num.rssi - mean, 2);
        return sum / (tamano - 1);
    }

}