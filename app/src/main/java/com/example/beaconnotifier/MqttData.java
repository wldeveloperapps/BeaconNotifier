package com.example.beaconnotifier;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.Calendar;


public class MqttData {
    static public String patron="{\"id\":\"5e9db856a07d9a000116928f\",\"endDevice\":{\"devEui\":\"000DB5380D863861\",\"devAddr\":\"021471A0\",\"cluster\":{\"id\":484}},\"fPort\":2,\"fCntDown\":1122,"+
        "\"fCntUp\":1141,\"adr\":true,\"confirmed\":true,\"encrypted\":false,\"payload\":\"8083d08ac5fff377670242\",\"encodingType\":\"HEXA\",\"recvTime\":1587394646374,"+
        "\"classB\":false,\"delayed\":false,\"ulFrequency\":868.3,\"modulation\":\"LORA\",\"dataRate\":\"SF12BW125\",\"codingRate\":\"4/5\",\"gwCnt\":1,\"gwInfo\":[{\"gwEui\":\"7076FF00560502A2\","+
        "\"rfRegion\":\"EU868\",\"rssi\":-119,\"snr\":-13,\"latitude\":40.341434,\"longitude\":-3.8205936,\"altitude\":728,\"channel\":6,\"radioId\":262,\"antenna\":0}]}";

    private JSONObject jsonObj;
    private JSONObject jsonData;

    //40.341598 <->5e906702
    //-3.820935 <-> 79b2c5ff
    //8180 79b2c5ff 5e906702 42 55
    MqttData() {
        try {
            jsonObj = new JSONObject(patron);
            jsonData=jsonObj.getJSONArray("gwInfo").getJSONObject(0);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private static String doubleToStringLittleEndian(double data) {
        byte[] result = new byte[8];
        long l=(long)((data)*1000000.0);
        for (int i = 0; i <8; i++) {
            result[i] = (byte)(l & 0xFF);
            l >>= 8;
        }
        return new BigInteger(1, result).toString(16).substring(0,8);
    }


    public String buildJsonTrack(String devEUI,String devAddr,int protocol, int command,double lat,double lon,int gpsStatus,int battery) {
        long epoch = Calendar.getInstance().getTimeInMillis();
        String payload = String.format("%02X%02X%s%s%02X%02X", protocol, command, doubleToStringLittleEndian(lon), doubleToStringLittleEndian(lat), gpsStatus, battery);
        try {
            jsonObj.put("recvTime", epoch);
            jsonObj.getJSONObject("endDevice").put("devEui", devEUI);
            jsonObj.getJSONObject("endDevice").put("devAddr", devAddr);
            jsonObj.put("payload", payload);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObj.toString();
    }



}
