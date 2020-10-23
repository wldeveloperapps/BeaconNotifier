package com.example.beaconnotifier;
//https://altbeacon.github.io/android-beacon-library/

import android.Manifest;
import android.location.Location;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.RemoteException;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.justadeveloper96.permissionhelper.PermissionHelper;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.service.ArmaRssiFilter;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

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

    private String deviceId = "123456789";
    //*****************************************************************************************************************
    private static final int MAX_VECES_INZONE = 2;
    private static final int RSSI_PROXIMITY_MINIMO = -95;
    private static final int RSSI_OWNERZONE_MINIMO = -65;
    private static final long TIMEOUT_CHK_OWNERZONE = 10000;
    private static final long TIMEOUT_CHK_PROXIMITY = 2000;
    private static final long TIMEOUT_PREALARM_CAIDA = 15000;
    private static final int SEND_GPS_MOVIMIENTO = 5000;
    private static final int SEND_GPS_PARADO = 30000;
    private static final long BEACON_SCAN_FOREGROUND = 1000L;
    private static final long BEACON_BETWEEN_SCAN_FOREGROUND = 0L;
    //*****************************************************************************************************************
    private final static String MQTT_APN = "tcp://smart-fisherman.cloudmqtt.com:1883";
    private final static String MQTT_USR = "awstecreu";
    private final static String MQTT_PASS = "awstecreu1234";
    private final static String MQTT_ROOT_TOPIC = "WILOC/SACYR";
    private final static String MQTT_DOWNLINK_TOPIC = "WILOC/SACYR/DOWNLINK";
    private final static String MQTT_CLIENTE = "CHK_SACYR";

    //*****************************************************************************************************************
    private BeaconManager beaconManager;
    private List<String> filtros = new ArrayList<String>();
    private String IBEACON_MASK = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";
    // private String IBEACON_MASK = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24,m:27-28=5749";
    //private String IBEACON_EMB ="m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24,m:20-21=04D2,m:22-23=162E";
    private String IBEACON_EMB = "m:2-3=0215,i:4-19,m:20-21=04D2,i:22-23,p:24-24";
    private final static double ARMA_COEFF = 1.0;
    private final static int NIVEL_LUZ = 50;
    private final static int INCREMENTO_RSSI_CUERPO = -10;
    private final static int BRILLO_MAX = 100;
    private final static int BRILLO_MIN = 3;
    private boolean onLuz = true;
    //*****************************************************************************************************************
    Map<String, Integer> mapbeacons = new HashMap<String, Integer>();
    private int rssiProximity = RSSI_PROXIMITY_MINIMO;
    private int rssiOwnerZone = RSSI_OWNERZONE_MINIMO;
    private int alarmaProximidad = 0;
    private CountDownTimer countDownTimerOWNERZONE;
    private CountDownTimer countDownTimerPROXIMITY;
    private ReentrantLock lockBeaconsOWNERZONE = new ReentrantLock();
    private Map<String, Integer> beaconOWNERZONE = new HashMap<String, Integer>();
    private boolean analizaProximidad = true;
    private boolean analizaOWNERZONE = true;
    private boolean firstTimeOWNERZONE = false;
    //*****************************************************************************************************************
    FragmentTransaction transaction;
    Fragment fragmentLocation;
    //*****************************************************************************************************************
    private ShakeHelper mShaker = null;
    private SoundHelper mSound = null;
    private LightHelper mLight = null;
    //*****************************************************************************************************************
    private TextView textViewAviso, textViewRssi;
    private ImageButton imageButtonPanic;
    private ImageButton imageButtonCancelManDown;
    private boolean onMovement;
    private RadioButton radioButtonGPS;
    private RadioButton radioButtonGPRS;
    private RadioButton radioButtonMOV;
    private SeekBar seekBarProximidad;
    //*****************************************************************************************************************
    private PermissionHelper permissionHelper;
    private Location lastLocation = null;
    private boolean accionCancelada = false;
    private List<String> lastBad = new ArrayList<>();
    //*****************************************************************************************************************
    private String mqttApn = MQTT_APN;
    private String mqttUsr = MQTT_USR;
    private String mqttPass = MQTT_PASS;
    private String mqttRootTopic = MQTT_ROOT_TOPIC;
    private String mqttCliente = MQTT_CLIENTE;
    private MqttHelper mqttClient = null;
    private boolean onConnectMqtt = false;
    private String downlinkRoot = "";


    //*****************************************************************************************************************
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Tools.setContext(getApplicationContext(), getWindow());
        setEntorno();
        setComponents();

        verifyBluetooth();
        checkPermisos();
        setBeacons();
        setFragments();

        setMqtt();
        deviceId = UUID.randomUUID().toString();
        muestraAviso("Sistema inicializado:" + deviceId);
    }

    //*****************************************************************************************************************
    private void setMqtt() {
        try {
            mqttClient = new MqttHelper(getApplicationContext(), mqttApn, mqttCliente, mqttUsr, mqttPass);
            mqttClient.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean b, String s) {
                    onConnectMqtt = true;
                    radioButtonGPRS.setChecked(onConnectMqtt);
                    setMqttSubscriptions();
                }

                @Override
                public void connectionLost(Throwable throwable) {
                    onConnectMqtt = false;
                    radioButtonGPRS.setChecked(onConnectMqtt);
                }

                @Override
                public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                    setMqttMessage(topic, mqttMessage.toString());
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

                }
            });
            if (mqttClient != null) mqttClient.connect();
            downlinkRoot = MQTT_DOWNLINK_TOPIC + "/" + deviceId;
        } catch (Exception e) {
            Error("setMqtt:" + e.toString());
        }
    }

    //*****************************************************************************************************************
    private void setMqttMessage(String topic, String message) {
        if (!topic.contains(downlinkRoot)) return;
        if (topic.contains("IMSG")) {
            Tools.soundRing(RingtoneManager.TYPE_RINGTONE, 500);
            muestraAviso(message);
            if (message.toLowerCase() == "apaga") {

            }
        }
    }

    //*****************************************************************************************************************
    private void setMqttSubscriptions() {
        try {
            mqttClient.subscribeToTopic(mqttRootTopic + "/#");
        } catch (Exception e) {
            Error("setMqttSubscriptions:" + e.toString());
        }
    }

    //*****************************************************************************************************************
    private void publica(String subtopic, String msg) {
        mqttClient.publish(mqttRootTopic + "/" + subtopic, msg, 0, false);
    }

    //*****************************************************************************************************************
    private void checkPermisos() {
        try {
            permissionHelper = new PermissionHelper(this).setListener(new PermissionHelper.PermissionsListener() {
                @Override
                public void onPermissionGranted(int request_code) {
                    Log.d(TAG, "onPermissionGranted:" + request_code);
                }

                @Override
                public void onPermissionRejectedManyTimes(List<String> rejectedPerms, int request_code) {
                    for (int i = 0; i < rejectedPerms.size(); i++) {
                        Log.d(TAG, "onPermissionRejected:" + rejectedPerms.get(i) + "," + request_code);
                    }
                }
            });
            String[] needed_permissions = new String[]{
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.INTERNET,
                    Manifest.permission.MODIFY_AUDIO_SETTINGS
            };
            permissionHelper.requestPermission(needed_permissions, 100);
        } catch (Exception e) {
            Error("checkPermisos:" + e.toString());
        }
    }

    //*****************************************************************************************************************
    private void setEntorno() {
        try {
            getSupportActionBar().hide();
            if (Build.VERSION.SDK_INT > 16) {
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        } catch (Exception e) {
            Error("setEntorno:" + e.toString());
        }
    }

    //*****************************************************************************************************************
    @Override
    public void onBackPressed() {
    }


    //*****************************************************************************************************************
    private void setFragments() {
        try {
            fragmentLocation = new FragmentLocation();
            Bundle bundle = new Bundle();
            bundle.putInt("gps_interval", SEND_GPS_PARADO);
            fragmentLocation.setArguments(bundle);
            getSupportFragmentManager().beginTransaction().add(R.id.contenedorFraments, fragmentLocation).commit();
            ((FragmentLocation) fragmentLocation).setFragmentLocationInterface(new FragmentLocation.FragmentLocationInterface() {
                @Override
                public void onFragmentLocation(Location location) {
                    radioButtonGPS.setChecked(false);
                    radioButtonGPS.setChecked(true);
                    lastLocation = location;
                    String out = new MqttData().buildJsonTrack(deviceId, "0", 0x80, 0x81, location.getLatitude(), location.getLongitude(), 0x42, Tools.getBatteryLevel());
                    publica("TRACK", out);
                }
            });
        } catch (Exception e) {
            Error("setFragments:" + e.toString());
        }
    }

    //***********************************************************************************************************
    private void GetConfig() {
        /*
        mqttApn=ConfigShared.getValue("MQTT_APN",MQTT_APN ,getApplication());
        if(mqttApn=="")mqttApn=MQTT_APN;
        mqttUsr=ConfigShared.getValue("MQTT_USR",MQTT_USR ,getApplication());
        if(mqttUsr=="")mqttUsr=MQTT_USR;
        mqttPass=ConfigShared.getValue("MQTT_PASS",MQTT_PASS ,getApplication());
        if(mqttPass=="")mqttPass=MQTT_PASS;
        mqttRootTopic=ConfigShared.getValue("MQTT_ROOT_TOPIC",MQTT_ROOT_TOPIC ,getApplication());
        if(mqttRootTopic=="")mqttRootTopic=MQTT_ROOT_TOPIC;

         */
    }

    //****************************************************************************************************************
    private void cambiaLocationParam(long interval, long fastInterval) {
        try {
            ((FragmentLocation) fragmentLocation).setLocationParam(interval, fastInterval, 0);
        } catch (Exception e) {
            Error("cambiaLocationParam:" + e.toString());
        }
    }


    //*****************************************************************************************************************
    private void setComponents() {
        try {
            radioButtonGPRS = (RadioButton) findViewById(R.id.radioButtonGPRS);
            radioButtonMOV = (RadioButton) findViewById(R.id.radioButtonMOV);
            radioButtonGPS = (RadioButton) findViewById(R.id.radioButtonGPS);
            textViewAviso = (TextView) findViewById(R.id.textViewAviso);
            textViewRssi = (TextView) findViewById(R.id.textViewRssi);
            textViewAviso.setMovementMethod(new ScrollingMovementMethod());
            mSound = new SoundHelper(this, R.raw.alarm1, R.raw.beep2);
            mLight = new LightHelper(this, NIVEL_LUZ);
            mLight.setOnLightListener(new LightHelper.OnLightListener() {
                @Override
                public void onLight(boolean luz) {
                    if (!luz) {
                        //    rssiProximity-=INCREMENTO_RSSI_CUERPO;
//                        rssiOwnerZone-=INCREMENTO_RSSI_CUERPO;
                        Tools.setBrillo(BRILLO_MIN);
                    } else {
                        //                      rssiProximity+=INCREMENTO_RSSI_CUERPO;
                        //                    rssiOwnerZone+=INCREMENTO_RSSI_CUERPO;
                        if (onMovement) Tools.setBrillo(BRILLO_MAX);
                    }
                    /*
                    if(rssiProximity>0)rssiProximity=0;
                    if(rssiOwnerZone>0)rssiOwnerZone=0;
                    seekBarProximidad.setProgress(rssiProximity);
                    */
                    Log.e(TAG, "luz:" + luz);
                    onLuz = luz;
                }
            });
            mShaker = new ShakeHelper(this);
            mShaker.setOnShakeListener(new ShakeHelper.OnShakeListener() {
                public void onShake() {
                    preAlarmaCaida();
                    new CountDownTimer(TIMEOUT_PREALARM_CAIDA, 500) {
                        public void onTick(long millisUntilFinished) {
                            if (accionCancelada) onFinish();
                        }

                        public void onFinish() {
                            if (!accionCancelada) alarmaCaida();
                            else alarmaCaidaCancelada();
                        }

                    }.start();
                }
            });
            mShaker.setOnMovementListener(new ShakeHelper.OnMovementListener() {
                @Override
                public void onMove(boolean mov) {
                    onMovement = mov;
                    radioButtonMOV.setChecked(mov);
                    if (mov) {
                        cambiaLocationParam(SEND_GPS_MOVIMIENTO, SEND_GPS_MOVIMIENTO);
                        if (onLuz) {
                            Tools.setBrillo(BRILLO_MAX);
                        }
                    } else {
                        cambiaLocationParam(SEND_GPS_PARADO, SEND_GPS_PARADO);
                        Tools.setBrillo(BRILLO_MIN);
                    }
                }
            });

            imageButtonPanic = (ImageButton) findViewById(R.id.imageButtonPanic);
            imageButtonPanic.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Tools.Tick();
                    panic();
                    return true;
                }
            });
            imageButtonPanic.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    muestraAviso("");
                }
            });

            imageButtonCancelManDown = (ImageButton) findViewById(R.id.imageButtonCancelarManDown);
            imageButtonCancelManDown.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    accionCancelada = true;
                }
            });
            textViewRssi.setText("" + rssiProximity + "dBm");
            seekBarProximidad = (SeekBar) findViewById(R.id.seekBarProximidad);
            seekBarProximidad.setProgress(Math.abs(rssiProximity));
            seekBarProximidad.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    rssiProximity = progress * -1;
                    textViewRssi.setText("" + rssiProximity + "dBm");
                    analizaOWNERZONE = rssiProximity == 0;
                }

                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });

            setButtonPanicActive(true);
            Tools.setBluetoothOnOffCycle(1000);
        } catch (Exception e) {
            Error("setComponents:" + e.toString());
        }
        onMovement = false;
        Tools.setBrillo(BRILLO_MIN);
    }

    //*****************************************************************************************************************
    private void setButtonPanicActive(boolean panico) {
        try {
            if (panico) {
                imageButtonCancelManDown.setVisibility(View.INVISIBLE);
                imageButtonPanic.setVisibility(View.VISIBLE);
            } else {
                imageButtonCancelManDown.setVisibility(View.VISIBLE);
                imageButtonPanic.setVisibility(View.INVISIBLE);
            }
        } catch (Exception e) {
            Error("setButtonPanicActive:" + e.toString());
        }
    }

    //*****************************************************************************************************************
    private void panic() {
        try {
            muestraAviso("Llamando a la central...");
            alarma(2000);
            Tools.llamar("666972966");
        } catch (Exception e) {
            Error("panic:" + e.toString());
        }
    }

    //*****************************************************************************************************************
    private void alarmaCaidaCancelada() {
        try {
            Tools.vibra(Tools.VIBRACION_TIPO_OFF);
            mSound.stopSound();
            setButtonPanicActive(true);
        } catch (Exception e) {
            Error("alarmaCaidaCancelada:" + e.toString());
        }
    }

    //*****************************************************************************************************************
    private void alarmaCaida() {
        try {
            muestraAviso("Enviando SMS de alerta de caida...");
            String lat = String.format("%f", lastLocation.getLatitude()).replace(",", ".");
            String lon = String.format("%f", lastLocation.getLongitude()).replace(",", ".");
            //Tools.sms(String.format("Alerta de Caida en http://maps.google.com/?q=%s,%s", lat, lon), "666972966");
            setButtonPanicActive(true);
        } catch (Exception e) {
            Error("alarmaCaida:" + e.toString());
        }

    }

    //*****************************************************************************************************************
    private void alarma(int msec) {
        Tools.vibra(Tools.VIBRACION_TIPO_1);
        mSound.playSound(-1, msec, mSound.S_ALARMA);
    }

    //*****************************************************************************************************************
    private void preAlarmaCaida() {
        try {
            muestraAviso("Prealerta de caida...");
            accionCancelada = false;
            setButtonPanicActive(false);
            alarma((int) TIMEOUT_PREALARM_CAIDA);
        } catch (Exception e) {
            Error("preAlarmaCaida:" + e.toString());
        }
    }

    //****************************************************************************************************************
    private void alarmaProximidad() {
        try {
            alarma(2000);
            muestraAviso("Alerta de proximidad...");
        } catch (Exception e) {
            Error("alarmaProximidad:" + e.toString());
        }

    }

    //****************************************************************************************************************
    private void preAlarmaProximidad() {
        try {

            mSound.playSound(-1, 600, mSound.S_BEEP);
            muestraAviso("Prealerta proximidad...");
        } catch (Exception e) {
            Error("preAlarmaProximidad:" + e.toString());
        }

    }


    //*****************************************************************************************************************
    private void muestraAviso(String str) {
        try {
            if (textViewAviso != null) {
                textViewAviso.setText(str);
            }
        } catch (Exception e) {
            Error("muestraAviso:" + e.toString());
        }
    }

    //*****************************************************************************************************************
    private void setBeacons() {
        try {
            beaconManager = (BeaconManager) BeaconManager.getInstanceForApplication(this);
            beaconManager.setForegroundScanPeriod(BEACON_SCAN_FOREGROUND);
            beaconManager.setForegroundBetweenScanPeriod(BEACON_BETWEEN_SCAN_FOREGROUND);
            filtros.add(IBEACON_EMB);
            setFilterBeacon(filtros);
            beaconManager.setDebug(false);
            analizaOWNERZONE = false;
            startCheckOWNERZONE();
            startCheckPROXIMITY();
        } catch (Exception e) {
            Error("setBeacons:" + e.toString());
        }
    }

    private void startCheckPROXIMITY() {
        try {
            countDownTimerPROXIMITY = new CountDownTimer(Long.MAX_VALUE, TIMEOUT_CHK_PROXIMITY) {
                public void onTick(long millisUntilFinished) {
                    switch (alarmaProximidad) {
                        case 0:
                            break;
                        case 1:
                        case 2:
                            preAlarmaProximidad();
                            break;
                        default:
                            alarmaProximidad--;
                            alarmaProximidad();
                    }
                }

                public void onFinish() {
                    start();
                }
            };
        } catch (Exception e) {
            Error("startCheckProximity:" + e.toString());
        }
    }

    private List<String> checkRepeated(List<String> list) {
        return lastBad = list;
    }

    private void startCheckOWNERZONE() {
        try {
            countDownTimerOWNERZONE = new CountDownTimer(Long.MAX_VALUE, TIMEOUT_CHK_OWNERZONE) {
                public void onTick(long millisUntilFinished) {
                    if (!firstTimeOWNERZONE) {
                        firstTimeOWNERZONE = true;
                        return;
                    }
                    if (!analizaOWNERZONE) return;
                    List<String> bad = checkRepeated(verifyBeaconsOWNERZONE());
                    if (bad.size() > 0) {
                        alarmaOWNERZONE(bad);
                        bad.clear();
                    }

                }

                public void onFinish() {
                    start();
                }
            };
            InflateWhiteList();
        } catch (Exception e) {
            Error("startCheckOWNERZONE:" + e.toString());
        }
    }

    private void InflateWhiteList() {
        //addBeaconWhileList("E1:E9:80:FD:25:11");
        addBeaconWhileList("0C:F3:EE:71:66:7C");
    }


    private void alarmaOWNERZONE(List<String> list) {
        if (list.size() > 0) {
            for (String b : list) {
                muestraAviso("Atención con el casco");
                Tools.soundRing(RingtoneManager.TYPE_RINGTONE, 600);
            }
        }
    }

    private void addBeaconWhileList(String addr) {
        try {
            lockBeaconsOWNERZONE.lock();
            if (!beaconOWNERZONE.containsKey(addr))
                beaconOWNERZONE.put(addr, 0);
            lockBeaconsOWNERZONE.unlock();
        } catch (Exception e) {
            Error("addBeaconWhileList:" + e.toString());
        }
    }

    private void delBeaconWhileList(String addr) {
        try {
            lockBeaconsOWNERZONE.lock();
            if (beaconOWNERZONE.containsKey(addr)) {
                beaconOWNERZONE.remove(addr);
            }
            lockBeaconsOWNERZONE.unlock();
        } catch (Exception e) {
            Error("delBeaconWhileList:" + e.toString());
        }
    }

    private void clearBeaconOWNERZONE() {
        try {
            lockBeaconsOWNERZONE.lock();
            beaconOWNERZONE.clear();
            lockBeaconsOWNERZONE.unlock();
        } catch (Exception e) {
            Error("clearBeaconOWNERZONE:" + e.toString());
        }
    }

    private void clearValuesBeaconOWNERZONE() {
        try {
            lockBeaconsOWNERZONE.lock();
            for (Map.Entry<String, Integer> entry : beaconOWNERZONE.entrySet()) {
                entry.setValue(0);
            }
            lockBeaconsOWNERZONE.unlock();
        } catch (Exception e) {
            Error("clearValuesBeaconOWNERZONE:" + e.toString());
        }
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

            //Average
            //BeaconManager.setRssiFilterImplClass(RunningAverageRssiFilter.class);
            //RunningAverageRssiFilter.setSampleExpirationMilliseconds(5000L);

            //Arma
            BeaconManager.setRssiFilterImplClass(ArmaRssiFilter.class);
            ArmaRssiFilter.setDEFAULT_ARMA_SPEED(ARMA_COEFF);

            //Kalman
            //BeaconManager.setRssiFilterImplClass(KalmanRssiFilter.class);
            //KalmanRssiFilter.setSampleExpirationMilliseconds(5000L);
            //KalmanRssiFilter.setKalmanFilterValues(0.5,0.008);

            beaconManager.getBeaconParsers().clear();
            for (String b : beacons) {
                beaconManager.getBeaconParsers().add(new BeaconParser().
                        setBeaconLayout(b));
            }
        } catch (Exception e) {
            Error("setFilterBeacon:" + e.toString());
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
            mLight.Pause();
            beaconManager.removeAllRangeNotifiers();
            beaconManager.unbind(this);
        } catch (Exception e) {
            Error("StopScanner:" + e.toString());
        }
    }

    //****************************************************************************************************************
    private void StartScanner() {
        try {
            mLight.Resume();
            beaconManager.addRangeNotifier(this);
            beaconManager.bind(this);
        } catch (Exception e) {
            Error("StartScanner:" + e.toString());
        }
    }

    //****************************************************************************************************************
    @Override
    public void onResume() {
        try {
            super.onResume();
            StartScanner();
            StartMandown();
            if (countDownTimerOWNERZONE != null) countDownTimerOWNERZONE.start();
            if (countDownTimerPROXIMITY != null) countDownTimerPROXIMITY.start();
        } catch (Exception e) {
            Error("onResume:" + e.toString());
        }
    }

    @Override
    public void onPause() {
        try {
            super.onPause();
            StopScanner();
            if (countDownTimerOWNERZONE != null) countDownTimerOWNERZONE.cancel();
            if (countDownTimerPROXIMITY != null) countDownTimerPROXIMITY.cancel();
            StopMandown();
        } catch (Exception e) {
            Error("onPause:" + e.toString());
        }
    }

    @Override
    protected void onDestroy() {
        try {
            super.onDestroy();
            permissionHelper.onDestroy();
            android.os.Process.killProcess(android.os.Process.myPid());
        } catch (Exception e) {
            Error("onDestroy:" + e.toString());
        }
    }

    //****************************************************************************************************************
    //****************************************************************************************************************
    //****************************************************************************************************************
    //****************************************************************************************************************
    //****************************************************************************************************************
    @Override
    public void onBeaconServiceConnect() {
        try {
            beaconManager.startRangingBeaconsInRegion(new Region("ZonaDeteccion", null, null, null));
            analizaOWNERZONE = true;
        } catch (RemoteException e) {
            Error("onBeaconServiceConnect:" + e.toString());
            throw new RuntimeException(e);
        }
    }

    //****************************************************************************************************************
    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        try {
            if (analizaProximidad) checkBeaconsProximity(beacons);
            if (analizaOWNERZONE) checkBeaconsOWNERZONE(beacons);
        } catch (Exception e) {
            Error("didRangeBeaconsInRegion:" + e.toString());
        }
    }

    //****************************************************************************************************************
    private boolean isBeaconOWNERZONE(Beacon beacon) {
        boolean result = false;
        lockBeaconsOWNERZONE.lock();
        result = beaconOWNERZONE.containsKey(beacon.getBluetoothAddress());
        lockBeaconsOWNERZONE.unlock();
        return result;
    }

    //****************************************************************************************************************
    private boolean checkAllBeacons(Beacon beacon) {
        try {
            if (isBeaconOWNERZONE(beacon)) {
                return true;
            } else {
                if (beacon.getRunningAverageRssi() >= rssiProximity) {
                    alarmaProximidad++;
                    return false;
                }
            }
        } catch (Exception e) {
            Error("checkProximity:" + e.toString());
        }
        return true;
    }

    //****************************************************************************************************************
    private boolean _checkAllBeacons(Beacon beacon) {
        try {
            if (isBeaconOWNERZONE(beacon)) {
                return true;
            } else {
                if (beacon.getRunningAverageRssi() >= rssiProximity) {
                    if (mapbeacons.containsKey(beacon.toString())) {
                        Integer cont = mapbeacons.get(beacon.toString()) + 1;
                        mapbeacons.put(beacon.toString(), cont);
                        if (cont >= MAX_VECES_INZONE) {
                            mapbeacons.clear();
                            alarmaProximidad++;
                            return false;
                        }
                    } else {
                        mapbeacons.put(beacon.toString(), 0);
                    }
                } else {
                    if (mapbeacons.containsKey(beacon.toString())) {
                        mapbeacons.put(beacon.toString(), 0);
                    }
                }
            }
        } catch (Exception e) {
            Error("checkProximity:" + e.toString());
        }
        return true;
    }

    //****************************************************************************************************************
    private void checkBeaconsProximity(Collection<Beacon> beacons) {
        try {
            String str = "";
            if (beacons.size() > 0) {
                Iterator itr = beacons.iterator();
                while (itr.hasNext()) {
                    Beacon beacon = (Beacon) itr.next();
                    Log.d(TAG, beacon.getBluetoothName() + "," + Math.round(beacon.getRunningAverageRssi()) + "," + beacon.getRssi() + "," + beacon.getBluetoothAddress());
                    if (!checkAllBeacons(beacon)) return;
                }
            }
            alarmaProximidad=0;
        } catch (Exception e) {
            Error("checkBeaconsProximity:" + e.toString());
        }
    }

    //****************************************************************************************************************
    private List<String> verifyBeaconsOWNERZONE() {
        List<String> bad = new ArrayList<String>();
        try {
            lockBeaconsOWNERZONE.lock();
            for (Map.Entry<String, Integer> entry : beaconOWNERZONE.entrySet()) {
                String key = entry.getKey();
                int value = entry.getValue();
                if (value <= 0) bad.add(key);
                entry.setValue(0);
            }
            lockBeaconsOWNERZONE.unlock();
        } catch (Exception e) {
            Error("verifyBeaconsOWNERZONE:" + e.toString());
        }
        return bad;
    }


    //****************************************************************************************************************
    private void checkBeaconsOWNERZONE(Collection<Beacon> beacons) {
        lockBeaconsOWNERZONE.lock();
        try {
            if (beacons.size() > 0 && beaconOWNERZONE.size() > 0) {
                Iterator itr = beacons.iterator();
                while (itr.hasNext()) {
                    Beacon beacon = (Beacon) itr.next();
                    if (beaconOWNERZONE.containsKey(beacon.getBluetoothAddress())) {
                        if (beacon.getRunningAverageRssi() >= rssiOwnerZone) {
                            Integer cont = beaconOWNERZONE.get(beacon.getBluetoothAddress()) + 1;
                            beaconOWNERZONE.put(beacon.getBluetoothAddress(), cont);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Error("checkBeaconsOWNERZONE:" + e.toString());
        }
        lockBeaconsOWNERZONE.unlock();
    }

    //****************************************************************************************************************
    private void verifyBluetooth() {
        try {
            if (!Tools.bluetoothAvailable()) {
                Tools.alerta("Bluetooth no accesible", "Por favor, active el Bluetooth y reinicie la App.");
            }
        } catch (RuntimeException e) {
            Tools.alerta("Bluetooth LE no implementado", "Imposible ejecutar la App");
        }

    }

    //   @Override
//    public boolean onTouchEvent(MotionEvent ev) {
//        return super.onTouchEvent(ev);
//    }
    //****************************************************************************************************************
    private void Error(String msg) {
        Log.e(TAG, msg);
        //publica("ERROR",serialNumber+","+msg);

    }

}