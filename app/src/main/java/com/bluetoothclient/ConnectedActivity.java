package com.bluetoothclient;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;



public class ConnectedActivity extends ActionBarActivity implements SensorEventListener {

    private final String TAG = "ConnectedActivity";
    private List<UUID> candidateUUIDs= new ArrayList<UUID>();
    private final  BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothDevice mBluetoothDevice;
    private String mac_address = null;
    private ConnectToDevice connectToDevice;
    private StreamToServer streamToServer;
    private SensorManager sensorManager;
    private Sensor accelrometer;
    private int t=0;

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

        Button closeConnection = (Button) findViewById(R.id.closeConnection);
        closeConnection.setOnClickListener(onCloseConnectionClick);

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

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    View.OnClickListener onCloseConnectionClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            streamToServer.write("quit".getBytes());
            Intent intent = new Intent(ConnectedActivity.this, MainActivity.class);
            startActivity(intent);
        }
    };

    @Override
    public final void onSensorChanged(SensorEvent event) {
        t = t+1;
        byte[] b = (String.valueOf(t) + "," +String.valueOf(event.values[0]) + "," + String.valueOf(event.values[1]) + "," + String.valueOf(event.values[2])).getBytes();
        streamToServer.write(b);
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelrometer, SensorManager.SENSOR_DELAY_GAME);
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

    private class ConnectToDevice extends Thread{
        private  BluetoothConnector bluetoothConnector;
        private BluetoothDevice bluetoothDevice;
        private  BluetoothAdapter bluetoothAdapter;
        private List<UUID> candidateUUIDs;

        public ConnectToDevice(BluetoothDevice device, BluetoothAdapter adpater, List<UUID> UUIDs){
            bluetoothDevice = device;
            bluetoothAdapter = adpater;
            candidateUUIDs = UUIDs;
            bluetoothConnector = new BluetoothConnector(bluetoothDevice, bluetoothAdapter, candidateUUIDs);
        }

        public void run(){
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
        private BluetoothConnector bluetoothConnector;

        public StreamToServer(BluetoothConnector btConnector){
            try {
                bluetoothConnector = btConnector;
                outStream = bluetoothConnector.getOutputStream();
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
        mBluetoothAdapter.cancelDiscovery();
    }

}
