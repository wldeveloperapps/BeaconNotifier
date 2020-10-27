package com.example.beaconnotifier;
//https://altbeacon.github.io/android-beacon-library/

import android.Manifest;
import android.location.Location;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
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

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

//****************************************************************************************************************
public class MainActivity extends AppCompatActivity {
    protected static final String TAG = "LCF";

    private String deviceId = "";
    //*****************************************************************************************************************
    private static final int MAX_VECES_INZONE = 2;
    private static final int RSSI_PROXIMITY_MINIMO = -86;
    private static final int RSSI_OWNERZONE_MINIMO = -65;
    private static final long TIMEOUT_CHK_OWNERZONE = 10000;
    private static final long TIMEOUT_CHK_PROXIMITY = 500;
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
    private BluetoothLeScannerCompat scanner;
    private List<ScanFilter> filters = new ArrayList<>();
    private ScanSettings settings;
    private boolean scanRunning = false;


    private final static int NIVEL_LUZ = 10;
    private final static int INCREMENTO_RSSI_CUERPO = -10;
    private final static int BRILLO_MAX = 100;
    private final static int BRILLO_MIN = 3;
    private boolean onLuz = true;
    //*****************************************************************************************************************
    Map<String, Integer> mapbeacons = new HashMap<>();
    private int rssiProximity = RSSI_PROXIMITY_MINIMO;
    private int rssiOwnerZone = RSSI_OWNERZONE_MINIMO;
    private  boolean hayProximidad = false;
    private boolean hayDatos = false;
    //
    private final static double ARMA_COEFF = 0.6;
    private boolean conFiltro = true;
    //
    private final static int ST_REPOSO = 0;
    private final static int ST_PREALARMA_1 = ST_REPOSO + 1;
    private final static int ST_PREALARMA_2 = ST_REPOSO + 2;
    private final static int ST_PREALARMA_3 = ST_REPOSO + 3;
    private final static int ST_ALARMA = ST_REPOSO + 4;
    private int estado = ST_REPOSO;
    private CountDownTimer countDownTimerOWNERZONE;
    private CountDownTimer countDownTimerPROXIMITY;
    private ReentrantLock lockBeaconsOWNERZONE = new ReentrantLock();
    private Map<String, Integer> beaconOWNERZONE = new HashMap<>();
    private boolean analizaProximidad = true;
    private boolean analizaOWNERZONE = false;
    private boolean firstTimeOWNERZONE = false;
    //*****************************************************************************************************************
    private double armaSpeed = ARMA_COEFF;
    private boolean armaisInitialized = false;
    private Map<String, Integer> beaconARMA = new HashMap<>();
    //*****************************************************************************************************************
    FragmentTransaction transaction;
    Fragment fragmentLocation;
    //*****************************************************************************************************************
    private ShakeHelper mShaker = null;
    private SoundHelper mSound = null;
    private List<Integer> soundCollection = new ArrayList<>();
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
                    Manifest.permission.MODIFY_AUDIO_SETTINGS,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
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
            soundCollection.add(R.raw.alarm1);
            soundCollection.add(R.raw.beep2);
            mSound = new SoundHelper(this, soundCollection);
            mLight = new LightHelper(this, NIVEL_LUZ);
            mLight.setOnLightListener(new LightHelper.OnLightListener() {
                @Override
                public void onLight(boolean luz) {
                    if (!luz) {
                        //    rssiProximity-=INCREMENTO_RSSI_CUERPO;
                        //    rssiOwnerZone-=INCREMENTO_RSSI_CUERPO;
                        Tools.setBrillo(BRILLO_MIN);
                    } else {
                        //    rssiProximity+=INCREMENTO_RSSI_CUERPO;
                        //    rssiOwnerZone+=INCREMENTO_RSSI_CUERPO;
                        if (onMovement) Tools.setBrillo(BRILLO_MAX);
                    }
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
                        //Tools.setBrillo(BRILLO_MIN);
                        Tools.setBrillo(BRILLO_MAX);
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
                }
                public void onStartTrackingTouch(SeekBar seekBar) {
                }
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
            setButtonPanicActive(true);
            //Tools.setBluetoothOnOffCycle(1000);
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
        mSound.playSound(-1, msec, R.raw.alarm1);
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
            mSound.playSound(-1, 600, R.raw.beep2);
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
/*
    ScanFilters
    You can pass a list of ScanFilters.
    They define what beacon advertisements you want to be notified of, when in range. You can create a ScanFilter with a ScanFilter.Builder. You can filter on many parameters.
    If you want to filter based on the iBeacon spec, it's best to use the
    ScanFilter.Builder setManufacturerData (int manufacturerId, byte[] manufacturerData, byte[] manufacturerDataMask). You can filter with it on uuid, major and minor.
    As you see you need to set an int and 2 byte arrays. The manufacturerId is not very important. But the manufacturerData is! Y
    ou can create a byte array that matches the byte array that is advertised by the beacon!
    And to make it even more cool, you can use the manufacturerDataMask to tell what bytes should match, and what bytes shouldn't.
    This way you can scan for all beacons with a fixed uuid, but any major or minor! Any combination is possible really.
    This utils class showcases how to create such byte arrays.
    */
    private void setBeacons() {
        try {
            scanner = BluetoothLeScannerCompat.getScanner();
            settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setReportDelay(0)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .setUseHardwareBatchingIfSupported(true)
                    .setUseHardwareFilteringIfSupported(false)
                    .build();
            filters.add(new ScanFilter.Builder().setDeviceAddress("0C:F3:EE:71:66:7C").build());
            filters.add(new ScanFilter.Builder().setDeviceAddress("0C:F3:EE:72:A9:41").build());
            //filters.add(setUUIDFilter("699EBC80-E1F3-11E3-9A0F-0CF3EE3BC012"));
            //filters.add(setibeaconMajorFilter("04D2"));
            startCheckOWNERZONE();
            startCheckPROXIMITY();
        } catch (Exception e) {
            Error("setBeacons:" + e.toString());
        }
    }

    private ScanFilter setibeaconMajorFilter(String  major) {
        // Empty data
        byte[] manData = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        // Data Mask
        byte[] mask = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0};
        // Copy UUID into data array and remove all "-"
        System.arraycopy(Tools.hexStringToByteArray(major), 0, manData, 18, 2);
        // Add data array to filters
        return new ScanFilter.Builder().setManufacturerData(76, manData, mask).build();
    }
    private ScanFilter setibeaconMinorFilter(String  minor) {
        // Empty data
        byte[] manData = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        // Data Mask
        byte[] mask = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0};
        // Copy UUID into data array and remove all "-"
        System.arraycopy(Tools.hexStringToByteArray(minor), 0, manData, 20, 2);
        // Add data array to filters
        return new ScanFilter.Builder().setManufacturerData(76, manData, mask).build();
    }

