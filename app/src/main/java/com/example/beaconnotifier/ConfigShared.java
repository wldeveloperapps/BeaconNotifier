package com.example.beaconnotifier;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class ConfigShared {
    final static  String FILE_CONFIG = "Settings";

    public static String getValue(String clave,String def,Context c){
        SharedPreferences preferences=c.getSharedPreferences(FILE_CONFIG, Context.MODE_PRIVATE);
        return preferences.getString(clave,def);
    }

    public static void setValue(String clave,String valor,Context c) {
        SharedPreferences preferences = c.getSharedPreferences(FILE_CONFIG, Context.MODE_PRIVATE);
        Editor editor = preferences.edit();
        editor.putString(clave, valor);
        editor.commit();
    }
}
