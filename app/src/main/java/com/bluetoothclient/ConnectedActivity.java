package com.bluetoothclient;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;



public class ConnectedActivity extends ActionBarActivity implements SensorEventListener {

    private final String TAG = "ConnectedActivity";
    private List<UUID> candidateUUIDs= new ArrayList<UUID>();
    private final  BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private  BluetoothConnector bluetoothConnector;
    private BluetoothDevice bluetoothDevice;
    private String mac_address = null;
    private StreamToServer streamToServer;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private int t=0;
    private boolean streamPaused = false;
    private float threshold;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connected);

        Bundle extras = getIntent().getExtras();
        if (extras != null){
            mac_address = extras.getString("MAC_ADDRESS");
        }

        if (mac_address != null){
            bluetoothDevice = bluetoothAdapter.getRemoteDevice(mac_address);
        }

        findViewById(R.id.closeConnection).setEnabled(false);

        ToggleButton toggleStream = (ToggleButton) findViewById(R.id.toggleStream);
        toggleStream.setOnCheckedChangeListener(onPauseStreamToggle);

        TextView deviceName = (TextView) findViewById(R.id.deviceName);
        deviceName.setText(bluetoothDevice.getName());

        Button closeConnection = (Button) findViewById(R.id.closeConnection);
        closeConnection.setOnClickListener(onCloseConnectionClick);

        SeekBar threshold = (SeekBar) findViewById(R.id.threshold);
        threshold.setOnSeekBarChangeListener(onThresholdChange);

        candidateUUIDs.add(UUID.fromString("a1a738e0-c3b3-11e3-9c1a-0800200c9a66"));
        candidateUUIDs.add(UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee"));

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        bluetoothConnector = new BluetoothConnector(bluetoothDevice, bluetoothAdapter, candidateUUIDs);
        new ConnectToDevice().execute(bluetoothConnector);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    SeekBar.OnSeekBarChangeListener onThresholdChange = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            threshold = (float) progress;
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    CompoundButton.OnCheckedChangeListener onPauseStreamToggle = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(isChecked){
                streamPaused = false;
            } else {
                streamPaused = true;
            }
        }
    };

    View.OnClickListener onCloseConnectionClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (streamToServer != null) {
                streamToServer.write("quit".getBytes());
                try {
                    bluetoothConnector.close();
                } catch (IOException e){
                    Log.d(TAG, "IOException occurred while closing connection");
                    e.printStackTrace();
                }
                Intent intent = new Intent(ConnectedActivity.this, MainActivity.class);
                startActivity(intent);
            }
        }
    };

    @Override
    public final void onSensorChanged(SensorEvent event) {
        t = t+1; //relative timestamps
        byte[] data = (String.valueOf(t) + "," +String.valueOf(event.values[0]) + "," + String.valueOf(event.values[1]) + "," + String.valueOf(event.values[2])).getBytes();
        float mod = 0;
        for(int i=0; i<3; i++){
            mod += event.values[i]*event.values[i];
        }
        if (streamToServer != null && !streamPaused && mod > threshold) {
            streamToServer.write(data);
        }
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
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

    private class ConnectToDevice extends AsyncTask<BluetoothConnector, Void, BluetoothConnector>{
        @Override
        protected BluetoothConnector doInBackground(BluetoothConnector... params){
            try {
                params[0].connect();
                Log.d(TAG, "Socket connected");
            } catch (IOException e) {
                e.printStackTrace();
                //TODO : send failure info to UI thread
            }
            return params[0];
        }

        @Override
        protected void onPreExecute(){
            Toast.makeText(ConnectedActivity.this, "Initiating device connection", Toast.LENGTH_LONG).show();
        }

        @Override
        protected void onPostExecute(BluetoothConnector bluetoothConnector) {
            Log.d(TAG, "Connected to device, getting input stream");
            Toast.makeText(ConnectedActivity.this, "Connected to device", Toast.LENGTH_LONG).show();
            findViewById(R.id.closeConnection).setEnabled(true);
            streamToServer = new StreamToServer(bluetoothConnector);
            streamToServer.start();
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
        bluetoothAdapter.cancelDiscovery();
    }

}

