package com.example.beaconnotifier;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.util.Log;

import java.util.UUID;

public class BleGattHelper {
    //******************************************************************************************************************************************
        private Activity act;
        private BleCallback bleCallback;
        private BluetoothGatt mBluetoothGatt;
    //******************************************************************************************************************************************
        private static final int STATE_DISCONNECTED = 0;
        private static final int STATE_CONNECTED    = 1;
        private int              mConnectionState   = STATE_DISCONNECTED;

    //******************************************************************************************************************************************
        public BleGattHelper(Activity _act){
                act = _act;
           }
    //******************************************************************************************************************************************
        public void connect(BluetoothDevice device, BleCallback _bleCallback){
            if (mBluetoothGatt == null && !isConnected()) {
                bleCallback = _bleCallback;
                mBluetoothGatt = device.connectGatt(act, false, mGattCallback);
            }
        }
    //******************************************************************************************************************************************
        public void disconnect(){
            if (mBluetoothGatt != null) {
                mBluetoothGatt.close();
                mBluetoothGatt = null;
            }
        }
    //******************************************************************************************************************************************
        public void write(String service, String characteristic, byte[] aBytes){
            BluetoothGattCharacteristic mBluetoothGattCharacteristic;
            mBluetoothGattCharacteristic = mBluetoothGatt.getService(UUID.fromString(service)).getCharacteristic(UUID.fromString(characteristic));
            mBluetoothGattCharacteristic.setValue(aBytes);
            mBluetoothGatt.writeCharacteristic(mBluetoothGattCharacteristic);
        }
    //******************************************************************************************************************************************
        public void write(String service, String characteristic, String aData){
            BluetoothGattCharacteristic mBluetoothGattCharacteristic;
            mBluetoothGattCharacteristic = mBluetoothGatt.getService(UUID.fromString(service)).getCharacteristic(UUID.fromString(characteristic));
            mBluetoothGattCharacteristic.setValue(aData);
            mBluetoothGatt.writeCharacteristic(mBluetoothGattCharacteristic);
        }
    //******************************************************************************************************************************************
        public void read(String service, String characteristic){
            mBluetoothGatt.readCharacteristic(mBluetoothGatt.getService(UUID.fromString(service)).getCharacteristic(UUID.fromString(characteristic)));
        }
    //******************************************************************************************************************************************
        private final BluetoothGattCallback mGattCallback;
        {
            mGattCallback = new BluetoothGattCallback() {
                @Override
                public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
                    super.onPhyUpdate(gatt, txPhy, rxPhy, status);
                }

                @Override
                public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
                    super.onPhyRead(gatt, txPhy, rxPhy, status);
                }

                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.i("BluetoothLEHelper", "Attempting to start service discovery: " + mBluetoothGatt.discoverServices());
                        mConnectionState = STATE_CONNECTED;
                    }else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        mConnectionState = STATE_DISCONNECTED;
                    }
                    bleCallback.onBleConnectionStateChange(gatt, status, newState);
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    bleCallback.onBleServiceDiscovered(gatt, status);
                }

                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicWrite(gatt, characteristic, status);
                    bleCallback.onBleWrite(gatt, characteristic, status);
                }

                @Override
                public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    bleCallback.onBleRead(gatt, characteristic, status);
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    bleCallback.onBleCharacteristicChange(gatt, characteristic);
                }

                @Override
                public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                    super.onDescriptorRead(gatt, descriptor, status);
                }

                @Override
                public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                    super.onDescriptorWrite(gatt, descriptor, status);
                }

                @Override
                public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
                    super.onReliableWriteCompleted(gatt, status);
                }

                @Override
                public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                    super.onReadRemoteRssi(gatt, rssi, status);
                }

                @Override
                public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                    super.onMtuChanged(gatt, mtu, status);
                }
            };
        }
    //******************************************************************************************************************************************
        public boolean isConnected(){
            return mConnectionState == STATE_CONNECTED;
        }
    //******************************************************************************************************************************************
        public BluetoothGattCallback getGatt(){
            return mGattCallback;
        }

    }

