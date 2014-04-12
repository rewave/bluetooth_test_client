package com.bluetoothclient;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Toast;
import android.widget.ToggleButton;


public class MainActivity extends ActionBarActivity {

    private BluetoothAdapter mBluetoothAdapter;
    private final static int REQUEST_ENABLE_BT = 1;
    private final static int ON = 1;
    private final static int OFF = 0;
    ArrayAdapter<String> devices;

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView

                Log.i("Found", device.getName() + " found");
                devices.add(device.getName() + "\n" + device.getAddress());
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final ToggleButton toggleBluetooth = (ToggleButton) findViewById(R.id.toggleBluetooth);

        toggleBluetooth.setOnClickListener(toggleBluetoothListner);

    }

    View.OnClickListener toggleBluetoothListner = new View.OnClickListener() {
        final ToggleButton toggleBluetooth = (ToggleButton) findViewById(R.id.toggleBluetooth);

        @Override
            public void onClick(View v) {
                if (toggleBluetooth.isChecked()){
                    switchBluetooth(OFF);
                } else {
                    switchBluetooth(ON);
                }
                toggleBluetooth.toggle();
            }
    };

    private void switchBluetooth(int action){
        if (action == ON){
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null) {
                // Device does not support Bluetooth
                Toast.makeText(this, "No bluetooth available", Toast.LENGTH_LONG).show();
                Log.i("No Support", "Device doesn't support bluetooth");
                finish();
                System.exit(0);
            }

            if (!mBluetoothAdapter.isEnabled()){
                Log.i("Bt not enabled", "Bluetooth is not enabled");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT); //calls onActivityResult
            } else {
                // Register the BroadcastReceiver
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(mReceiver, filter);
            }
        } else {
            //switch bt off
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        ToggleButton toggleBluetooth = (ToggleButton) findViewById(R.id.toggleBluetooth);
        if (requestCode == REQUEST_ENABLE_BT) {
            if(resultCode == RESULT_OK){
                Log.i("Bt allowed", "User enabled bluetooth");
            }
            if (resultCode == RESULT_CANCELED) {
                //Write your code if there's no result
                Log.i("Bt not allowed","This app needs bt to function");
                Toast.makeText(this, "We need bluetooth to provide awesome functionality", Toast.LENGTH_LONG).show();
            }
        }
    }
    protected void onDestroy(){
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }
}
