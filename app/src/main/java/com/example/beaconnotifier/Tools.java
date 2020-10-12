package com.example.beaconnotifier;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.Toast;
import android.location.Location;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;

import static androidx.core.content.ContextCompat.getSystemService;

public class Tools {
    //****************************************************************************************************************
    static public void vibra(Context ctx, int mseg) {
        try {
            Vibrator vb = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
            if (vb != null && vb.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    VibrationEffect vibe = VibrationEffect.createOneShot(mseg, VibrationEffect.DEFAULT_AMPLITUDE);
                    vb.vibrate(vibe);
                } else {
                    vb.vibrate(mseg);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //***********************************************************************************************************
    static public void sound(Context ctx, int tipo, int timeout) {
        try {
            Uri notification = RingtoneManager.getDefaultUri(tipo);
            final Ringtone r = RingtoneManager.getRingtone(ctx, notification);
            if(timeout<0){
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
            throw new RuntimeException(e);
        }
    }

    //***********************************************************************************************************
    static public void alerta(Context ctx, String titulo, String mensaje) {
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
            throw new RuntimeException(e);
        }
    }

    //****************************************************************************************************************
    static public void notificacion(Context ctx, String title, String info, boolean lights, boolean vibration) {
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
            throw new RuntimeException(e);
        }
    }

    //***********************************************************************************************************
    static public void debug(Context ctx, String str, int tipo) {
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
    static public boolean gpsLocationEnabled(Context ctx) {
        LocationManager lm=(LocationManager)ctx.getSystemService(Context.LOCATION_SERVICE);
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
    //**************************************************************************************************************
    static public boolean netLocationEnabled(Context ctx) {
        LocationManager lm=(LocationManager)ctx.getSystemService(Context.LOCATION_SERVICE);
        return lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }
    //**************************************************************************************************************
    static public void enableBluetooth(boolean enable) {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(enable){
            if (!mBluetoothAdapter.isEnabled()) mBluetoothAdapter.enable();
        }else{
            if (mBluetoothAdapter.isEnabled()) mBluetoothAdapter.disable();
        }
    }
}
