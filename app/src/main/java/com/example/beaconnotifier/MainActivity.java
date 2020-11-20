package com.example.beaconnotifier;
//https://altbeacon.github.io/android-beacon-library/

import android.Manifest;
import android.location.Location;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.justadeveloper96.permissionhelper.PermissionHelper;
import com.neovisionaries.bluetooth.ble.advertising.IBeacon;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

//****************************************************************************************************************
public class MainActivity extends AppCompatActivity {
    protected static final String TAG = "LCF";


    private String deviceId = "";
    //*****************************************************************************************************************
    private static final int MAX_VECES_INZONE = 2;
    private static final int RSSI_PROXIMITY_H_MINIMO = -88;
    private static final int RSSI_PROXIMITY_L_MINIMO = -88;
    private static final int RSSI_OWNERZONE_MINIMO = -65;
    private static final long TIMEOUT_CHK_OWNERZONE = 10000;
    private static final long TIMEOUT_CHK_PROXIMITY = 1500;
    private static final long TIMEOUT_PREALARM_CAIDA = 15000;
    private static final int SEND_GPS_MOVIMIENTO = 5000;
    private static final int SEND_GPS_PARADO = 30000;
    private static final long BEACON_SCAN_FOREGROUND = 1000L;
    private static final String EMB_OUI_1 = "0C:F3:EE";
    private static final String EMB_OUI_2 = "E0:18:9F";
    private static final long BEACON_BETWEEN_SCAN_FOREGROUND = 0L;
    //*****************************************************************************************************************
    private final static String MQTT_APN = "tcp://smart-fisherman.cloudmqtt.com:1883";
    private final static String MQTT_USR = "awstecreu";
    private final static String MQTT_PASS = "awstecreu1234";
    private final static String MQTT_ROOT_TOPIC = "WILOC/SACYR";
    private final static String MQTT_DOWNLINK_TOPIC = "WILOC/SACYR/DOWNLINK";
    private final static String MQTT_CLIENTE = "CHK_SACYR";

    private final static String FMQTT_APN = "tcp://mqtt.flespi.io:1883";
    private final static String FMQTT_USR = "fWG9bnAyMpI5OBQZBMlj3RbgTYsYxljC5Oz71ZtBeZuRoaGQKPZDx1KgP03abaA5";
    private final static String FMQTT_PASS = "";
    private final static String FMQTT_ROOT_TOPIC = "flespi/message/gw/channels/33037";
    private final static String FMQTT_CLIENTE = "";

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
    private int rssiOwnerZone = RSSI_OWNERZONE_MINIMO;
    private boolean hayProximidad = false;
    private boolean hayDatos = false;
    //
    private final static long TIMEOUT_RECONNECT = 900000;//15 minutes
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
    private CountDownTimer _countDownTimerPROXIMITY;
    private CountDownTimer countDownTimerReconnect;
    private ReentrantLock lockBeaconsOWNERZONE = new ReentrantLock();
    private Map<String, Integer> beaconOWNERZONE = new HashMap<>();
    private boolean analizaProximidad = true;
    private boolean analizaOWNERZONE = false;
    private boolean firstTimeOWNERZONE = false;
    //*****************************************************************************************************************
    FragmentTransaction transaction;
    Fragment fragmentLocation;
    //*****************************************************************************************************************
    private BeaconScanner bs;
    //*****************************************************************************************************************
    private FallDownHelper mShaker = null;
    private SoundHelper mSound = null;
    private List<Integer> soundCollection = new ArrayList<>();
    private LightHelper mLight = null;
    //*****************************************************************************************************************
    private TextView textViewAviso, textViewLastRssi;
    private ImageButton imageButtonPanic;
    private ImageButton imageButtonCancelManDown;
    private Button cancelaProximidad;
    private boolean onMovement;
    private RadioButton radioButtonGPS;
    private RadioButton radioButtonGPRS;
    private RadioButton radioButtonMOV;
    //*****************************************************************************************************************
    private TextView textViewRssiE, textViewRssiI, textViewARMA, textViewMaxTramas;
    private SeekBar seekBarProximidadE, seekBarProximidadI, seekBarProximidadARMA, seekBarMaxTramas;
    private EditText editTextNumberDecimalQ;
    private int rssiProximityE = RSSI_PROXIMITY_H_MINIMO;
    private int rssiProximityI = RSSI_PROXIMITY_L_MINIMO;
    private int lastRssi = 0;
    private int tramasE = 0, tramasI = 0;
    private int maxTramas = 1;

    private boolean armaisInitialized = false;

    private final static String FILE_DATA = "/storage/emulated/0/Download/KalmanData";
    private List<String> dataRssiCollection = new ArrayList<>();
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
    private  android.app.Activity curActivity;