    private ScanFilter setibeaconMajorMinorFilter(String  major,String minor) {
        // Empty data
        byte[] manData = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        // Data Mask
        byte[] mask = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0};
        // Copy UUID into data array and remove all "-"
        System.arraycopy(Tools.hexStringToByteArray(major), 0, manData, 18, 2);
        System.arraycopy(Tools.hexStringToByteArray(minor), 0, manData, 20, 2);
        // Add data array to filters
        return new ScanFilter.Builder().setManufacturerData(76, manData, mask).build();
    }

    private ScanFilter setUUIDFilter(String uuid) {
        // Empty data
        byte[] manData = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        // Data Mask
        byte[] mask = new byte[]{0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0};
        // Copy UUID into data array and remove all "-"
        System.arraycopy(Tools.hexStringToByteArray(uuid.replace("-", "")), 0, manData, 2, 16);
        // Add data array to filters
        return new ScanFilter.Builder().setManufacturerData(76, manData, mask).build();
    }

    private void startCheckPROXIMITY() {
        try {
            countDownTimerPROXIMITY = new CountDownTimer(Long.MAX_VALUE, TIMEOUT_CHK_PROXIMITY) {
                public void onTick(long millisUntilFinished) {
                    Log.d(TAG, "D:" + getHayDatos() + ",P:"+getHayProximidad());
                    if (getHayDatos()) {
                        switch (estado) {
                            case ST_REPOSO:
                                if (getHayProximidad()) {
                                    estado = ST_PREALARMA_1;
                                    preAlarmaProximidad();
                                }
                                break;
                            case ST_PREALARMA_1:
                                if (getHayProximidad()) {
                                    estado = ST_PREALARMA_2;
                                    preAlarmaProximidad();
                                } else {
                                    estado = ST_REPOSO;
                                }
                                break;
                            case ST_PREALARMA_2:
                                if (getHayProximidad()) {
                                    estado = ST_ALARMA;
                                    preAlarmaProximidad();
                                } else {
                                    estado = ST_PREALARMA_1;
                                }
                            case ST_PREALARMA_3:
                                if (getHayProximidad()) {
                                    estado = ST_ALARMA;
                                    preAlarmaProximidad();
                                } else {
                                    estado = ST_PREALARMA_2;
                                }
                            case ST_ALARMA:
                                if (getHayProximidad()) {
                                    alarmaProximidad();
                                } else {
                                    estado = ST_PREALARMA_3;
                                }
                                break;
                            default:
                                estado = ST_REPOSO;
                        }
                        setHayProximidad(false);
                        setHayDatos(false);
                    } else {
                        muestraAviso("");
                        estado = ST_REPOSO;
                        hayProximidad = false;
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
    //**************************************************************************************
    private synchronized boolean getHayProximidad(){
        return hayProximidad;
    }
    private synchronized void setHayProximidad(boolean set){
        hayProximidad=set;
    }
    private synchronized boolean getHayDatos(){
        return hayDatos;
    }
    private synchronized void setHayDatos(boolean set){
        hayDatos=set;
    }
    //**************************************************************************************

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

            if (scanRunning) {
                scanner.stopScan(mScanCallback);
                scanRunning = false;
            }
            if (countDownTimerOWNERZONE != null) countDownTimerOWNERZONE.cancel();
            if (countDownTimerPROXIMITY != null) countDownTimerPROXIMITY.cancel();

        } catch (Exception e) {
            Error("StopScanner:" + e.toString());
        }
    }

    //****************************************************************************************************************
    private void StartScanner() {
        try {
            if (!scanRunning) {
                beaconARMA.clear();
                scanner.startScan(filters, settings, mScanCallback);
                scanRunning = true;
            }
            if (countDownTimerOWNERZONE != null) countDownTimerOWNERZONE.start();
            if (countDownTimerPROXIMITY != null) countDownTimerPROXIMITY.start();

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
            mLight.Resume();
        } catch (Exception e) {
            Error("onResume:" + e.toString());
        }
    }

    @Override
    public void onPause() {
        try {
            super.onPause();
            StopScanner();
            StopMandown();
            mLight.Pause();
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
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(final int callbackType, final ScanResult result) {
            try {
                addRssiFilter(result.getDevice().getAddress(), result.getRssi());
                if (analizaProximidad) checkAllBeacons(result);
                if (analizaOWNERZONE) checkBeaconsOWNERZONE(result);
                //Log.d(TAG, result.getDevice().getAddress() + "," + result.getRssi());
            } catch (Exception e) {
                Error("onScanResult:" + e.toString());
            }
        }

        @Override
        public void onBatchScanResults(final List<ScanResult> results) {
            try {
                for (int i = 0; i < results.size(); i++) {
                    addRssiFilter(results.get(i).getDevice().getAddress(), results.get(i).getRssi());
                    if (analizaProximidad) checkAllBeacons(results.get(i));
                    if (analizaOWNERZONE) checkBeaconsOWNERZONE(results.get(i));
                }
            } catch (Exception e) {
                Error("onBatchScanResults:" + e.toString());
            }
        }

        @Override
        public void onScanFailed(final int errorCode) {
            Log.e(TAG, "onScanFailed " + errorCode);
        }
    };

    //****************************************************************************************************************
    private boolean isBeaconOWNERZONE(ScanResult beacon) {
        boolean result = false;
        try {

            lockBeaconsOWNERZONE.lock();
            result = beaconOWNERZONE.containsKey(beacon.getDevice().getAddress());
            lockBeaconsOWNERZONE.unlock();

        } catch (Exception e) {
            Error("isBeaconOWNERZONE:" + e.toString());
        }
        return result;
    }

    //****************************************************************************************************************
    private void checkAllBeacons(ScanResult beacon) {
        try {
            if (isBeaconOWNERZONE(beacon)) {
            } else {
                setHayDatos(true);
                if (getRssiFilter(beacon.getDevice().getAddress(), beacon.getRssi()) >= rssiProximity) {
                    setHayProximidad(true);
                } else {
                }
            }
        } catch (Exception e) {
            Error("checkProximity:" + e.toString());
        }
    }

    //****************************************************************************************************************
    private void _checkAllBeacons(ScanResult beacon) {
        try {
            if (isBeaconOWNERZONE(beacon)) {
            } else {
                setHayDatos(true);
                String addr = beacon.getDevice().getAddress();
                if (getRssiFilter(addr, beacon.getRssi()) >= rssiProximity) {
                    if (mapbeacons.containsKey(addr)) {
                        Integer cont = mapbeacons.get(addr) + 1;
                        //Log.d(TAG, addr + "," + cont + "," + beacon.getRssi());
                        mapbeacons.put(addr, cont);
                        if (cont >= MAX_VECES_INZONE) {
                            mapbeacons.clear();
                            setHayProximidad(true);
                        }
                    } else {
                        mapbeacons.put(addr, 0);
                    }
                } else {
                    if (mapbeacons.containsKey(addr)) {
                        mapbeacons.put(addr, 0);
                    }
                }
            }
        } catch (Exception e) {
            Error("checkProximity:" + e.toString());
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
    private void checkBeaconsOWNERZONE(ScanResult beacon) {
        lockBeaconsOWNERZONE.lock();
        try {
            if (beaconOWNERZONE.size() > 0) {
                String addr = beacon.getDevice().getAddress();
                if (beaconOWNERZONE.containsKey(addr)) {
                    if (getRssiFilter(addr, beacon.getRssi()) >= rssiOwnerZone) {
                        Integer cont = beaconOWNERZONE.get(addr) + 1;
                        beaconOWNERZONE.put(addr, cont);
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

    //****************************************************************************************************************
    //****************************************************************************************************************

    private void clearRssiFilter() {
        beaconARMA.clear();
    }

    //****************************************************************************************************************
    private int getRssiFilter(String addr, int rssi) {
        if (conFiltro) {
            if (beaconARMA.containsKey(addr))
                return beaconARMA.get(addr);
        }
        return rssi;
    }

    //****************************************************************************************************************
    private int addRssiFilter(String addr, int rssi) {
        int armaMeasurement = rssi;
        try {
            if (conFiltro) {
                if (beaconARMA.containsKey(addr)) {
                    armaMeasurement = beaconARMA.get(addr);
                    armaMeasurement = Double.valueOf(armaMeasurement - armaSpeed * (armaMeasurement - rssi)).intValue();
                }
                Log.d(TAG, "ARMA:" + addr + "," + rssi + "," + armaMeasurement + "=" + (rssi - armaMeasurement));
                beaconARMA.put(addr, armaMeasurement);
            }
        } catch (Exception e) {
            Error("addRssiFilter:" + e.toString());
        }
        return armaMeasurement;
    }
    //****************************************************************************************************************


}