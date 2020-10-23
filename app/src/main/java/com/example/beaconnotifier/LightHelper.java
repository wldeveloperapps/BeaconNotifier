package com.example.beaconnotifier;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.ArrayList;
import java.util.Collections;

public class LightHelper  {
    private LightHelper.OnLightListener mLigthListener=null;
    private Context mContext;
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private SensorEventListener lightEventListener;
    private int nivelOscuridad;
    private boolean dia=false;

    private ArrayList<Measurement> mMeasurements = new ArrayList<Measurement>();
    private final long CANTIDAD_DATOS=50;
    private long contador=CANTIDAD_DATOS;

    //******************************************************************************************************************************************
    private class Measurement implements Comparable<Measurement> {
        Integer lmx;
        @Override
        public int compareTo(Measurement arg0) {
            return lmx.compareTo(arg0.lmx);
        }
    }

    //******************************************************************************************************************************************
    private int calculateLmx() {
        int size = mMeasurements.size();
        int startIndex = 0;
        int endIndex = size -1;
        if (size > 2) {
            startIndex = size/10+1;
            endIndex = size-size/10-2;
        }
        double sum = 0;
        for (int i = startIndex; i <= endIndex; i++) {
            sum += mMeasurements.get(i).lmx;
        }
        double runningAverage = sum/(endIndex-startIndex+1);
       return (int) Math.round(runningAverage);
    }

    //******************************************************************************************************************************************
    private  void addMeasurement(Integer lmx) {
        if(contador<=0)return;
        Measurement measurement = new Measurement();
        measurement.lmx = lmx;
        mMeasurements.add(measurement);
        if(--contador<=0){
            Collections.sort(mMeasurements);
            if(calculateLmx()>nivelOscuridad){
                if(!dia){
                    if(mLigthListener!=null)mLigthListener.onLight(true);
                    dia=true;
                }
            }else{
                if(dia){
                    if(mLigthListener!=null)mLigthListener.onLight(false);
                    dia=false;
                }
            }
            mMeasurements.clear();
            contador=CANTIDAD_DATOS;
        }
    }

    //******************************************************************************************************************************************
    public interface OnLightListener {
        public void onLight(boolean luz);
    }
    //******************************************************************************************************************************************
    public LightHelper(Context context, int nOscuridad) {
        mContext = context;
        nivelOscuridad=nOscuridad;
        sensorManager = (SensorManager) mContext.getSystemService(mContext.SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        lightEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                addMeasurement((int)sensorEvent.values[0]);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };
    }
    //******************************************************************************************************************************************
    public void setOnLightListener(LightHelper.OnLightListener listener) {
        mLigthListener = listener;
    }

    public void Resume() {
        sensorManager.registerListener(lightEventListener, lightSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    public void Pause() {
        sensorManager.unregisterListener(lightEventListener);
    }

}
