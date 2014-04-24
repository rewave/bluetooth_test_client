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
import android.widget.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class MainActivity extends ActionBarActivity {

    private final String TAG = "MainActivity";
    private final int REQUEST_ENABLE_BT = 1;
    private final  BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private List<BluetoothDevice> availableDevices;
    private ArrayAdapter<String> devicesAdapter;
    private ListView devicesListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button discover = (Button) findViewById(R.id.discover);
        discover.setOnClickListener(onDiscoveryCLick);

        devicesAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        devicesListView = (ListView) findViewById(R.id.devicesList);
        devicesListView.setAdapter(devicesAdapter);

        availableDevices = new ArrayList<BluetoothDevice>();

        devicesListView.setOnItemLongClickListener(onDeviceClick);

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
    }

    View.OnClickListener onDiscoveryCLick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            startDiscovery();
        }
    };

    AdapterView.OnItemLongClickListener onDeviceClick = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            mBluetoothAdapter.cancelDiscovery();
            Intent intent = new Intent(MainActivity.this, ConnectedActivity.class);
            BluetoothDevice selectedDevice = availableDevices.get(position);
            if (selectedDevice != null){
                intent.putExtra("MAC_ADDRESS", selectedDevice.getAddress());
                startActivity(intent);
            } else {
                Toast.makeText(MainActivity.this, "Device Not in range", Toast.LENGTH_LONG).show();
            }

            return true;
        }
    };

    private boolean switchBluetooth(boolean action){
        if (mBluetoothAdapter != null){
            if (action) {
                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
            } else if ( mBluetoothAdapter.isEnabled() ) return mBluetoothAdapter.disable();
        } else {
            // No fucking BT support
            Toast.makeText(MainActivity.this, getString(R.string.no_bt_adapter), Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode == REQUEST_ENABLE_BT){
            if (resultCode == RESULT_OK) startDiscovery();
            else {
                // User denied access to bluetooth radio
                findViewById(R.id.discover).setEnabled(true);
                Toast.makeText(MainActivity.this, getString(R.string.bt_access_required), Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean startDiscovery(){
        if (availableDevices != null){
            availableDevices.clear();
            devicesAdapter.clear();
        }

        if (switchBluetooth(true)) {

            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            if (pairedDevices!= null) {
                for (BluetoothDevice device : pairedDevices) {
                    Log.d(TAG, "Added already paired device "+ device.getName());
                    availableDevices.add(device);
                    devicesAdapter.add(device.getName());
                }
            }

            if (mBluetoothAdapter.isDiscovering()) {
                Toast.makeText(this, "Discovery in process", Toast.LENGTH_LONG).show();
            } else {
                Boolean discoveryStarted = mBluetoothAdapter.startDiscovery();
                if (discoveryStarted) {
                    Log.d("MainActivity", "Discovery started");
                    Toast.makeText(MainActivity.this, "Discovery started", Toast.LENGTH_LONG).show();
                } else {
                    Log.d("MainActivity", "Discovery could not be started");
                    Toast.makeText(MainActivity.this, "Discovery could not be started", Toast.LENGTH_LONG).show();
                }
                return discoveryStarted;
            }
        }
        return false;
    }

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            //When user allows bluetooth usage, start discovering
            if(BluetoothAdapter.ACTION_REQUEST_ENABLE.equals(action)){
                startDiscovery();
            }

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                try {
                    Log.d(TAG, device.getName() + " found");
                    if (availableDevices.indexOf(device) == -1) {
                        availableDevices.add(device);
                        devicesAdapter.add(device.getName());
                    } else {
                        Log.d(TAG, "Device already in list");
                    }

                } catch (NullPointerException e){
                    Log.e("MainActivity", "Device name is null");
                }

            }

            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                //Toast.makeText(MainActivity.this, "Discovery complete", Toast.LENGTH_LONG).show();
                Log.d("MainActivity", "Discovery Finished");
                findViewById(R.id.discover).setEnabled(true);
            }

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.endsWith(action)){
                findViewById(R.id.discover).setEnabled(false);
            }
        }
    };

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

    protected  void onDestroy(){
        super.onDestroy();
        if (mBluetoothAdapter.isDiscovering()) mBluetoothAdapter.cancelDiscovery();
        unregisterReceiver(mReceiver);
    }
}

