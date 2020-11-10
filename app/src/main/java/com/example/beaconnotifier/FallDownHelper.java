package com.example.beaconnotifier;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

//********************************************************************************************************
public class FallDownHelper implements SensorEventListener {
    /*
    private static final int FORCE_THRESHOLD = 350;
    private static final int TIME_THRESHOLD = 100;
    private static final int SHAKE_TIMEOUT = 500;
    private static final int SHAKE_DURATION = 1000;
    private static final int SHAKE_COUNT = 3;
    */
    private static final int FORCE_MOV = 150;
    private static final int FORCE_THRESHOLD = 350;
    private static final int TIME_THRESHOLD = 100;
    private static final int SHAKE_TIMEOUT = 500;
    private static final int SHAKE_DURATION = 1000;
    private static final int SHAKE_COUNT = 3;

    private static final int MOV_COUNT = 10;
    private static final long MOV_TIMEOUT = 30000;
    private static final int TIME_GUARD = 5000; //Tiempo para que no se repita el impacto despues de dispararlo
    private static final int TIME_PRESHAKE = 5000;//Tiempo para ver si esta en posicion horizontal o la persona se ha levantado
    private static final int EJE_HORIZONTAL = 1;//eje Y
    private static final double EJE_HORIZONTAL_MAX_VALUE = 3.0;

    private static final int NUMERO_MUESTRAS_HORIZONTAL = 15;
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
    private boolean mGuardTime = false;
    private OnShakeListener mFallDownListener;
    private OnMovementListener mMovementListener;
    private Context mContext;
    private int mShakeCount = 0;
    private int mMovCount = 0;
    private long mLastShake;
    private long mLastMov;
    private long mLastForce;


    private boolean chkestadoHorizontal = false;

    private int forceThreshold = FORCE_THRESHOLD;
    private int timeThreshold = TIME_THRESHOLD;
    private int shakeTimeout = SHAKE_TIMEOUT;
    private int shakeDuration = SHAKE_DURATION;
    private int shakeCount = SHAKE_COUNT;
    private long timePreShake=TIME_PRESHAKE;
    private long timeGuard=TIME_GUARD;

    private int forceMovement = FORCE_MOV;
    private int movCount = MOV_COUNT;
    private long movTimeOut = MOV_TIMEOUT;


    private boolean movement = false;

    //******************************************************************************************************************************************
    public interface OnShakeListener {
        void onShake();
    }

    public interface OnMovementListener {
        void onMove(boolean mov);
    }

    //******************************************************************************************************************************************
    public FallDownHelper(Context context) {
        mContext = context;
        //resume();
    }

    //******************************************************************************************************************************************
    public FallDownHelper(Context context, int _forceThreshold, int _forceMovement, int _timeThreshold, int _shakeTimeout, int _shakeDuration, int _shakeCount, long _timePreShake, long _timeGuard, int _movCount, long _movTimeOut) {
        mContext = context;
        forceThreshold = _forceThreshold;
        timeThreshold = _timeThreshold;
        shakeTimeout = _shakeTimeout;
        shakeDuration = _shakeDuration;
        shakeCount = _shakeCount;
        forceMovement = _forceMovement;
        movCount = _movCount;
        movTimeOut = _movTimeOut;
        timePreShake=_timePreShake;
        timeGuard=_timeGuard;
    }

    //******************************************************************************************************************************************
    public void setOnShakeListener(OnShakeListener listener) {
        mFallDownListener = listener;
    }

    public void setOnMovementListener(OnMovementListener listener) {
        mMovementListener = listener;
    }