    //*****************************************************************************************************************
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Tools.setContext(getApplicationContext(), getWindow());
        curActivity=this;
        setEntorno();
        setComponents();
        verifyBluetooth();
        checkPermisos();
        //setBeacons();
        setBeaconsDecawave();
        setFragments();
        //setMqtt();
        setFlespiMqtt();
        deviceId = UUID.randomUUID().toString();
        deviceId = "123456789";
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
                public void messageArrived(String topic, MqttMessage mqttMessage) {
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
    private void setFlespiMqtt() {
        try {
            mqttClient = new MqttHelper(getApplicationContext(), FMQTT_APN, FMQTT_CLIENTE, FMQTT_USR, FMQTT_PASS);
            mqttClient.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean b, String s) {
                    onConnectMqtt = true;
                    radioButtonGPRS.setChecked(onConnectMqtt);
                    setFlespiMqttSubscriptions();
                }

                @Override
                public void connectionLost(Throwable throwable) {
                    onConnectMqtt = false;
                    radioButtonGPRS.setChecked(onConnectMqtt);
                }

                @Override
                public void messageArrived(String topic, MqttMessage mqttMessage) {
                    setFlespiMqttMessage(topic, mqttMessage.toString());

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
    private void setFlespiMqttMessage(String topic, String message) {

    }
    //*****************************************************************************************************************
    private void setFlespiMqttSubscriptions() {
        try {
        } catch (Exception e) {
            Error("setMqttSubscriptions:" + e.toString());
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
    private void publicaFlespi() {
        String payload = "[{ \"address\":{ \"ident\":\"359633109606256\",\"type\":\"connection\"},\"name\":\"codec12\",\"properties\":{ \"payload\":\"setdigout 1 1\"},\"ttl\":86400}]";
        String topic = "flespi/rest/post/gw/channels/33037/commands-queue/";
        mqttClient.publish( topic, payload, 0, false);
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
            String[] needed_permissions = new String[0];
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                needed_permissions = new String[]{
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                        Manifest.permission.CALL_PHONE,
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.INTERNET,
                        Manifest.permission.MODIFY_AUDIO_SETTINGS,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                };
            }
            permissionHelper.requestPermission(needed_permissions, 100);
        } catch (Exception e) {
            Error("checkPermisos:" + e.toString());
        }
    }

    //*****************************************************************************************************************
    private void setEntorno() {
        try {
            getSupportActionBar().hide();
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
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
            radioButtonGPRS = findViewById(R.id.radioButtonGPRS);
            radioButtonMOV = findViewById(R.id.radioButtonMOV);
            radioButtonGPS = findViewById(R.id.radioButtonGPS);
            textViewAviso = findViewById(R.id.textViewAviso);
            cancelaProximidad = findViewById(R.id.toggleButtonCancelarAlarmaProximidad);
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

            mShaker = new FallDownHelper(this);
            mShaker.setOnShakeListener(new FallDownHelper.OnShakeListener() {
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
            mShaker.setOnMovementListener(new FallDownHelper.OnMovementListener() {
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

            imageButtonPanic = findViewById(R.id.imageButtonPanic);
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

            imageButtonCancelManDown = findViewById(R.id.imageButtonCancelarManDown);
            imageButtonCancelManDown.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    accionCancelada = true;
                }
            });
            cancelaProximidad.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });
            setProgressControls();
            setButtonPanicActive(true);
            //Tools.setBluetoothOnOffCycle(1000);
        } catch (Exception e) {
            Error("setComponents:" + e.toString());
        }
        onMovement = false;
        Tools.setBrillo(BRILLO_MAX);
    }

    //*****************************************************************************************************************
    private void setProgressControls() {
        textViewRssiE = findViewById(R.id.textViewRssiH);
        textViewRssiI = findViewById(R.id.textViewRssiL);

        textViewLastRssi = findViewById(R.id.textViewRssi);
        textViewMaxTramas = findViewById(R.id.textViewMaxTramas);
        textViewRssiE.setText("Ext:" + rssiProximityE + "dBm");
        textViewRssiI.setText("Int:" + rssiProximityI + "dBm");

        textViewMaxTramas.setText("Tramas:" + maxTramas);
        textViewLastRssi.setText("");
        seekBarProximidadE = findViewById(R.id.seekBarProximidadH);
        seekBarProximidadI = findViewById(R.id.seekBarProximidadL);
        seekBarMaxTramas = findViewById(R.id.seekBarMaxTramas);
        seekBarProximidadE.setProgress(Math.abs(rssiProximityE));
        seekBarProximidadI.setProgress(Math.abs(rssiProximityI));

        seekBarProximidadE.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                rssiProximityE = progress * -1;
                if (rssiProximityE > rssiProximityI) {
                    seekBarProximidadI.setProgress(seekBarProximidadE.getProgress());
                }
                textViewRssiE.setText("Ext:" + rssiProximityE + "dBm");
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        seekBarProximidadI.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                rssiProximityI = progress * -1;
                if (rssiProximityI <= rssiProximityE) {
                    seekBarProximidadE.setProgress(seekBarProximidadI.getProgress());
                }
                textViewRssiI.setText("Int:" + rssiProximityI + "dBm");
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        seekBarMaxTramas.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                maxTramas = progress;
                if (maxTramas <= 0) maxTramas = 1;
                textViewMaxTramas.setText("Tramas:" + maxTramas);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
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
            //alarma(2000);
            //Tools.llamar("666972966");
            //publicaFlespi();
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
    private void setBeacons() {
        try {
            bs = new BeaconScanner();
            bs.setibeaconMajorFilter(76, 0x04D2);
            bs.setOnScanListener(new BeaconScanner.OnScanListener() {
                @Override
                public void onScan(BeaconScanner.Beacon b) {
                    checkAllBeacons(b);
                    IBeacon ib = bs.parseIBeacon(b);
                    textViewLastRssi.setText("" + b.filteredRssi + "dBm");
                }
            });
            startCheckOWNERZONE();
            startCheckPROXIMITY();
        } catch (Exception e) {
            Error("setBeacons:" + e.toString());
        }
    }
    class DW{
        public byte len1;
        public byte type1;
        public byte data1;
        public byte len2;
        public byte type2;
        public byte[] uuid=new byte[16];
        public byte data2;
        public byte len3;
        public byte type3;
        public byte[] data3=new byte[5];
    }
    //*****************************************************************************************************************
    private void setBeaconsDecawave() {
        List<String> mac=new ArrayList<>();
        mac.add("D9:04:E0:64:35:1A");
        try {
            bs = new BeaconScanner();
            bs.setMacFilter(mac);
            DWPans2Ble2 dwPans2= new DWPans2Ble2(curActivity,true);
            dwPans2.setCharacteristicReadListener(new DWPans2Ble2.OnCharacteristicReadListener() {
                @Override
                public void onCharteristicRead() {
                    Log.d(TAG,"Distancias...");
                    if(dwPans2.getDistancias().size()>0){

                    }
                    dwPans2.readDistances();
                }
            });
            dwPans2.setOnConnectListener(new DWPans2Ble2.OnConnectListener() {
                                             @Override
                                             public void onConnected(boolean connected) {
                                                 if(connected){

                                                 }
                                             }
                                         });

            dwPans2.setOnServiceListener(new DWPans2Ble2.OnServiceListener() {
                                             @Override
                                             public void onService(boolean ok, int status) {
                                                if(ok){

                                                }
                                             }
                                         });
            bs.setOnScanListener(new BeaconScanner.OnScanListener() {
                @Override
                public void onScan(BeaconScanner.Beacon b) {
                    bs.stopScanner();
                    dwPans2.connect(b.beaconInfo.getDevice());

                }
            });
        } catch (Exception e) {
            Error("setBeacons:" + e.toString());
        }
    }

    private void _startCheckPROXIMITY() {
        try {
            _countDownTimerPROXIMITY = new CountDownTimer(Long.MAX_VALUE, TIMEOUT_CHK_PROXIMITY) {
                public void onTick(long millisUntilFinished) {
                    Log.d(TAG, "D:" + getHayDatos() + ",P:" + getHayProximidad());
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
                        setHayProximidad(false);
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

    private void startCheckPROXIMITY() {
        try {
            countDownTimerPROXIMITY = new CountDownTimer(Long.MAX_VALUE, TIMEOUT_CHK_PROXIMITY) {
                public void onTick(long millisUntilFinished) {
                    if (getHayDatos()) {
                        int tI = getTramasI();
                        int tE = getTramasE();
                        //Log.d(TAG, "I:" + tI + " E:" + tE);
                        if (tI >= maxTramas) {
                            muestraAviso("Alarma Proximidad");
                        } else if (tE >= maxTramas) {
                            muestraAviso("Prealarma");
                        } else {
                            muestraAviso("I:" + tI + "<<E:" + tE);
                        }
                        setHayDatos(false);
                    } else {
                        muestraAviso("");
                        textViewLastRssi.setText("");
                        decTramas();
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
    private synchronized boolean getHayProximidad() {
        return hayProximidad;
    }

    private synchronized void setHayProximidad(boolean set) {
        hayProximidad = set;
    }

    private synchronized boolean getHayDatos() {
        return hayDatos;
    }

    private synchronized void setHayDatos(boolean set) {
        hayDatos = set;
    }

    private synchronized void incTramasI() {
        tramasI++;
        if (tramasI > maxTramas) tramasI = maxTramas;
    }

    private synchronized void decTramasI() {
        tramasI--;
        if (tramasI < 0) tramasI = 0;
    }

    private synchronized void decTramas() {
        tramasI--;
        if (tramasI < 0) tramasI = 0;
        tramasE--;
        if (tramasE < 0) tramasE = 0;
    }


    private synchronized void incTramasE() {
        tramasE++;
        if (tramasE > maxTramas) tramasE = maxTramas;
    }

    private synchronized void decTramasE() {
        tramasE--;
        if (tramasE < 0) tramasE = 0;
    }

    private synchronized int getTramasE() {
        return tramasE;
    }

    private synchronized int getTramasI() {
        return tramasI;
    }

    private synchronized void setTramasI(int value) {
        tramasI = value;
    }

    private synchronized void setTramasE(int value) {
        tramasE = value;
    }

    private synchronized void clrTramas() {
        tramasI = tramasE = 0;
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
            bs.stopScanner();
            if (countDownTimerOWNERZONE != null) countDownTimerOWNERZONE.cancel();
            if (countDownTimerPROXIMITY != null) countDownTimerPROXIMITY.cancel();

        } catch (Exception e) {
            Error("StopScanner:" + e.toString());
        }
    }

    //****************************************************************************************************************
    private void StartScanner() {
        try {
            bs.startScanner();
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
            //String file = FILE_DATA + Calendar.getInstance().getTime().getTime() + ".txt";
            //salvaFileData(file, dataRssiCollection);
            super.onDestroy();
            permissionHelper.onDestroy();
            android.os.Process.killProcess(android.os.Process.myPid());
        } catch (Exception e) {
            Error("onDestroy:" + e.toString());
        }
    }


    //****************************************************************************************************************
    private boolean isBeaconOWNERZONE(BeaconScanner.Beacon beacon) {
        boolean result = false;
        try {

            lockBeaconsOWNERZONE.lock();
            result = beaconOWNERZONE.containsKey(beacon.beaconInfo.getDevice().getAddress());
            lockBeaconsOWNERZONE.unlock();

        } catch (Exception e) {
            Error("isBeaconOWNERZONE:" + e.toString());
        }
        return result;
    }

    //****************************************************************************************************************
    private void checkAllBeacons(BeaconScanner.Beacon beacon) {
        try {
            if (isBeaconOWNERZONE(beacon)) {
            } else {
                setHayDatos(true);
                int rssi = beacon.filteredRssi;
                if (rssi >= rssiProximityE) {
                    if (rssi >= rssiProximityI) {
                        incTramasI();
                    } else {
                        incTramasE();
                        decTramasI();
                    }
                } else {
                    decTramas();
                }
            }
        } catch (Exception e) {
            Error("checkProximity:" + e.toString());
        }
    }

    //****************************************************************************************************************
    private void __checkAllBeacons(BeaconScanner.Beacon beacon) {
        try {
            if (isBeaconOWNERZONE(beacon)) {
            } else {
                setHayDatos(true);
                if (beacon.filteredRssi >= rssiProximityE) {
                    setHayProximidad(true);
                } else {
                }
            }
        } catch (Exception e) {
            Error("checkProximity:" + e.toString());
        }
    }

    //****************************************************************************************************************
    private void _checkAllBeacons(BeaconScanner.Beacon beacon) {
        try {
            if (isBeaconOWNERZONE(beacon)) {
            } else {
                setHayDatos(true);
                String addr = beacon.beaconInfo.getDevice().getAddress();
                if (beacon.filteredRssi >= rssiProximityE) {
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
        List<String> bad = new ArrayList();
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
    private void checkBeaconsOWNERZONE(BeaconScanner.Beacon beacon) {
        lockBeaconsOWNERZONE.lock();
        try {
            if (beaconOWNERZONE.size() > 0) {
                String addr = beacon.beaconInfo.getDevice().getAddress();
                if (beaconOWNERZONE.containsKey(addr)) {
                    if (beacon.filteredRssi >= rssiOwnerZone) {
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
    private void salvaFileData(String filePath, List<String> data) {

        try {

            if (data.size() <= 0) return;
            FileOutputStream outputStream = new FileOutputStream(new File(filePath));
            for (String b : data) {
                outputStream.write(b.getBytes());
            }
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            Error("salvaFileData:" + e.toString());
        }
    }


    //****************************************************************************************************************


}