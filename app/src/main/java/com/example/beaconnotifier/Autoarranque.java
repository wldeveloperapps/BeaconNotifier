package com.example.beaconnotifier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class Autoarranque extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent myIntent = new Intent(context,MainActivity.class);
        myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(myIntent);
    }
}
