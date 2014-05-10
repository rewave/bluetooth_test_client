package com.bluetoothclient;

import android.app.Activity;
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
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class MainActivity extends ActionBarActivity {

    private final String TAG = "MainActivity";
    private final int REQUEST_ENABLE_BT = 1;
    private final  BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    List<BluetoothDevice> pairedDevices = new ArrayList<BluetoothDevice>(mBluetoothAdapter.getBondedDevices());
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

        registerReceiver(BluetoothReceiver, filter); // Don't forget to unregister during onDestroy
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
            intent.putExtra("MAC_ADDRESS", selectedDevice.getAddress());
            startActivity(intent);
            return true;
        }
    };

    private boolean switchBluetooth(boolean action){
        if (mBluetoothAdapter != null){
            if (action) {
                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                } else {
                    //bt is on
                    return true;
                }
            } else if ( mBluetoothAdapter.isEnabled() ) return mBluetoothAdapter.disable();
        } else {
            // No fucking BT support
            Crouton.makeText(this, R.string.no_bt_adapter, Style.ALERT).show();
            return false;
        }
        return true;
    }

    private boolean startDiscovery(){
        if (availableDevices != null){
            availableDevices.clear();
            devicesAdapter.clear();
        }

        if (mBluetoothAdapter.isEnabled()) {
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
            }
            Boolean discoveryStarted = mBluetoothAdapter.startDiscovery();
            if (discoveryStarted) {
                Log.d("MainActivity", "Discovery started");
            } else {
                Log.d("MainActivity", "Discovery could not be started");
                Crouton.makeText(this, "Discovery could not be started", Style.ALERT).show();
            }
            return discoveryStarted;

        } else {
            switchBluetooth(true);
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_ENABLE_BT){
            if (resultCode == RESULT_OK) startDiscovery();
            else Crouton.makeText(this, "We need bluetooth to give you superpowers", Style.INFO).show();
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver BluetoothReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            //When user allows bluetooth usage, start discovering
            if(BluetoothAdapter.ACTION_REQUEST_ENABLE.equals(action)){
            }

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int deviceIndex = pairedDevices.indexOf(device);
                if (deviceIndex != -1){
                    //if device is paired, list the paired version
                    device = pairedDevices.get(deviceIndex);
                }
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
                Log.d(TAG, "Discovery Finished");
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
        unregisterReceiver(BluetoothReceiver);
        Crouton.cancelAllCroutons();
    }
}

