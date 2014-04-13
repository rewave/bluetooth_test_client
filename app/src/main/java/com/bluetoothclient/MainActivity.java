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
import android.widget.Button;
import android.widget.Toast;


public class MainActivity extends ActionBarActivity {

    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    ArrayAdapter<String> devices;
    private final int REQUEST_ENABLE_BT = 1;
    private int USER_ALLOWED_BT = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button discover = (Button) findViewById(R.id.discover);
        discover.setOnClickListener(onDiscoveryCLick);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
    }

    View.OnClickListener onDiscoveryCLick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            findViewById(R.id.discover).setEnabled(false);
            switchBluetooth(true);
        }
    };

    private boolean startDiscovery(){
        if (mBluetoothAdapter.isDiscovering()) {
            Toast.makeText(this, "Discovery in process", Toast.LENGTH_LONG).show();
        } else {
            Boolean discoveryStarted = mBluetoothAdapter.startDiscovery();
            if (discoveryStarted) {
                Log.i("discovery", "started");
            } else {
                Log.i("discovery", "could not be started");
                Toast.makeText(this, "Discovery could not be started", Toast.LENGTH_LONG);
                findViewById(R.id.discover).setEnabled(true);
            }
            return discoveryStarted;
        }
        return false;
    }

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

                Toast.makeText(getApplicationContext(), "Found"+device.getName(), Toast.LENGTH_LONG).show();

                //devices.add(device.getName() + "\n" + device.getAddress());
            }
        }
    };

    private boolean switchBluetooth(boolean action){
        if (mBluetoothAdapter != null){
            if (action)
                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
            else if ( mBluetoothAdapter.isEnabled() ) return mBluetoothAdapter.disable();
        } else {
            // No fucking BT support
            Toast.makeText(getApplicationContext(), getString(R.string.no_bt_adapter), Toast.LENGTH_LONG).show();
        }
        return false;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode == REQUEST_ENABLE_BT){
            if (resultCode == RESULT_OK){
                USER_ALLOWED_BT = 1;
                startDiscovery();
            } else {
                // User denied access to bluetooth radio
                Toast.makeText(getApplicationContext(), getString(R.string.bt_access_required), Toast.LENGTH_LONG).show();
            }
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

    protected  void onDestroy(){
        super.onDestroy();
        mBluetoothAdapter.cancelDiscovery();
        unregisterReceiver(mReceiver);
    }
}
