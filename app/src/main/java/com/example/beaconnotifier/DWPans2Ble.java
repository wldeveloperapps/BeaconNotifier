package com.example.beaconnotifier;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.UUID;

import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.PhyRequest;

public class DWPans2Ble extends BleManager {
    final String TAG="DWPans2Ble";
    final static UUID SERVICE_UUID = UUID.fromString("680c21d9-c946-4c1f-9c11-baa1c21329e7");
    final static UUID CHR_OPERATION_MODE   = UUID.fromString("3f0afd88-7770-46b0-b5e7-9fc099598964");
    final static UUID CHR_LOCATION_DATA   = UUID.fromString("003bbdf2-c634-4b3d-ab56-7ec889b89a37");
    private BluetoothGattCharacteristic chrOperationMode;
    private BluetoothGattCharacteristic chrLocationData;
//final static UUID SECOND_CHAR  = UUID.fromString("");
//private BluetoothGattCharacteristic SecondChar;

    DWPans2Ble(@NonNull final Context context) {
        super(context);
    }
    @NonNull
    @Override
    protected BleManagerGattCallback getGattCallback() {
        return new DWPans2BleManagerGattCallback();
    }
    @Override
    public void log(final int priority, @NonNull final String message) {
        Log.e(TAG,message);
    }

    /**
     * BluetoothGatt callbacks object.
     */
    private class DWPans2BleManagerGattCallback extends BleManagerGattCallback {

        // This method will be called when the device is connected and services are discovered.
        // You need to obtain references to the characteristics and descriptors that you will use.
        // Return true if all required services are found, false otherwise.
        @Override
        public boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {
            final BluetoothGattService service = gatt.getService(SERVICE_UUID);
            if (service != null) {
                chrOperationMode = service.getCharacteristic(CHR_OPERATION_MODE);
                chrLocationData = service.getCharacteristic(CHR_LOCATION_DATA);
            }
            return (service!=null)&&(chrOperationMode!=null)&&(chrLocationData!=null);
            /*
            // Validate properties
            boolean readRequest = false;
            boolean writeRequest = false;
            if (OperationMode != null) {
                final int properties = OperationMode.getProperties();
                readRequest = (properties & BluetoothGattCharacteristic.PROPERTY_READ) != 0;
                writeRequest = (properties & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0;
                OperationMode.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            }
            // Return true if all required services have been found
            return OperationMode != null  && readRequest && writeRequest;
            */

        }

        // If you have any optional services, allocate them here. Return true only if
        // they are found.
        @Override
        protected boolean isOptionalServiceSupported(@NonNull final BluetoothGatt gatt) {
            return super.isOptionalServiceSupported(gatt);
        }

        // Initialize your device here. Often you need to enable notifications and set required
        // MTU or write some initial data. Do it here.
        @Override
        protected void initialize() {
            // You may enqueue multiple operations. A queue ensures that all operations are
            // performed one after another, but it is not required.
            beginAtomicRequestQueue()
                            .add(requestMtu(106) // Remember, GATT needs 3 bytes extra. This will allow packet size of 244 bytes.
                            .with((device, mtu) -> log(Log.INFO, "MTU set to " + mtu))
                            .fail((device, status) -> log(Log.WARN, "Requested MTU not supported: " + status)))
                            .add(setPreferredPhy(PhyRequest.PHY_LE_2M_MASK, PhyRequest.PHY_LE_2M_MASK, PhyRequest.PHY_OPTION_NO_PREFERRED)
                            .fail((device, status) -> log(Log.WARN, "Requested PHY not supported: " + status)))
                            .add(enableNotifications(chrLocationData))
                            .done(device -> log(Log.INFO, "Target initialized"))
                            .enqueue();
            /*
            // You may easily enqueue more operations here like such:
            writeCharacteristic(secondCharacteristic, "Hello World!".getBytes())
                    .done(device -> log(Log.INFO, "Greetings sent"))
                    .enqueue();
            // Set a callback for your notifications. You may also use waitForNotification(...).
            // Both callbacks will be called when notification is received.
            setNotificationCallback(firstCharacteristic, callback);
            // If you need to send very long data using Write Without Response, use split()
            // or define your own splitter in split(DataSplitter splitter, WriteProgressCallback cb).
            writeCharacteristic(secondCharacteristic, "Very, very long data that will no fit into MTU")
                    .split()
                    .enqueue();

            */
        }

        @Override
        protected void onDeviceDisconnected() {
            // Device disconnected. Release your references here.
            chrOperationMode= chrLocationData= null;
        }
    }
}
