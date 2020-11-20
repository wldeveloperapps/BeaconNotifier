package com.example.beaconnotifier;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.util.Log;

import com.ederdoski.simpleble.interfaces.BleCallback;
import com.ederdoski.simpleble.utils.BluetoothLEHelper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class DWPans2Ble2 {
    public class typeLocationData {
        public int nodeId;
        public int distance;
        public int quality;

        public typeLocationData(int _nodeId, int _distance, int _quality) {
            nodeId = _nodeId;
            distance = _distance;
            quality = _quality;
        }
    }

    public class typeDataResult {
        public BluetoothGattCharacteristic c;
        public byte[] data;
    }

    public class typeDeviceInfo {
        public String nodeId;
        public String hwVersion;
        public String fw1Version;
        public String fw2Version;
        public byte operationFlag;

        public typeDeviceInfo(String _nodeId, String _hwVersion, String _fw1Version, String _fw2Version, byte _operationFlag) {
            nodeId = _nodeId;
            hwVersion = _hwVersion;
            fw1Version = _fw1Version;
            fw2Version = _fw2Version;
            operationFlag = _operationFlag;
        }

        public typeDeviceInfo() {
            clear();
        }

        public void clear() {
            nodeId = hwVersion = fw1Version = fw2Version = "";
            operationFlag = 0;
        }
    }

    class typeOperatioMode{
        int type;
        int uwb;
        boolean firmware;
        boolean accelEnable;
        boolean ledEnable;
        boolean firmUpdateEnable;
        boolean initiatorEnable;
        boolean lowPowerModeEnable;
        boolean locEngineEnable;

        public typeOperatioMode(byte type, short uwb, boolean firmware, boolean accelEnable, boolean ledEnable, boolean firmUpdateEnable, boolean initiatorEnable, boolean lowPowerModeEnable, boolean locEngineEnable) {
            this.type = type;
            this.uwb = uwb;
            this.firmware = firmware;
            this.accelEnable = accelEnable;
            this.ledEnable = ledEnable;
            this.firmUpdateEnable = firmUpdateEnable;
            this.initiatorEnable = initiatorEnable;
            this.lowPowerModeEnable = lowPowerModeEnable;
            this.locEngineEnable = locEngineEnable;
        }


        public typeOperatioMode(){
            clear();
        }

        public void clear(){
            this.type = 0;
            this.uwb = 0;
            this.firmware = false;
            this.accelEnable = false;
            this.ledEnable = false;
            this.firmUpdateEnable = false;
            this.initiatorEnable = false;
            this.lowPowerModeEnable = false;
            this.locEngineEnable = false;
        }
    }

    final String TAG = "DWPans2Ble2";
    boolean debug = false;
    public static final String SRV_NETWORK_NODE_SERVICE = "680c21d9-c946-4c1f-9c11-baa1c21329e7";
    public static final String CHR_OPERATION_MODE = "3f0afd88-7770-46b0-b5e7-9fc099598964";
    public static final String CHR_LOCATION_DATA_MODE = "a02b947e-df97-4516-996a-1882521e0ead";
    public static final String CHR_LOCATION_DATA = "003bbdf2-c634-4b3d-ab56-7ec889b89a37";
    public static final String CHR_PROXY_POSITIONS = "f4a67d7d-379d-4183-9c03-4b6ea5103291";
    public static final String CHR_DEVICE_INFO = "1e63b1eb-d4ed-444e-af54-c1e965192501";
    public static final String CHR_PERSISTED_POSITION = "f0f26c9b-2c8c-49ac-ab60-fe03def1b40c";
    public static final String CHR_MAC_STATS = "28d01d60-89de-4bfa-b6e9-651ba596232c";
    public static final String CHR_CLUSTER_INFO = "17b1613e-98f2-4436-bcde-23af17a10c72";
    public static final String CHR_ANCHOR_LIST = "5b10c428-af2f-486f-aee1-9dbd79b6bccb";
    public static final String CHR_UPDATE_RATE_TAG = "7bd47f30-5602-4389-b069-8305731308b6";
    public static final String CHR_NETWORK_ID = "80f9d8bc-3bff-45bb-a181-2d6a37991208";

    final UUID CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    final int TIMEOUT_MS = 5000;

    BlockingQueue<Object> blockingQueueRead = new ArrayBlockingQueue<Object>(1);
    BlockingQueue<Object> blockingQueueWrite = new ArrayBlockingQueue<Object>(1);
    BluetoothLEHelper ble;
    BluetoothGatt localGatt = null;
    BleCallback blecb;
    byte[] response = null;

    private OnConnectListener mOnConnect = null;
    private OnServiceListener mOnService = null;
    private OnDistanceListener mOnDistance = null;
    private OnCharacteristicReadListener mOnCharacteristicRead = null;

    int networkId = 0;
    List<typeLocationData> distancias = new ArrayList<>();
    int locationDataMode = 0;
    typeDeviceInfo deviceInfo=new typeDeviceInfo();
    typeOperatioMode operationMode=new typeOperatioMode();

    //getters
    public typeOperatioMode getOperationMode() {
        return operationMode;
    }
    public typeDeviceInfo getDeviceInfo() {
        return deviceInfo;
    }

    public int getLocationDataMode() {
        return locationDataMode;
    }

    public int getNetworkId() {
        return networkId;
    }

    public List<typeLocationData> getDistancias() {
        return distancias;
    }

    //******************************************************************************************************************************************
    public interface OnCharacteristicReadListener {
        public void onCharteristicRead();
    }

    public void setCharacteristicReadListener(OnCharacteristicReadListener listener) {
        mOnCharacteristicRead = listener;
    }

    //******************************************************************************************************************************************
    public interface OnDistanceListener {
        public void onDistance();
    }

    public void setOnDistanceListener(OnDistanceListener listener) {
        mOnDistance = listener;
    }

     //******************************************************************************************************************************************
    public interface OnConnectListener {
        public void onConnected(boolean connected);
    }

    public void setOnConnectListener(OnConnectListener listener) {
        mOnConnect = listener;
    }

    //******************************************************************************************************************************************
    public interface OnServiceListener {
        public void onService(boolean ok, int status);
    }

    public void setOnServiceListener(OnServiceListener listener) {
        mOnService = listener;
    }

    //******************************************************************************************************************************************
    //val oficilaServiceUuid = UUID.fromString("0000xxxx-0000-1000-8000-00805f9b34fb")
    //para servicion oficiales los uuids se forman rellenando los 4 caracteres (xxxx) con la identificacion del servicio oficial
    //ej. el servicio de bateria tiene un UUID de 0x180F luego en Android el UUID del servicio seria
    //val betteryServiceUuid = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb")
    public DWPans2Ble2(android.app.Activity ctx, boolean withDebug) {
        ble = new BluetoothLEHelper(ctx);
        debug = withDebug;
        blecb = new BleCallback() {
            @Override
            public void onBleConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onBleConnectionStateChange(gatt, status, newState);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        if (localGatt == null) localGatt = gatt;
                        if (debug) Log.d(TAG, String.format("Connected"));
                        if (mOnConnect != null) mOnConnect.onConnected(true);
                        if (gatt.requestMtu(106)) {
                            if (debug) Log.d(TAG, String.format("MTU 106 bytes request OK"));
                        }
                        //gatt.setPreferredPhy(PhyRequest.PHY_LE_2M_MASK, PhyRequest.PHY_LE_2M_MASK, PhyRequest.PHY_OPTION_NO_PREFERRED);
                        gatt.discoverServices();
                    }

                    if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        if (mOnConnect != null) mOnConnect.onConnected(false);
                        if (debug) Log.d(TAG, String.format("Disconnected"));
                    }
                    return;
                }
                if (debug) Log.e(TAG, String.format("Status:%d, State:%d", status, newState));
            }

            @Override
            public void onBleServiceDiscovered(BluetoothGatt gatt, int status) {
                super.onBleServiceDiscovered(gatt, status);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    for (BluetoothGattService s : gatt.getServices()) {
                        if (debug) Log.d(TAG, String.format("SRV:%s", s.getUuid().toString()));
                        if (s.getUuid().toString().equals(SRV_NETWORK_NODE_SERVICE)) {
                            /*for (BluetoothGattCharacteristic c : s.getCharacteristics()) {

                                if (debug) {
                                    String uuid = c.getUuid().toString();
                                    int flag = c.getProperties();
                                    String permission = "";
                                    if ((flag & BluetoothGattCharacteristic.PROPERTY_BROADCAST) > 0) {
                                        permission = permission.concat(",Broadcast");
                                    }
                                    if ((flag & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                                        permission = permission.concat(",Read");
                                    }
                                    if ((flag & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0) {
                                        permission = permission.concat(",WriteNR");
                                    }
                                    if ((flag & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                                        permission = permission.concat(",Write");
                                    }
                                    if ((flag & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                                        permission = permission.concat(",Notify");
                                    }
                                    if ((flag & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
                                        permission = permission.concat(",Indicate");
                                    }
                                    if ((flag & BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE) > 0) {
                                        permission = permission.concat(",SignedWrite");
                                    }
                                    if ((flag & BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS) > 0) {
                                        permission = permission.concat(",ExtendedP");
                                    }
                                    Log.d(TAG, String.format("-->CHR:%s,Pro:%04X(%s)", uuid, flag, permission));
                                }

                            }

                             */
                            if (mOnService != null) mOnService.onService(true, status);
                            readValues(new ArrayList<>(Arrays.asList(new String[]{CHR_LOCATION_DATA_MODE, CHR_DEVICE_INFO,CHR_NETWORK_ID,CHR_OPERATION_MODE})));
                            return;
                        }
                    }
                }
                if (mOnService != null) mOnService.onService(false, status);
            }

            @Override
            public void onBleCharacteristicChange(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onBleCharacteristicChange(gatt, characteristic);
                if (debug)
                    Log.d(TAG, String.format("onBleCharacteristicChange %s(%s)", characteristic.getUuid().toString(), Arrays.toString(characteristic.getValue())));
            }

            @Override
            public void onBleRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onBleRead(gatt, characteristic, status);
                try {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        blockingQueueRead.put(characteristic.getValue());
                        if (debug)
                            Log.d(TAG, String.format("onBleRead %s(0x%s)", characteristic.getUuid().toString(), Tools.bytesToHex(characteristic.getValue())));
                    }
                } catch (InterruptedException e) {
                    if (debug)
                        Log.d(TAG, String.format("onBleRead Error%s()", characteristic.getUuid().toString(), e.getMessage()));
                }
            }

            @Override
            public void onBleWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onBleWrite(gatt, characteristic, status);
                try {
                    if (status == BluetoothGatt.GATT_SUCCESS)
                        blockingQueueWrite.put(characteristic.getValue());
                } catch (InterruptedException e) {
                    if (debug)
                        Log.d(TAG, String.format("onBleWrite Error%s()", characteristic.getUuid().toString(), e.getMessage()));
                }
            }
        };
    }

    //******************************************************************************************************************************************
    public void connect(BluetoothDevice bd) {
        ble.connect(bd, blecb);
    }
    //******************************************************************************************************************************************
    public void disconnect() {
        ble.disconnect();
    }
    //******************************************************************************************************************************************
    public boolean isConnected() {
        return ble.isConnected();
    }
    //******************************************************************************************************************************************
    public boolean isScanning() {
        return ble.isScanning();
    }
    //******************************************************************************************************************************************
    public boolean setCharacteristicNofification(String service, String characteristic, boolean enable) {
        BluetoothGattDescriptor descriptor;
        try {
            if (ble.isConnected()) {
                BluetoothGattCharacteristic c = localGatt.getService(UUID.fromString((service))).getCharacteristic(UUID.fromString(characteristic));
                if (localGatt.setCharacteristicNotification(c, enable)) {
                    descriptor = c.getDescriptor(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID);
                    if (descriptor != null) {
                        if (descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE))
                            return localGatt.writeDescriptor(descriptor);
                    }
                }
            }
        } catch (Exception e) {
            throw e;
        }
        return false;
    }
    //******************************************************************************************************************************************
    private void writeCharacteristic(String service, String characteristic, byte[] data, int timeout) {
        if (ble.isConnected()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ble.write(service, characteristic, data);
                        if (blockingQueueWrite.poll(timeout, TimeUnit.MILLISECONDS) != null)
                            if (debug)
                                Log.d(TAG, String.format("writeCharacteristic OK"));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    //******************************************************************************************************************************************
    public void readDistances() {
        readValues(new ArrayList<>(Arrays.asList(new String[]{CHR_LOCATION_DATA})));
    }

    //******************************************************************************************************************************************
    private void writeLocationDataMode(byte value) {
        writeCharacteristic(SRV_NETWORK_NODE_SERVICE, CHR_LOCATION_DATA_MODE, new byte[]{value}, TIMEOUT_MS);
    }
//******************************************************************************************************************************************
    private byte[] readCharacteristic(String service, String characteristic, int timeout) { //ojo en el mismo thread de peticion de caracteristicas
        byte[] res = null;
        if (ble.isConnected()) {
            try {
                ble.read(service, characteristic);
                res = (byte[]) blockingQueueRead.poll(timeout, TimeUnit.MILLISECONDS);
                if (res != null)
                    if (debug)
                        Log.d(TAG, String.format("readCharacteristic:%s(0x%s)", characteristic, Tools.bytesToHex(res)));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return res;
    }
    //******************************************************************************************************************************************
    public void readValues(List<String> characteristics) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (String c : characteristics) {
                    if (c.equals(CHR_LOCATION_DATA_MODE)) {
                        readLocationDataMode(readCharacteristic(SRV_NETWORK_NODE_SERVICE, c, TIMEOUT_MS));
                    } else if (c.equals(CHR_ANCHOR_LIST)) {

                    } else if (c.equals(CHR_CLUSTER_INFO)) {

                    } else if (c.equals(CHR_DEVICE_INFO)) {
                        readDeviceInfo(readCharacteristic(SRV_NETWORK_NODE_SERVICE, c, TIMEOUT_MS));
                    } else if (c.equals(CHR_LOCATION_DATA)) {
                        readLocationData(readCharacteristic(SRV_NETWORK_NODE_SERVICE, c, TIMEOUT_MS));
                    } else if (c.equals(CHR_MAC_STATS)) {

                    } else if (c.equals(CHR_NETWORK_ID)) {
                        readNetworkId(readCharacteristic(SRV_NETWORK_NODE_SERVICE, c, TIMEOUT_MS));
                    } else if (c.equals(CHR_OPERATION_MODE)) {
                        readOperationMode(readCharacteristic(SRV_NETWORK_NODE_SERVICE, c, TIMEOUT_MS));
                    } else if (c.equals(CHR_PERSISTED_POSITION)) {

                    } else if (c.equals(CHR_PROXY_POSITIONS)) {

                    } else if (c.equals(CHR_UPDATE_RATE_TAG)) {

                    } else {
                    }
                }
                if (mOnCharacteristicRead != null) mOnCharacteristicRead.onCharteristicRead();
            }
        }).start();
    }
    //******************************************************************************************************************************************
    private void readOperationMode(byte[] result) {
        operationMode.clear();
        if (result != null) {
            operationMode.accelEnable=(result[0] & 0x08)>0;
            operationMode.firmUpdateEnable=(result[0] & 0x02)>0;
            operationMode.firmware  =(result[0] & 0x10)>0;
            operationMode.initiatorEnable  =(result[1] & 0x80)>0;
            operationMode.ledEnable  =(result[0] & 0x04)>0;
            operationMode.lowPowerModeEnable  =(result[1] & 0x40)>0;
            operationMode.locEngineEnable  =(result[1] & 0x20)>0;
            operationMode.uwb  =(result[0]>>5) & 0x03;
            operationMode.type  =result[0]&0x80;
            if (debug) Log.d(TAG, String.format("OperationMode:1-0x%02X,2-0x%02X", result[0],result[1]));
        }
    }
    //******************************************************************************************************************************************
    private void readDeviceInfo(byte[] result) {
        deviceInfo.clear();
        if (result != null) {
            deviceInfo = new typeDeviceInfo("DE"+Tools.bytesToHexReverse(Arrays.copyOfRange(result, 0, 7)),
                    Tools.bytesToHex(Arrays.copyOfRange(result, 8, 11)),
                    Tools.bytesToHex(Arrays.copyOfRange(result, 12, 15)),
                    Tools.bytesToHex(Arrays.copyOfRange(result, 16, 19)),
                    result[28]
            );
            if (debug) Log.d(TAG, String.format("Node:%s,HW:%s,FW1:%s,FW2:%s,OPF:%02X", deviceInfo.nodeId,deviceInfo.hwVersion,deviceInfo.fw1Version,deviceInfo.fw2Version,deviceInfo.operationFlag));
        }
    }
    //******************************************************************************************************************************************
    private void readLocationDataMode(byte[] result) {
        locationDataMode = -1;
        if (result != null) {
            locationDataMode = result[0];
        }
        if (locationDataMode != 1)
            writeLocationDataMode((byte) 1);
        if (debug) Log.d(TAG, String.format("LocationDataMode:%01X", locationDataMode));
    }
    //******************************************************************************************************************************************
    private void readLocationData(byte[] result) {
        distancias.clear();
        if (result != null && result.length>0) {
            int cantidad = (int)result[0];
            for (int i = 0; i < cantidad; i++) {
                distancias.add(new typeLocationData(
                        byteArrayToLeShort(result, (i * 7) + 1),
                        byteArrayToLeInt(result, (i * 7) + 1 + 2),
                        result[(i * 7) + 1 + 2 + 4]));
            }
        }
        if (debug) {
            if (distancias.size() > 0)
                for (typeLocationData l : distancias)
                    Log.d(TAG, String.format("Distance:%d->%d mts", l.nodeId, l.distance));
            else
                Log.d(TAG, String.format("Sin Distancias..."));
        }
    }
    //******************************************************************************************************************************************
    private void readNetworkId(byte[] result) {
        networkId = -1;
        if (result != null) {
            try {
                networkId = (int) byteArrayToLeShortUnsigned(result, 0);
            } catch (Exception e) {

            }
        }
        if (debug) Log.d(TAG, String.format("NetworkId:0x%02X", networkId));
    }

    //*********************************************************************************T O O L S*************************************************************/
    public static int byteArrayToLeShort(byte[] b, int index) {
        final ByteBuffer bb = ByteBuffer.wrap(b);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getShort(index);
    }

    public static int byteArrayToBeShort(byte[] b, int index) {
        final ByteBuffer bb = ByteBuffer.wrap(b);
        bb.order(ByteOrder.BIG_ENDIAN);
        return bb.getShort(index);
    }

    public static int byteArrayToLeShortUnsigned(byte[] b, int index) {
        final ByteBuffer bb = ByteBuffer.wrap(b);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getShort(index) & 0xFFFF;
    }

    public static int byteArrayToBeShortUnsigned(byte[] b, int index) {
        final ByteBuffer bb = ByteBuffer.wrap(b);
        bb.order(ByteOrder.BIG_ENDIAN);
        return bb.getShort(index) & 0xFFFF;
    }

    public static int byteArrayToLeInt(byte[] b, int index) {
        final ByteBuffer bb = ByteBuffer.wrap(b);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getInt(index);
    }

    public static int byteArrayToBeInt(byte[] b, int index) {
        final ByteBuffer bb = ByteBuffer.wrap(b);
        bb.order(ByteOrder.BIG_ENDIAN);
        return bb.getInt(index);
    }

    public static int byteArrayToLeIntUnsigned(byte[] b, int index) {
        final ByteBuffer bb = ByteBuffer.wrap(b);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getInt(index) & 0xFFFFFFFF;
    }

    public static int byteArrayToBeIntUnsigned(byte[] b, int index) {
        final ByteBuffer bb = ByteBuffer.wrap(b);
        bb.order(ByteOrder.BIG_ENDIAN);
        return bb.getInt(index) & 0xFFFFFFF;
    }

}
