package com.example.beaconnotifier;

/*
• Initiator: anchor that will initiate the network. A network must contain at least one.
• Anchor: used as reference to calculate tags position with trilateration.
• Tag: mobile node to be tracked within the system.

UWB: ‘off’, ‘passive’ or ‘active’.
o Set to ‘active’ to range in the network.
o Set to ‘passive’ if used as a listener.

If in anchor mode:
o INITIATOR Configure this anchor as an initiator. At least one of the anchors must be an initiator in the network. The initiator will start and control the network
o POSITION Position: The x,y,z co-ordinate of the anchor in the grid. Will be automatically populated if this device participated in auto-positioning.
▪ X position
▪ Y position
▪ Z position

If in tag mode:
o STATIONARY DETECTION: Enables/disables motion sensor operation. If disabled, then the stationary update rate will not be available.
o RESPONSIVE MODE:
o LOCATION ENGINE:
 */

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class DWPans2Ble2 {
    //************************************************** TIPOS ***************************************************************************************
    public class typeLocationData {
        public String nodeId;
        public int distanceMM;
        public int quality;

        public typeLocationData(String _nodeId, int _distanceMM, int _quality) {
            nodeId = _nodeId;
            distanceMM = _distanceMM;
            quality = _quality;
        }
    }

    //************************************************** TIPOS ***************************************************************************************
    public static class typeDataResult {
        public BluetoothGattCharacteristic c;
        public byte[] data;
    }
    //************************************************** TIPOS ***************************************************************************************
    public static class typeUpdateRate {
        public int updateRateActive;
        public int updateRatePassive;

        public  typeUpdateRate(int dateActiveActive, int dateRatePassive) {
            updateRateActive=dateActiveActive;
            updateRatePassive=dateRatePassive;
        }
    }
    //************************************************** TIPOS ***************************************************************************************
    public static class typeDeviceInfo {
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

    //************************************************** TIPOS ***************************************************************************************
    class typeOperatioMode {
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


        public typeOperatioMode() {
            clear();
        }

        public void clear() {
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

    //************************************************** CONST ***************************************************************************************
    final String TAG = "DWPans2Ble2";
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
    public static final int LOCATION_DATA_MODE_POSITION = 0;
    public static final int LOCATION_DATA_MODE_DISTANCES = 1;
    public static final int LOCATION_DATA_MODE_POSITION_DISTANCES = 2;

    //************************************************** VAR ***************************************************************************************
    BlockingQueue<Object> blockingQueueRead = new ArrayBlockingQueue<Object>(1);
    BlockingQueue<Object> blockingQueueWrite = new ArrayBlockingQueue<Object>(1);
    BleGattHelper ble;
    BluetoothGatt localGatt = null;
    BleCallback blecb;
    byte[] response = null;
    boolean debug = false;
    int state = -1;//dummy
    BluetoothDevice gattDevice=null;
    typeUpdateRate updateRateConfig=new typeUpdateRate(0,0);

    //************************************************** CALLBACKS ***************************************************************************************
    private OnConnectListener mOnConnect = null;
    private OnServiceListener mOnService = null;
    private OnNotificationListener mOnNotification = null;
    private OnCharacteristicReadListener mOnCharacteristicRead = null;
    private OnCharacteristicWriteListener mOnCharacteristicWrite = null;
    private OnDistanceListener mOnDistance = null;
    //************************************************** CHARACTERISTICS ***************************************************************************************
    int networkId = 0;
    List<typeLocationData> distances = new ArrayList<>();
    int locationDataMode = 0;
    typeDeviceInfo deviceInfo = new typeDeviceInfo();
    typeOperatioMode operationMode = new typeOperatioMode();
    typeUpdateRate updateRate=new typeUpdateRate(0,0);


    //************************************************** GETTERS/SETTERS ***************************************************************************************
    //getters


    public typeUpdateRate getUpdateRate() {
        return updateRate;
    }

    public void setUpdateRate(typeUpdateRate updateRate) {
        this.updateRate = updateRate;
    }

    public typeOperatioMode getOperationMode() {
        return operationMode;
    }

    public void setOperationMode(typeOperatioMode operation) {
        operationMode = operation;
    }

    public typeDeviceInfo getDeviceInfo() {
        return deviceInfo;
    }

    public void setLocationDataMode(int location) {
        locationDataMode = location;
    }

    public int getLocationDataMode() {
        return locationDataMode;
    }

    public int getNetworkId() {
        return networkId;
    }

    public List<typeLocationData> getDistances() {
        return distances;
    }

    //************************************************** INTERFACES ***************************************************************************************
    public interface OnCharacteristicWriteListener {
        public void onCharacteristicWrite(boolean ok, Exception e);
    }

    public void setCharacteristicWriteListener(OnCharacteristicWriteListener listener) {
        mOnCharacteristicWrite = listener;
    }

    //******************************************************************************************************************************************
    public interface OnCharacteristicReadListener {
        public void onCharacteristicRead(boolean ok, Exception e);
    }

    public void setCharacteristicReadListener(OnCharacteristicReadListener listener) {
        mOnCharacteristicRead = listener;
    }

    //******************************************************************************************************************************************
    public interface OnNotificationListener {
        public void onNotification(String uuidCharacteristic,byte[] data);
    }

    public void setOnNotificationListener(OnNotificationListener listener) {
        mOnNotification = listener;
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
    public interface OnDistanceListener {
        public void onDistance(String id, double meters);
    }

    public void setOnDistanceListener(OnDistanceListener listener) {
        mOnDistance = listener;
    }

    //******************************************************************************************************************************************
    //val oficilaServiceUuid = UUID.fromString("0000xxxx-0000-1000-8000-00805f9b34fb")
    //para servicion oficiales los uuids se forman rellenando los 4 caracteres (xxxx) con la identificacion del servicio oficial
    //ej. el servicio de bateria tiene un UUID de 0x180F luego en Android el UUID del servicio seria
    //val betteryServiceUuid = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb")
    public DWPans2Ble2(android.app.Activity ctx, boolean withDebug,typeUpdateRate ur) {
        ble = new BleGattHelper(ctx);
        debug = withDebug;
        updateRateConfig=ur;
        blecb = new BleCallback() {
            @Override
            public void onBleConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onBleConnectionStateChange(gatt, status, newState);
                localGatt = gatt;
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        if (localGatt == null) localGatt = gatt;
                        debugPrint( String.format("Connected"));
                        if (mOnConnect != null) mOnConnect.onConnected(true);
                        decawaveStateMachine(true);
                        if (gatt.requestMtu(106)) {
                            debugPrint( String.format("MTU 106 bytes request OK"));
                        }
                        //countDownTimerReconnect.start();
                    }

                    if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        decawaveStateMachine(false);
                        if (mOnConnect != null) mOnConnect.onConnected(false);
                        debugPrint( String.format("Disconnected"));
                    }
                    return;
                }else{
                    if(status == BluetoothProfile.GATT_SERVER || status==133){
                        if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                            decawaveStateMachine(false);
                            if (mOnConnect != null) mOnConnect.onConnected(false);
                            debugPrint( String.format("Disconnected"));
                        }
                    }
                }

                debugPrint(String.format("Status:%d, State:%d", status, newState));
            }

            //******************************************************************************************************************************************
            @Override
            public void onBleServiceDiscovered(BluetoothGatt gatt, int status) {
                super.onBleServiceDiscovered(gatt, status);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    for (BluetoothGattService s : gatt.getServices()) {
                        debugPrint( String.format("SRV:%s", s.getUuid().toString()));
                        if (s.getUuid().toString().equals(SRV_NETWORK_NODE_SERVICE)) {
                            if (debug) for (BluetoothGattCharacteristic c : s.getCharacteristics())
                                printCharacteristicProperties(c);
                            if (mOnService != null) mOnService.onService(true, status);
                            decawaveStateMachine(true);
                            return;
                        }
                    }
                }
                if (mOnService != null) mOnService.onService(false, status);
                decawaveStateMachine(false);
            }

            //******************************************************************************************************************************************
            @Override
            public void onBleCharacteristicChange(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onBleCharacteristicChange(gatt, characteristic);
                updateValue(characteristic.getUuid().toString(), characteristic.getValue());
                if (mOnNotification != null) mOnNotification.onNotification(characteristic.getUuid().toString(), characteristic.getValue());
                decawaveStateMachine(true);
                debugPrint( String.format("onBleCharacteristicChange %s(%s)", characteristic.getUuid().toString(), Arrays.toString(characteristic.getValue())));
            }

            //******************************************************************************************************************************************
            @Override
            public void onBleRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onBleRead(gatt, characteristic, status);
                try {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        blockingQueueRead.put(characteristic.getValue());
                        debugPrint( String.format("onBleRead %s(0x%s)", characteristic.getUuid().toString(), Tools.bytesToHex(characteristic.getValue())));
                    }
                } catch (InterruptedException e) {
                    debugPrint( String.format("onBleRead Error%s()", characteristic.getUuid().toString(), e.getMessage()));
                }
            }

            //******************************************************************************************************************************************
            @Override
            public void onBleWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onBleWrite(gatt, characteristic, status);
                try {
                    if (status == BluetoothGatt.GATT_SUCCESS)
                        blockingQueueWrite.put(characteristic.getValue());
                        debugPrint( String.format("onBleWrite %s(0x%s)", characteristic.getUuid().toString(), Tools.bytesToHex(characteristic.getValue())));
                } catch (InterruptedException e) {
                        debugPrint( String.format("onBleWrite Error%s()", characteristic.getUuid().toString(), e.getMessage()));
                }
            }
        };
    }

    //******************************************************************************************************************************************
    public void ser(int txPhy,int rxPhy,int phyOptions){//Ej. PhyRequest.PHY_LE_2M_MASK, PhyRequest.PHY_LE_2M_MASK, PhyRequest.PHY_OPTION_NO_PREFERRED
        localGatt.setPreferredPhy(txPhy,rxPhy,phyOptions);
    }
    //******************************************************************************************************************************************
    public void printCharacteristicProperties(BluetoothGattCharacteristic c) {
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
        debugPrint( String.format("-->CHR:%s,Pro:%04X(%s)", uuid, flag, permission));
    }

    //******************************************************************************************************************************************
    public void startStateMachine(boolean enable){
        state=(enable)?0:-1;
    }

    //******************************************************************************************************************************************
    public void connect(BluetoothDevice bd,boolean withStateMachine) {
        gattDevice=bd;
        startStateMachine(withStateMachine);
        if (!ble.isConnected()){
            if(withStateMachine)
                decawaveStateMachine(true);
            else ble.connect(gattDevice, blecb);
        }
    }

    //******************************************************************************************************************************************
    public void disconnect() {
        ble.disconnect();
    }
    //******************************************************************************************************************************************
    public void dettach() {
        localGatt.close();
    }

    //******************************************************************************************************************************************
    public boolean isConnected() {
        return ble.isConnected();
    }

    //******************************************************************************************************************************************
    public boolean discoverServices() {
        return localGatt.discoverServices();
    }
    //******************************************************************************************************************************************
    public boolean setCharacteristicNotificationDistances(boolean enable){
        return setCharacteristicNotification(SRV_NETWORK_NODE_SERVICE, CHR_LOCATION_DATA,enable);
    }

    //******************************************************************************************************************************************
    public boolean setCharacteristicNotification(String service, String characteristic, boolean enable) {
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
                        if (blockingQueueWrite.poll(timeout, TimeUnit.MILLISECONDS) != null) {
                            updateValue(characteristic, data);
                            debugPrint( String.format("writeCharacteristic OK"));
                            if (mOnCharacteristicWrite != null)
                                mOnCharacteristicWrite.onCharacteristicWrite(true, null);
                            decawaveStateMachine(true);
                        } else {
                            if (mOnCharacteristicWrite != null)
                                mOnCharacteristicWrite.onCharacteristicWrite(false, null);
                            decawaveStateMachine(false);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        if (mOnCharacteristicWrite != null)
                            mOnCharacteristicWrite.onCharacteristicWrite(false, e);
                        decawaveStateMachine(false);
                    }
                }
            }).start();
        }
    }

    //******************************************************************************************************************************************
    private void updateValue(String c, byte[] data) {
        if (c.equals(CHR_LOCATION_DATA_MODE)) {
            readLocationDataMode(data);
        } else if (c.equals(CHR_ANCHOR_LIST)) {
        } else if (c.equals(CHR_CLUSTER_INFO)) {
        } else if (c.equals(CHR_DEVICE_INFO)) {
        } else if (c.equals(CHR_LOCATION_DATA)) {
            readLocationData(data);
        } else if (c.equals(CHR_MAC_STATS)) {
        } else if (c.equals(CHR_NETWORK_ID)) {
        } else if (c.equals(CHR_OPERATION_MODE)) {
            readOperationMode(data);
        } else if (c.equals(CHR_PERSISTED_POSITION)) {
        } else if (c.equals(CHR_PROXY_POSITIONS)) {
        } else if (c.equals(CHR_UPDATE_RATE_TAG)) {
            readUpdateRate(data);
        } else {
        }
    }

    //******************************************************************************************************************************************
    private boolean readUpdateRate(byte[] data) {
        try {
            updateRate.updateRateActive = Tools.byteArrayToLeInt(data, 0);
            updateRate.updateRatePassive = Tools.byteArrayToLeInt(data, 4);
        }catch(Exception w){
            return false;
        }
        return true;
    }

    //******************************************************************************************************************************************
    public void readDistances() {
        readValues(new ArrayList<>(Arrays.asList(new String[]{CHR_LOCATION_DATA})),0);
    }
    //******************************************************************************************************************************************
    public void readDistances(int delayMs) {
        readValues(new ArrayList<>(Arrays.asList(new String[]{CHR_LOCATION_DATA})),delayMs);
    }

    //******************************************************************************************************************************************
    public void writeLocationDataMode(int value) {
        writeCharacteristic(SRV_NETWORK_NODE_SERVICE, CHR_LOCATION_DATA_MODE, new byte[]{(byte)value}, TIMEOUT_MS);
    }

    //******************************************************************************************************************************************
    public void writeUpdateRate(typeUpdateRate uprate) {
        writeCharacteristic(SRV_NETWORK_NODE_SERVICE, CHR_UPDATE_RATE_TAG, Tools.concatByteArray(Tools.intToBytesLe(uprate.updateRateActive),Tools.intToBytesLe(uprate.updateRatePassive)), TIMEOUT_MS);
    }

    //******************************************************************************************************************************************
    /*
    public void writeOperationMode(typeOperatioMode operation) {
        byte[] values=new byte[2];
        values[0]=values[1]=0;
        if(operation.initiatorEnable)values[1]|=0x80;
        if(operation.lowPowerModeEnable)values[1]|=0x40;
        if(operation.locEngineEnable)values[1]|=0x20;
        if(operation.type==1)values[0]|=0x80;
        values[0]|=((byte)operation.uwb)<<5;
        if(operation.firmware)values[0]|=0x10;
        if(operation.accelEnable)values[0]|=0x08;
        if(operation.ledEnable)values[0]|=0x04;
        if(operation.firmUpdateEnable)values[0]|=0x02;
        writeCharacteristic(SRV_NETWORK_NODE_SERVICE, CHR_OPERATION_MODE, values, TIMEOUT_MS);
    }

     */