    //******************************************************************************************************************************************
    public void resume() {
        mSensorMgr = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        movement = false;
        if (mSensorMgr == null) {
            throw new UnsupportedOperationException("Sensors not supported");
        }
        boolean supported = mSensorMgr.registerListener(this, mSensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
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
    private void checkMovement(boolean ahora, boolean antes) {
        if(mMovementListener==null || antes==ahora)return;
        Log.d("ACC", "mov:" + ahora);
        //if (antes) mMovementListener.onMove(false);
        //else mMovementListener.onMove(true);
    }

    //******************************************************************************************************************************************
    /*
    1.- Se debe detectar una caida libre durante u tiempo mínimo, luego se debe de esperar un impacto y por último esperar un tiempo y detrectar si existe movilidad
     */
    private double mLastacc=0.0;
    private long firstTimeFreeFall;
    private long firstTimeImpact;
    private long firstTimeFalseAlarm;
    private long firstTimeStop;
    private static final double FREE_FALL_ACC = 0.1;    //resultante de la caida libre
    private static final double IMPACT_ACC =1.2;        //impacto
    private static final double MOV_ACC_H =1.1;
    private static final double MOV_ACC_L =0.75;
    private static final double FREE_FALL_TIME = 100;   //319
    private static final double IMPACT_TIME = 2000;
    private static final double FALSE_ALARM_TIME = 5000;
    private static final double STOP_TIME = 5000;
    private int st_steep=0;

    private boolean calcula(double ax, double ay, double az){
        long now = System.currentTimeMillis();
        double acc= Math.sqrt(ax*ax+ay*ay+az*az)/9.80665;
        boolean result=false;

        movement=acc < MOV_ACC_L || acc > MOV_ACC_H;
        if(movement)firstTimeStop=now;
        else if(now-firstTimeStop>STOP_TIME){

        }
        switch(st_steep) {
            case 0://wait for free fall
                if (acc < FREE_FALL_ACC) {
                    st_steep++;
                    firstTimeFreeFall = now;
                } else {

                }
                break;
            case 1://free fall status
                Log.d("ACC", "acc0:" + acc);
                if (acc < FREE_FALL_ACC) {
                    if (now - firstTimeFreeFall > FREE_FALL_TIME) {
                        st_steep++;
                        firstTimeImpact = now;
                    }
                } else {
                    st_steep = 0;
                }
                break;
            case 2://impact
                Log.d("ACC", "acc1:" + acc);
                if (acc > IMPACT_ACC) {
                    st_steep++;
                    firstTimeFalseAlarm = now;
                } else {
                    if (now - firstTimeImpact >= IMPACT_TIME) {
                        st_steep = 0;
                    }
                }
                break;
            case 3://false alarm wait time
                Log.d("ACC", "acc2:" + acc);
                if (now - firstTimeFalseAlarm >= FALSE_ALARM_TIME) {
                    st_steep++;
                }
                break;
            case 4://check false alarm
                if(!movement)st_steep++;
                else  st_steep = 0;
            break;
            case 5://alarm real
                Log.d("ACC", "ALARMA:" + acc);
                st_steep = 0;
                result=true;
             break;
            default:
             st_steep = 0;
        }
        return result;
    }

    //******************************************************************************************************************************************
    private boolean calculate(float ax, float ay, float az) {
        boolean shake = false;
        boolean lastmov = movement;
        try {
            long now = System.currentTimeMillis();
            if ((now - mLastForce) > shakeTimeout) {
                mShakeCount = 0;
            }
            if ((now - mLastTime) > timeThreshold) {
                long diff = now - mLastTime;
                float speed = Math.abs(ax + ay + az - mLastX - mLastY - mLastZ) / diff * 10000;
                if (speed > forceMovement) {
                    mLastMov = 0;
                    if (++mMovCount >= movCount) {
                        mMovCount = movCount;
                        movement = true;
                    }
                } else {
                    if (--mMovCount <= 0) {
                        if (mLastMov == 0) mLastMov = now;
                        else {
                            if (now - mLastMov > movTimeOut) {
                                movement = false;
                                mLastMov = 0;
                            }
                        }
                        mMovCount = 0;
                    }
                }
                if (movement != lastmov) checkMovement(movement, lastmov);
                if (speed > forceThreshold) {
                    if ((++mShakeCount >= shakeCount) && (now - mLastShake > shakeDuration)) {
                        mLastShake = now;
                        mShakeCount = 0;
                        if (!movement) {
                            movement = true;
                            checkMovement(movement, false);
                        }
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
            throw e;
        }
        return shake;
    }

    //******************************************************************************************************************************************
    /*
    @Override
    public void onSensorChanged(final SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // Log.d("ACC", String.format("X:%f,Y:%f,Z:%f", event.values[0], event.values[1], event.values[2]));
            if (!chkestadoHorizontal) {
                if (calcula(event.values[0], event.values[1], event.values[2])) {
                    chkestadoHorizontal = true;
                    new CountDownTimer(timePreShake, 1000) {
                        public void onTick(long millisUntilFinished) {
                            if (!checkHorizontal(event.values[EJE_HORIZONTAL])) {
                                chkestadoHorizontal = false;
                                cancel();
                            }
                        }
                        public void onFinish() {
                            mGuardTime = true;
                            mShakeListener.onShake();
                        }
                    }.start();
                }
            } else {
                if (mGuardTime == true) {
                    mGuardTime = false;
                    new CountDownTimer(timeGuard, timeGuard / 2) {
                        public void onTick(long millisUntilFinished) {
                        }
                        public void onFinish() {
                            chkestadoHorizontal =false;
                        }
                    }.start();
                }
            }
        }
    }

     */

    @Override
    public void onSensorChanged(final SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
             if (calcula(event.values[0], event.values[1], event.values[2])) {
                 mFallDownListener.onShake();
             }
        }
    }


    //*****************************************************************************************************************************************
    private boolean checkHorizontal(float eje) {
        //Log.d("ACC", String.format("checkHorizontal"));
        return (Math.abs(eje) < EJE_HORIZONTAL_MAX_VALUE);
    }
}