package com.bluetoothclient;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.LogRecord;


public class ConnectedActivity extends ActionBarActivity implements SensorEventListener {

    private final String TAG = "ConnectedActivity";
    private List<UUID> candidateUUIDs= new ArrayList<UUID>();
    private final  BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothDevice mBluetoothDevice;
    private String mac_address = null;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread = null;
    private BluetoothConnector mBluetoothConnector;
    private ConnectToDevice connectToDevice;
    private StreamToServer streamToServer;
    private SensorManager sensorManager;
    private Sensor accelrometer;
    public static final int BOND_BONDED = 12;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connected);

        Bundle extras = getIntent().getExtras();
        if (extras != null){
            mac_address = extras.getString("MAC_ADDRESS");
        }

        if (mac_address != null){
            mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(mac_address);
        }

        TextView deviceName = (TextView) findViewById(R.id.deviceName);
        deviceName.setText(mBluetoothDevice.getName());

        EditText chatMessage = (EditText) findViewById(R.id.chatMessage);
        chatMessage.setOnEditorActionListener(onSendChatMessage);

        candidateUUIDs.add(UUID.fromString("a1a738e0-c3b3-11e3-9c1a-0800200c9a66"));
        candidateUUIDs.add(UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee"));

        connectToDevice = new ConnectToDevice(mBluetoothDevice, mBluetoothAdapter, candidateUUIDs);
        connectToDevice.start();

        try {
            connectToDevice.join();
        } catch (InterruptedException e){
            Log.d(TAG, "Connect to device interrupted");
            e.printStackTrace();
        }

        streamToServer = new StreamToServer(connectToDevice.getBluetoothConnector());
        streamToServer.start();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelrometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        /*mConnectThread = new ConnectThread(mBluetoothDevice);
        mConnectThread.start();*/
    }

    TextView.OnEditorActionListener onSendChatMessage = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if(actionId == EditorInfo.IME_ACTION_SEND){
                String c = v.getText().toString();
                v.setText("");
                mConnectedThread.write(c.getBytes());
            }
            return false;
        }
    };

    @Override
    public final void onSensorChanged(SensorEvent event) {
        byte[] b = String.valueOf(event.timestamp+','+event.values[0]+','+event.values[1]+','+event.values[2]).getBytes();
        streamToServer.write(b);
        //Log.d(TAG, "Got sensor event" + String.valueOf(event.timestamp)+ " : " + event.values[0]);
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelrometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.connected, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        public static final int BOND_BONDING = 11;
        public static final int BOND_BONDED = 12;

        private int bondState;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(UUID.fromString("11"));
            } catch (IOException e) {

            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            mBluetoothAdapter.cancelDiscovery();

            Log.d(TAG, "Entering socket connect loop");
            while(true){
                try {
                    mmSocket.connect();
                    break;
                } catch (IOException connectException){
                    Log.d(TAG, "Unable to connect, IOException, Retrying");
                }
            }
            Log.d(TAG, "Socket Connected");

            while(true) {
                bondState = mmDevice.getBondState();
                if (bondState == BOND_BONDING) {
                    Log.d(TAG, "Bonding in process");
                } else if (bondState == BOND_BONDED) {
                    Log.d(TAG, "Bond complete");
                    break;
                }
            }
            manageConnectedSocket(mmSocket);
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
    private void manageConnectedSocket(BluetoothSocket mSocket){
        mConnectedThread = new ConnectedThread(mSocket);

        while(true){
            if(mBluetoothDevice.getBondState() == BOND_BONDED){
                break;
            }
        }
        Log.d(TAG, "Paired. You may now kiss the bride.");
        mConnectedThread.start();
    }
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            //int bytes; // bytes returned from read()

            Log.d(TAG, "ConnectedThread @run activated");

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    //bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    //mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                    //Toast.makeText(MainActivity.this, new String(buffer, "UTF-8"), Toast.LENGTH_LONG).show();
                    String chatReply = new String(buffer, "UTF-8");
                    Log.i(TAG, "Received : " + chatReply);

                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    private class ConnectToDevice extends Thread{
        private  BluetoothConnector bluetoothConnector;
        private BluetoothDevice bluetoothDevice;
        private  BluetoothAdapter bluetoothAdapter;
        private List<UUID> candidateUUIDs;

        public ConnectToDevice(BluetoothDevice device, BluetoothAdapter adpater, List<UUID> UUIDs){
            bluetoothDevice = device;
            bluetoothAdapter = adpater;
            candidateUUIDs = UUIDs;
        }

        public void run(){
            bluetoothConnector = new BluetoothConnector(bluetoothDevice, bluetoothAdapter, candidateUUIDs);
            try {
                bluetoothConnector.connect();
                Log.d(TAG, "Socket connected");
            } catch (IOException e) {
                e.printStackTrace();
                //TODO : send failure info to UI thread
            }
        }

        public BluetoothConnector getBluetoothConnector(){
            return bluetoothConnector;
        }

        public void close(){
            try {
                bluetoothConnector.close();
            } catch (IOException e){
                Log.d(TAG, "Unable to close Socket");
                e.printStackTrace();
            }
        }
    }

    private class StreamToServer extends Thread{
        private OutputStream outStream;

        public StreamToServer(BluetoothConnector bluetoothConnector){
            try {
                this.outStream = bluetoothConnector.getOutputStream();
            } catch (IOException e){
                Log.d(TAG, "Unable to get output stream");
                e.printStackTrace();
            }
        }

        public void run() {
            //TODO : Timeout some ms and ensure if the connection is up
        }

        public void write(byte[] data){
            try {
                outStream.write(data);
            } catch (IOException e){
                Log.d(TAG, "Write Thread IOException");
                e.printStackTrace();
            }
        }
    }

    protected void OnDestroy(){
        connectToDevice.close();
    }

}