//******************************************************************************************************************************************
    private byte[] readCharacteristic(String service, String characteristic, int timeout) throws InterruptedException { //ojo en el mismo thread de peticion de caracteristicas
        byte[] res = null;
        if (ble.isConnected()) {
            try {
                ble.read(service, characteristic);
                res = (byte[]) blockingQueueRead.poll(timeout, TimeUnit.MILLISECONDS);
                if (res != null)
                    debugPrint( String.format("readCharacteristic:%s(0x%s)", characteristic, Tools.bytesToHex(res)));
            } catch (InterruptedException e) {
                throw e;
            }
        }
        return res;
    }

    //******************************************************************************************************************************************
    public void readAllCharacteristics() {
        if(ble.isConnected())readValues(new ArrayList<>(Arrays.asList(new String[]{CHR_LOCATION_DATA_MODE, CHR_DEVICE_INFO, CHR_NETWORK_ID, CHR_OPERATION_MODE,CHR_OPERATION_MODE,CHR_UPDATE_RATE_TAG})),0);
        else{
            if (mOnCharacteristicRead != null) mOnCharacteristicRead.onCharacteristicRead(false, null);
            decawaveStateMachine(false);
        }
    }

    //******************************************************************************************************************************************
    public void readValues(List<String> characteristics,int delayMs) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean result = true;
                try {
                    if(delayMs>0)Thread.sleep(delayMs);
                    for (String c : characteristics) {
                        if (c.equals(CHR_LOCATION_DATA_MODE)) {
                            result = readLocationDataMode(readCharacteristic(SRV_NETWORK_NODE_SERVICE, c, TIMEOUT_MS));
                        } else if (c.equals(CHR_ANCHOR_LIST)) {

                        } else if (c.equals(CHR_CLUSTER_INFO)) {

                        } else if (c.equals(CHR_DEVICE_INFO)) {
                            result = readDeviceInfo(readCharacteristic(SRV_NETWORK_NODE_SERVICE, c, TIMEOUT_MS));
                        } else if (c.equals(CHR_LOCATION_DATA)) {
                            result = readLocationData(readCharacteristic(SRV_NETWORK_NODE_SERVICE, c, TIMEOUT_MS));
                        } else if (c.equals(CHR_MAC_STATS)) {

                        } else if (c.equals(CHR_NETWORK_ID)) {
                            result = readNetworkId(readCharacteristic(SRV_NETWORK_NODE_SERVICE, c, TIMEOUT_MS));
                        } else if (c.equals(CHR_OPERATION_MODE)) {
                            result = readOperationMode(readCharacteristic(SRV_NETWORK_NODE_SERVICE, c, TIMEOUT_MS));
                        } else if (c.equals(CHR_PERSISTED_POSITION)) {

                        } else if (c.equals(CHR_PROXY_POSITIONS)) {

                        } else if (c.equals(CHR_UPDATE_RATE_TAG)) {
                            result = readUpdateRate(readCharacteristic(SRV_NETWORK_NODE_SERVICE, c, TIMEOUT_MS));
                        } else {
                            if (mOnCharacteristicRead != null)
                                mOnCharacteristicRead.onCharacteristicRead(false, null);
                        }
                        if (!result) {
                            if (mOnCharacteristicRead != null)
                                mOnCharacteristicRead.onCharacteristicRead(false, null);
                        }
                    }
                    if (mOnCharacteristicRead != null)
                        mOnCharacteristicRead.onCharacteristicRead(true, null);
                    decawaveStateMachine(true);
                } catch (Exception e) {
                    if (mOnCharacteristicRead != null)
                        mOnCharacteristicRead.onCharacteristicRead(false, e);
                    decawaveStateMachine(false);

                }
            }
        }).start();
    }

    //******************************************************************************************************************************************
    private boolean readOperationMode(byte[] result) {
        operationMode.clear();
        if (result != null) {
            if(result.length>0) {
                operationMode.accelEnable = (result[0] & 0x08) > 0;
                operationMode.firmUpdateEnable = (result[0] & 0x02) > 0;
                operationMode.firmware = (result[0] & 0x10) > 0;
                operationMode.initiatorEnable = (result[1] & 0x80) > 0;
                operationMode.ledEnable = (result[0] & 0x04) > 0;
                operationMode.lowPowerModeEnable = (result[1] & 0x40) > 0;
                operationMode.locEngineEnable = (result[1] & 0x20) > 0;
                operationMode.uwb = (result[0] >> 5) & 0x03;
                operationMode.type = result[0] & 0x80;
                debugPrint( String.format("OperationMode:1-0x%02X,2-0x%02X", result[0], result[1]));
                return true;
            }else{
                debugPrint( String.format("OperationMode not defined"));
                return true;
            }
        }
        return false;
    }

    //******************************************************************************************************************************************
    private boolean readDeviceInfo(byte[] result) {
        deviceInfo.clear();
        if (result != null) {
            if(result.length>0) {
                deviceInfo = new typeDeviceInfo(Tools.bytesToHexReverse(Arrays.copyOfRange(result, 0, 8)),
                        Tools.bytesToHexReverse(Arrays.copyOfRange(result, 8, 12)),
                        Tools.bytesToHexReverse(Arrays.copyOfRange(result, 12, 16)),
                        Tools.bytesToHexReverse(Arrays.copyOfRange(result, 16, 20)),
                        result[28]
                );
                debugPrint( String.format("Node:%s,HW:%s,FW1:%s,FW2:%s,OPF:%02X", deviceInfo.nodeId, deviceInfo.hwVersion, deviceInfo.fw1Version, deviceInfo.fw2Version, deviceInfo.operationFlag));
                return true;
            }else{
                debugPrint( String.format("Device Info not defined"));
                return true;
            }
        }
        return false;
    }

    //******************************************************************************************************************************************
    private boolean readLocationDataMode(byte[] result) {
        locationDataMode = 0;
        if (result != null) {
            if(result.length>0) {
                locationDataMode = result[0];
                debugPrint( String.format("LocationDataMode:%01X", locationDataMode));
                return true;
            }else{
                debugPrint( String.format("LocationDataMode not defined"));
                return true;
            }
        }
        return false;
    }


    //******************************************************************************************************************************************
    private boolean readLocationData(byte[] result) {
        distances.clear();
        if(result==null)return false;
        switch(locationDataMode){
            case LOCATION_DATA_MODE_POSITION:
            case LOCATION_DATA_MODE_POSITION_DISTANCES :
                result[0]=0;
                break;
        }
        if (result.length > 0) {
            int cantidad = (int) result[0];
            for (int i = 0; i < cantidad; i++) {
                distances.add(new typeLocationData(
                        Tools.bytesToHexReverse(Arrays.copyOfRange(result, (i * 8) + 2, (i * 8) + 2+2)),
                        Tools.byteArrayToLeInt(result, (i * 8) + 2 + 2),
                        result[(i * 8) + 2 + 2 + 4]));
            }
        }
        if (debug) {
            if (distances.size() > 0)
                for (typeLocationData l : distances)
                    debugPrint( String.format("Distance:%s->%.2fmts", l.nodeId, l.distanceMM/1000.0));
            else
                debugPrint( String.format("Sin Distancias..."));
        }
        return true;
    }

    //******************************************************************************************************************************************
    private boolean readNetworkId(byte[] result) {
        networkId = 0;
        if (result != null) {
            if(result.length>0) {
                try {
                    networkId = (int) Tools.byteArrayToLeShortUnsigned(result, 0);
                    debugPrint( String.format("NetworkId:0x%02X", networkId));
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }else{
                debugPrint( String.format("NetworkId not defined"));
                return true;
            }
        }else return false;
    }

    //**************************************************************E****************************************************************************
    private void debugPrint(String str){
        if(debug) Log.d(TAG, str);
    }

    //**************************************************************STATE MACHINE****************************************************************************
    private void decawaveStateMachine(boolean resultado) {
        switch (state) {
            case -1://dummy
                debugPrint( String.format("Dummy:%s",Boolean.toString(resultado)));
                break;
            case 0://waiting
                if (resultado) {
                    debugPrint( "DSM:Connecting...");
                } else {
                    debugPrint( "DSM:Disconnected...");
                    debugPrint( "DSM:Reconnecting...");
                }
                if(gattDevice!=null){
                    ble.connect(gattDevice, blecb);
                    debugPrint( "DSM:Try Connecting...");
                }
                state = 1;
                break;
            case 1://connected
                if (resultado) {
                    discoverServices();
                    state = 2;
                    debugPrint( "DSM:Discovering Services...");
                } else {
                    disconnect();
                    state = 0;
                    debugPrint( "DSM:Disconnecting...");
                }
                break;
            case 2://Services
                if (resultado) {
                    readAllCharacteristics();
                    state = 3;
                    debugPrint( "DSM:Reading Characteristics...");
                } else {
                    disconnect();
                    state = 0;
                    debugPrint( "DSM:Disconnecting...");
                }
                break;
            case 3://Characteristics
                if (resultado) {
                    debugPrint( String.format("DSM:LocationDataMode:%d,NetworkID:%d,NodeId:%s,HW:%s,FW1:%s,FW2:%s",
                            getLocationDataMode(), getNetworkId(), getDeviceInfo().nodeId,
                            getDeviceInfo().hwVersion, getDeviceInfo().fw1Version, getDeviceInfo().fw2Version));
                    if(getLocationDataMode()!=DWPans2Ble2.LOCATION_DATA_MODE_DISTANCES){
                        writeLocationDataMode(DWPans2Ble2.LOCATION_DATA_MODE_DISTANCES);
                        debugPrint( "DSM:Writing Location Data Mode ...");
                    }else{
                        if(updateRateConfig!=null){
                            writeUpdateRate(updateRateConfig);
                            state=4;
                            debugPrint( "DSM:Writing Update Data Rate...");
                        }else {
                            state = 5;
                            if (setCharacteristicNotificationDistances(true)) {
                                debugPrint("DSM: setCharacteristicNotificationDistances...OK");
                            } else {
                                debugPrint("DSM: setCharacteristicNotificationDistances...FAIL");

                            }
                        }
                    }
                } else {
                    disconnect();
                    state = 0;
                    debugPrint( "DSM:Disconnecting...");
                }
                break;
            case 4://SetNotifications
                if (resultado) {
                    state = 5;
                    if (setCharacteristicNotificationDistances(true)) {
                        debugPrint("DSM: setCharacteristicNotificationDistances...OK");
                    } else {
                        debugPrint("DSM: setCharacteristicNotificationDistances...FAIL");

                    }
                } else {
                    disconnect();
                    state = 0;
                    debugPrint( "DSM:Disconnecting...");
                }
                break;
            case 5://Read Distances
                if (resultado) {
                    for (DWPans2Ble2.typeLocationData d : distances) {
                        debugPrint( String.format("DSM:%s(%d,%d)", d.nodeId, d.distanceMM, d.quality));
                        if(mOnDistance!=null)mOnDistance.onDistance(d.nodeId,(double)d.distanceMM/1000.0);
                    }
                } else {
                    disconnect();
                    state = 0;
                    debugPrint( "DSM:Disconnecting...");
                }
                break;
            default:
                disconnect();
                state = 0;
                debugPrint( "DSM:Disconnecting...");

        }
    }

}
