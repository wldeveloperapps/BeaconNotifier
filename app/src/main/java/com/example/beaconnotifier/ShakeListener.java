package com.example.beaconnotifier;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

//********************************************************************************************************
public class ShakeListener implements SensorEventListener {
    private static final int FORCE_THRESHOLD = 350;
    private static final int TIME_THRESHOLD = 100;
    private static final int SHAKE_TIMEOUT = 500;
    private static final int SHAKE_DURATION = 1000;
    private static final int SHAKE_COUNT = 3;
    /*
    private static final int FORCE_THRESHOLD = 350;
    private static final int TIME_THRESHOLD = 100;
    private static final int SHAKE_TIMEOUT = 500;
    private static final int SHAKE_DURATION = 1000;
    private static final int SHAKE_COUNT = 3;
*/
    private SensorManager mSensorMgr;
    private float mLastX = -1.0f, mLastY = -1.0f, mLastZ = -1.0f;
    private long mLastTime;
    private OnShakeListener mShakeListener;
    private Context mContext;
    private int mShakeCount = 0;
    private long mLastShake;
    private long mLastForce;
    private boolean estadoHorizontal=false;

    private int forceThreshold = FORCE_THRESHOLD;
    private int timeThreshold = TIME_THRESHOLD;
    private int shakeTimeout = SHAKE_TIMEOUT;
    private int shakeDuration = SHAKE_DURATION;
    private int shakeCount = SHAKE_COUNT;

    //******************************************************************************************************************************************
    public interface OnShakeListener {
        public void onShake();
    }

    //******************************************************************************************************************************************
    public ShakeListener(Context context) {
        mContext = context;
        //resume();
    }

    //******************************************************************************************************************************************
    public ShakeListener(Context context, int _forceThreshold, int _timeThreshold, int _shakeTimeout, int _shakeDuration, int _shakeCount) {
        mContext = context;
        forceThreshold = _forceThreshold;
        timeThreshold = _timeThreshold;
        shakeTimeout = _shakeTimeout;
        shakeDuration = _shakeDuration;
        shakeCount = _shakeCount;
    }

    //******************************************************************************************************************************************
    public void setOnShakeListener(OnShakeListener listener) {
        mShakeListener = listener;
    }

    //******************************************************************************************************************************************
    public void resume() {
        mSensorMgr = (SensorManager) mContext.getSystemService(mContext.SENSOR_SERVICE);
        if (mSensorMgr == null) {
            throw new UnsupportedOperationException("Sensors not supported");
        }
        boolean supported = mSensorMgr.registerListener(this, mSensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
        if (!supported) {
            mSensorMgr.unregisterListener(this, mSensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
            throw new UnsupportedOperationException("Accelerometer not supported");
        }
    }

    //******************************************************************************************************************************************
    public void pause() {
        if (mSensorMgr != null) {
            mSensorMgr.unregisterListener(this, mSensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
            mSensorMgr = null;
        }
    }

    //******************************************************************************************************************************************
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    //******************************************************************************************************************************************
    private boolean calculate(float ax, float ay, float az) {
        boolean shake = false;
        try {
            long now = System.currentTimeMillis();
            if ((now - mLastForce) > shakeTimeout) {
                mShakeCount = 0;
            }
            if ((now - mLastTime) > timeThreshold) {
                long diff = now - mLastTime;
                float speed = Math.abs(ax + ay + az - mLastX - mLastY - mLastZ) / diff * 10000;
                if (speed > forceThreshold) {
                    if ((++mShakeCount >= shakeCount) && (now - mLastShake > shakeDuration)) {
                        mLastShake = now;
                        mShakeCount = 0;
                        shake = true;
                    }
                    mLastForce = now;
                }
                mLastTime = now;
                mLastX = ax;
                mLastY = ay;
                mLastZ = az;
            }
        } catch (Exception e) {
        }
        return shake;
    }

    //******************************************************************************************************************************************
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            //Log.d("ACC", String.format("X:%f,Y:%f,Z:%f", event.values[0], event.values[1], event.values[2]));
            if(!estadoHorizontal){
                if (calculate(event.values[0], event.values[1], event.values[2])) {
                    estadoHorizontal=true;
                }
            }else{
                if (checkHorizontal(event.values[0], event.values[1], event.values[2])) {
                    mShakeListener.onShake();
                }
                estadoHorizontal=false;
            }
        }
    }

    //******************************************************************************************************************************************
    private boolean checkHorizontal(float x, float y, float z) {
        if (Math.abs(y) < 1.0)
            return true;
        return false;
    }
}