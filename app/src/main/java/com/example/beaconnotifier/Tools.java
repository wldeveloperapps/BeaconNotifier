package com.example.beaconnotifier;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.telephony.SmsManager;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import static android.content.Context.BATTERY_SERVICE;

public class Tools {
    private static final String TAG = "Tools";

    private static Context ctx=null;
    private static android.view.Window win;
    public static final int VIBRACION_TIPO_OFF=0;
    public static final int VIBRACION_TIPO_1=1;
    public static final int VIBRACION_TIPO_TICK=2;
    public static final int VIBRACION_TIPO_CONTINUO=3;

    static public void setContext(Context applicationContext,android.view.Window w){
        ctx=applicationContext;
        win=w;
        VibradorHelper.init(ctx);
    }
    //*****************************************************************************************************************
    public static int getBatteryLevel(){
        int batLevel=0;
        try {
            BatteryManager bm = (BatteryManager) ctx.getSystemService(BATTERY_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            }
        } catch (Exception e) {
            throw e;
        }
        return batLevel;
    }

    //****************************************************************************************************************
    static public void setBrillo(int porcentaje) {
        float brightness = porcentaje / (float) 255;
        WindowManager.LayoutParams lp = win.getAttributes();
        lp.screenBrightness = brightness;
        win.setAttributes(lp);
    }

    //****************************************************************************************************************
    static public void vibra(int tipo) {
        try {
            switch(tipo){
                case VIBRACION_TIPO_1:
                    VibradorHelper.makePattern()
                            .beat(200)
                            .rest(100)
                            .beat(500).playPattern();
                    break;
                case VIBRACION_TIPO_TICK:
                    VibradorHelper.once(150);
                    break;
                case VIBRACION_TIPO_CONTINUO:
                    VibradorHelper.once(5000);
                    break;
                case VIBRACION_TIPO_OFF:
                    VibradorHelper.stop();
                    break;
            }
        } catch (Exception e) {
            throw e;
        }
    }

    //***********************************************************************************************************
    static public void Tick() {
        vibra(VIBRACION_TIPO_TICK);
    }



    //***********************************************************************************************************
    static public void soundRing(int tipo, int timeout) {

        try {
            Uri notification = RingtoneManager.getDefaultUri(tipo);
            final Ringtone r = RingtoneManager.getRingtone(ctx, notification);
            if(timeout<=0){
                r.stop();
            }else{
                r.play();
            }
            if(timeout>0) {
                new CountDownTimer(timeout, timeout) {
                    public void onTick(long millisUntilFinished) {
                    }
                    public void onFinish() {
                        r.stop();
                    }
                }.start();
            }
        } catch (Exception e) {
            throw e;
        }
    }

    //***********************************************************************************************************
    static public void alerta(String titulo, String mensaje) {
        try {
            final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
            builder.setTitle(titulo);
            builder.setMessage(mensaje);
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {

                }
            });
            builder.show();
        } catch (Exception e) {
            throw e;
        }
    }

    //****************************************************************************************************************
    static public void notificacion(String title, String info, boolean lights, boolean vibration) {
        try {
            NotificationManager notificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            Notification.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel("Beacon Reference Notifications",
                        "Beacon Reference Notifications", NotificationManager.IMPORTANCE_HIGH);
                channel.enableLights(lights);
                channel.enableVibration(vibration);
                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                notificationManager.createNotificationChannel(channel);
                builder = new Notification.Builder(ctx, channel.getId());
            } else {
                builder = new Notification.Builder(ctx);
                builder.setPriority(Notification.PRIORITY_HIGH);
            }
            builder.setSmallIcon(R.drawable.ic_launcher_background);
            builder.setContentTitle(title);
            builder.setContentText(info);
            notificationManager.notify(1, builder.build());
        } catch (Exception e) {
            throw e;
        }
    }

    //***********************************************************************************************************
    static public void debug(String str, int tipo) {
        Toast.makeText(ctx, str, tipo).show();
    }
    //**************************************************************************************************************
    static public boolean bluetoothAvailable() {
        return BluetoothAdapter.getDefaultAdapter() != null;
    }
    //**************************************************************************************************************
    static public boolean bluetoothEnabled() {
        return BluetoothAdapter.getDefaultAdapter().isEnabled();
    }

    //**************************************************************************************************************
    static public boolean gpsLocationEnabled() {
        LocationManager lm=(LocationManager)ctx.getSystemService(Context.LOCATION_SERVICE);
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
    //**************************************************************************************************************
    static public boolean netLocationEnabled() {
        LocationManager lm=(LocationManager)ctx.getSystemService(Context.LOCATION_SERVICE);
        return lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }
    //**************************************************************************************************************
    static public void enableBluetooth(boolean enable) {
        try {
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if(enable){
                if (!mBluetoothAdapter.isEnabled()) mBluetoothAdapter.enable();
            }else{
                if (mBluetoothAdapter.isEnabled()) {
                    mBluetoothAdapter.disable();

                }
            }
        } catch (Exception e) {
            throw e;
        }
    }

    //**************************************************************************************************************
    static public void setBluetoothOnOffCycle(final int msec){
        if(!BluetoothAdapter.getDefaultAdapter().isEnabled()){
            BluetoothAdapter.getDefaultAdapter().enable();
            return;
        }
        try {
            final BroadcastReceiver mReceiver = new BroadcastReceiver() {
               @Override
               public void onReceive(Context context, Intent intent) {
                   final String action = intent.getAction();
                   if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                       final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                               BluetoothAdapter.ERROR);
                       switch (state) {
                           case BluetoothAdapter.STATE_OFF:
                               new Handler().postDelayed(new Runnable() {
                                   @Override
                                   public void run() {
                                       BluetoothAdapter.getDefaultAdapter().enable();
                                   }}, msec);
                               break;
                           case BluetoothAdapter.STATE_TURNING_OFF:
                               break;
                           case BluetoothAdapter.STATE_ON:
                               break;
                           case BluetoothAdapter.STATE_TURNING_ON:
                               break;
                       }
                   }
               }
           };
            BluetoothAdapter.getDefaultAdapter().disable();
            ctx.registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        } catch (Exception e) {
            throw e;
        }
    }

    //*****************************************************************************************************************
    public static void llamar(String tfno) {
        try {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + tfno));
            ctx.startActivity(callIntent);

        } catch (Exception e) {
            throw e;
        }
    }

    public static void sms(String datos,String tfno) {
        try {

            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(tfno, null, datos, null, null);
        } catch (Exception e) {
            throw e;
        }
    }



   //new Handler().postDelayed(new Runnable() {
//        @Override
//        public void run() {
//            BluetoothAdapter.getDefaultAdapter().enable();
//        }}, 500);

}
