package com.example.beaconnotifier;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;


public class MqttHelper  {
    protected static final String TAG = "MqttHelper";
    private MqttAndroidClient mqttAndroidClient;
    private String serverUri = "";
    private String clientId = "";
    private String subscriptionTopic = "";
    private String username = "";
    private String password = "";
    private Context context;
    MqttConnectOptions mqttConnectOptions;

    //***********************************************************************************************************
    public MqttHelper(Context context, String serverUri, String cliendId, String username, String password){
        this.clientId=cliendId;
        this.serverUri=serverUri;
        this.username=username;
        this.password=password;
        this.context=context;
        mqttConnectOptions=new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(true);
        mqttConnectOptions.setUserName(username);
        mqttConnectOptions.setPassword(password.toCharArray());

        mqttAndroidClient = new MqttAndroidClient(context, serverUri, clientId);
    }

    //***********************************************************************************************************
    public void setCallback(MqttCallbackExtended callback) {
        mqttAndroidClient.setCallback(callback);
    }

    //***********************************************************************************************************
    public void publish(String topic, final String message, int qos, boolean retain){

        try {
            mqttAndroidClient.publish(topic, message.getBytes(), qos, retain, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "publish:" +message+" ok");
                }
                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable e) {

                }
            });
        } catch( MqttException e){
            Log.e(TAG, "publish:" + e.toString());
        }
    }


    //***********************************************************************************************************
    public void disconnect(){
        try {
            mqttAndroidClient.disconnect();
            mqttAndroidClient.close();
        } catch (MqttException e){
            Log.e(TAG, "disconnect:" + e.toString());
        }
    }

    //***********************************************************************************************************
    public boolean isConnected(){
        try {
            return mqttAndroidClient.isConnected();
        } catch (Exception e){
            Log.e(TAG, "isConnected:" + e.toString());
        }
        return false;
    }

    //***********************************************************************************************************
    public void connect(){
        try {

            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {

                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable e) {
                    Log.e(TAG, "connect:" + e.toString());
                }
            });
        } catch (MqttException e){
            Log.e(TAG, "connect:" + e.toString());
        }

    }

    //***********************************************************************************************************
    public void subscribeToTopic(final String subscriptionTopic) {
        try {
            mqttAndroidClient.subscribe(subscriptionTopic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "subscribeToTopic " +subscriptionTopic+" ok");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable e) {
                    Log.e(TAG, "subscribeToTopic:" + e.toString());
                }
            });

        } catch (MqttException e) {
            Log.e(TAG, "subscribeToTopic:" + e.toString());
        }
    }
}
