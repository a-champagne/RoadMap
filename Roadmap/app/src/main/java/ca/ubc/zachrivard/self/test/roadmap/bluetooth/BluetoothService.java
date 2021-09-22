package ca.ubc.zachrivard.self.test.roadmap.bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BluetoothService extends Service {

    private static final boolean SHOW_BUFFER = false;
    private static final int MIN_AUTO_CONNECT_RSSI = -90;

    private static boolean serviceStarted = false;
    private static List<BluetoothObserver> sObservers = new ArrayList<>();

    private IBinder mBinder = new LocalBinder();

    private ConnectThread mConnectThread;
    private ManagerThread mManagerThread;

    private BluetoothSocket openSocket;

    public class LocalBinder extends Binder {
        public BluetoothService getService() {
            return BluetoothService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

    }

    public void beginService(){

        if(openSocket == null) {
            mConnectThread = new ConnectThread();
            mConnectThread.start();
        }
    }

    public interface BluetoothObserver {

        String KEY_EVENT_TYPE = "EVENT_TYPE";
        String KEY_REQUEST_TYPE = "REQUEST_TYPE";
        String KEY_DATA = "DATA";

        String KEY_ID = "ID";
        String KEY_TRIP_ID = "TripID";
        String KEY_MESSAGE = "Message";
        String KEY_MESSAGE_DATA = "Message_Data";
        String KEY_TIME_STAMP = "TimeStamp";
        String KEY_VEHICLE_ID = "VehicleID";
        String KEY_DRIVER_ID = "DriverID";
        String KEY_READ_STATUS = "ReadStatus";
        String KEY_LATITUDE = "KEY_LATITUDE";
        String KEY_LONGITUDE = "KEY_LONGITUDE";
        String KEY_AVAILABLE = "Available";
        String KEY_SPEED = "KEY_SPEED";
        String KEY_NS = "KEY_NS";
        String KEY_EW = "KEY_EW";
        String KEY_STATUS = "Status";
        String KEY_PROBLEM_TYPE = "ProblemType";
        String KEY_PROBLEM_DESCRIPTION = "ProblemDescription";
        String KEY_FROM_USER_TYPE = "fromUserType";


        String REQUEST_BT_CONNECTED = "BT_CONNECTED";
        String REQUEST_BT_EMERGENCY = "BT_EMERGENCY";
        String REQUEST_BT_SEND_MESSAGE = "BT_MESSAGE";
        String REQUEST_BT_GET_MESSAGES = "BT_GET_MESSAGES";
        String REQUEST_BT_MAINTENANCE = "BT_MAINTENANCE";
        String REQUEST_BT_CHOOSE_TRIP = "BT_CHOOSETRIP";
        String REQUEST_BT_LOCATIONDATA = "BT_LOCATION_DATA";
        String REQUEST_BT_UPDATE_ID = "BT_UPDATE_ID";
        String REQUEST_BT_STOP_TRIP = "BT_STOP_TRIP";


        String EVENT_LOCATION_UPDATE = "EVENT_LOCATION_UPDATE";
        String EVENT_EMERGENCY = "EVENT_EMERGENCY";
        String EVENT_ROUTE_DATA = "EVENT_ROUTE_DATA";
        String EVENT_ZONE_VIOLATION = "EVENT_ZONE_VIOLATION";
        String EVENT_MESSAGE = "EVENT_MESSAGE";
        String EVENT_SINGLE_MESSAGE = "EVENT_SINGLE_MESSAGE";
        String EVENT_MAINTENANCE = "EVENT_MAINTENANCE";
        String EVENT_CHANGE_VEHICLE = "EVENT_CHANGE_VEHICLE";

        String EVENT_CONNECTION_ESTABLISHED = "EVENT_CONNECTION_ESTABLISHED";
        String EVENT_DISCOVERY_BEGINNING = "EVENT_DISCOVERY_BEGINNING";
        String EVENT_CONNECTION_DISCONNECTED = "EVENT_CONNECTION_DISCONNECTED";
        String EVENT_BEGIN_TRIP = "EVENT_BEGIN_TRIP";



        /**
         * Callback interface by which observers receive bluetooth events.
         * @param event the event which occurred.
         * @param payload the event arguments
         */
        void onBluetoothEvent(String event, JSONObject payload);
    }

    public void issueCommand(String command) {
        // Figure out what to write to output stream
        mConnectThread.issueCommand(command);
    }

    public void closeConnection(){
        mManagerThread.closeSocket();
    }

    public static boolean registerObserver(BluetoothObserver observer) {
        return sObservers.add(observer);
    }

    public static boolean unregisterObserver(BluetoothObserver observer) {
        return sObservers.remove(observer);
    }

    private class ConnectThread extends Thread {

        private final String TAG = ConnectThread.class.getName();
        private final UUID MY_UUID = UUID.fromString ("00001101-0000-1000-8000-00805F9B34FB");

        private Set<BluetoothDevice> pairedDevices;

        private boolean foundNearbyDevice = false;

        private BluetoothAdapter mAdapter;

        private ConnectThread(){
            mAdapter = BluetoothAdapter.getDefaultAdapter();

            if(mAdapter == null){
                Toast.makeText(BluetoothService.this,
                        "Bluetooth not supported on this device!", Toast.LENGTH_SHORT).show();
            }
        }

        public void run(){
            findNearbyKnownDevice();
        }

        public void findNearbyKnownDevice(){

            if(mAdapter == null){
                return;
            }

            pairedDevices = mAdapter.getBondedDevices();

            for(BluetoothDevice d : pairedDevices){
                Log.e(TAG, "Paired Device: " + d.getName() + ", MAC: " + d.getAddress());
            }

            // Register for broadcasts
            registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            registerReceiver(receiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));

            mAdapter.startDiscovery();

            sObservers.forEach(observer -> observer.onBluetoothEvent(BluetoothObserver.EVENT_DISCOVERY_BEGINNING, null));
        }

        private void beginConnection(BluetoothDevice device){

            mAdapter.cancelDiscovery();
            BluetoothSocket tmpSocket = null;

            if(openSocket != null){
                return;
            }

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmpSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }

            Log.e(TAG, "Created a RFCOMM Socket!");
            openSocket = tmpSocket;

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                openSocket.connect();
            } catch (IOException connectException) {
                Log.e(TAG, connectException.getMessage());
                // Unable to connect; close the socket and return.
                try {
                    openSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            Log.e(TAG, "Socket Connected!");

            mManagerThread = new ManagerThread();
            mManagerThread.start();
        }

        // Create a BroadcastReceiver for ACTION_FOUND.
        private final BroadcastReceiver receiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if(action == null){
                    return;
                }


                switch (action){
                    case BluetoothDevice.ACTION_FOUND : {
                        // Discovery has found a device. Get the BluetoothDevice
                        // object and its info from the Intent.
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        String deviceName = device.getName();
                        String deviceHardwareAddress = device.getAddress(); // MAC address

                        int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);

                        if(rssi > MIN_AUTO_CONNECT_RSSI && pairedDevices.contains(device)){
                            foundNearbyDevice = true;
                            mAdapter.cancelDiscovery();
                            beginConnection(device);

                            Toast.makeText(getApplicationContext(), "Connecting to: " + device.getName(), Toast.LENGTH_SHORT).show();
                            return;
                        }


                            //Log.e(TAG, "Found Device! Name: " + deviceName + ", Address: " + deviceHardwareAddress + ", RSSI: " + rssi);

                    }

                    case BluetoothAdapter.ACTION_DISCOVERY_FINISHED : {

                        if(foundNearbyDevice){
                            //Log.e(TAG, "Found nearby known device! Stopping BT discovery");

                        } else {
                           // Log.e(TAG, "Device not known, or signal too weak. Starting discovery again.");
                            mAdapter.startDiscovery();
                        }
                    }
                }

            }
        };

        private void issueCommand(String command){

            while(mManagerThread == null){
                try {
                    sleep(100);
                } catch(InterruptedException e){
                    Log.e(TAG, "I got interrupted!");
                }
            }


            command += "\n";

            mManagerThread.write(command.getBytes());
        }
    }

    private class ManagerThread extends Thread {

        private final String TAG = ManagerThread.class.getName();
        private final InputStream mInStream;
        private final OutputStream mOutStream;

        public ManagerThread(){

            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = openSocket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = openSocket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }

            Log.e(TAG, "Created a Socket Manager!");
            mInStream = tmpIn;
            mOutStream = tmpOut;

            sObservers.forEach(observer -> observer.onBluetoothEvent(BluetoothObserver.EVENT_CONNECTION_ESTABLISHED, null));


            sendConnectionMessage();

        }

        public void run(){

            int numBytes;
            int btIndex = 0;
            byte btBuffer[] = new byte[1 << 16];

            String header = "###";
            String footer = "@@@";


            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    // Read from the InputStream.
                    numBytes = mInStream.read(btBuffer, btIndex, btBuffer.length - btIndex);
                    if (numBytes == 0) continue;

                    btIndex += numBytes;

                    String btData = new String(btBuffer).substring(0, btIndex);

                    if(SHOW_BUFFER) {
                        Log.e("BUFFER", btData);
                    }

                    if(btData.contains(header) && btData.contains(footer)){
                        //We have a full JSON object somewhere in here

                        int headerIndex = btData.indexOf(header);
                        int footerIndex = btData.indexOf(footer);

                        if(SHOW_BUFFER) {
                            Log.e("BUF_PRE", new String(btBuffer));
                        }

                        if(headerIndex + header.length() >= footerIndex){

                            //The header came after the footer. Clear until only the header remains
                            btBuffer = clearBufferUntilIndex(btBuffer, headerIndex);
                            btIndex -= headerIndex;

                            if(SHOW_BUFFER) {
                                Log.e("BUF_POST", new String(btBuffer));
                            }

                            continue;
                        }



                        String message = btData.substring(headerIndex, footerIndex + footer.length());
                        String fullJson = message.substring(header.length(), message.indexOf(footer));

                        try{
                            final JSONObject jsonObject = new JSONObject(fullJson);
                            final String eventType = jsonObject.getString(BluetoothObserver.KEY_REQUEST_TYPE);

                            if(eventType.length() == 0) continue;


                            if(SHOW_BUFFER) {
                                Log.e(TAG, jsonObject.toString(4));
                            }

                            sObservers.forEach(observer -> observer.onBluetoothEvent(eventType, jsonObject));

                        }catch (JSONException e){
                            Log.e(TAG, "Failed parsing the JSON from BT");
                        }

                        int removeLength = btData.indexOf(message) + message.length();
                        btBuffer = clearBufferUntilIndex(btBuffer, removeLength);
                        btIndex -= removeLength;

                        if(SHOW_BUFFER) {
                            Log.e("BUF_POST", new String(btBuffer));
                        }

                    }

                } catch (IOException e) {
                    Log.e(TAG, "Input stream was disconnected", e);
                    break;
                }

            }
        }

        private void sendConnectionMessage(){

            JSONObject json = new JSONObject();
            JSONObject data = new JSONObject();

            try {
                json.put(BluetoothService.BluetoothObserver.KEY_REQUEST_TYPE,
                        BluetoothService.BluetoothObserver.REQUEST_BT_CONNECTED);


                data.put(BluetoothService.BluetoothObserver.KEY_DRIVER_ID,
                        "TEST_DRIVER_ID");

                json.put(BluetoothService.BluetoothObserver.KEY_DATA, data);

                Log.e("JSON", json.toString(4));

                write(json.toString().getBytes());

            }catch (JSONException e){
                Log.e(TAG, "Error sending JSON connection message");
            }
        }

        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            try {
                mOutStream.write(bytes);
                mOutStream.flush();

            } catch (IOException e) {
                Log.e(TAG, "Error Sending data");
            }
        }

        private byte[] clearBufferUntilIndex(byte[] buffer, int startIndex){

            int index;
            for(index = 0; index < buffer.length; index++){

                if(index + startIndex < buffer.length) {
                    buffer[index] = buffer[index + startIndex];
                }else{
                    buffer[index] = 0;
                }
            }

            return buffer;
        }

        private void closeSocket(){

            if(openSocket == null){
                return;
            }

            try{
                openSocket.close();
                serviceStarted = false;
                openSocket = null;
            }catch (IOException e){}
        }
    }

}
