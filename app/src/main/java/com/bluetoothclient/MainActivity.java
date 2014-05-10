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
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import uk.co.senab.actionbarpulltorefresh.extras.actionbarcompat.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ActionBarActivity {

    private final String TAG = "MainActivity";
    private final int REQUEST_ENABLE_BT = 1;
    private final  BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    List<BluetoothDevice> pairedDevices;
    private List<BluetoothDevice> availableDevices;
    private ArrayAdapter<String> devicesAdapter;
    private ListView devicesListView;
    private PullToRefreshLayout pullToDiscover;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pullToDiscover = (PullToRefreshLayout) findViewById(R.id.devicesListParent);
        ActionBarPullToRefresh.from(MainActivity.this)
                .allChildrenArePullable()
                .listener(onPullToDiscover)
                .setup(pullToDiscover);

        devicesAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        devicesListView = (ListView) findViewById(R.id.devicesList);
        devicesListView.setAdapter(devicesAdapter);

        availableDevices = new ArrayList<BluetoothDevice>();
        pairedDevices = new ArrayList<BluetoothDevice>(bluetoothAdapter.getBondedDevices());

        devicesListView.setOnItemLongClickListener(onDeviceClick);

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        registerReceiver(BluetoothReceiver, filter);

        listDevicesRoutine();

    }

    OnRefreshListener onPullToDiscover = new OnRefreshListener() {
        @Override
        public void onRefreshStarted(View view) {
            listDevicesRoutine();
        }
    };

    AdapterView.OnItemLongClickListener onDeviceClick = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            bluetoothAdapter.cancelDiscovery();
            Intent intent = new Intent(MainActivity.this, ConnectedActivity.class);
            BluetoothDevice selectedDevice = availableDevices.get(position);
            intent.putExtra("MAC_ADDRESS", selectedDevice.getAddress());
            startActivity(intent);
            return true;
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_ENABLE_BT){
            if (resultCode == RESULT_OK) listDevicesRoutine();
            else Crouton.makeText(this, R.string.bt_access_required, Style.INFO).show();
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver BluetoothReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice foundDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                try {
                    Log.d(TAG, foundDevice.getName() + " found");
                    if (availableDevices.indexOf(foundDevice) == -1) {
                        displayDevice(foundDevice);
                    } else {
                        Log.d(TAG, "Device already in list");
                    }

                } catch (NullPointerException e){
                    Log.e("MainActivity", "Device name is null");
                }
            }

            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                Log.d(TAG, "Discovery Finished");
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        final SearchView filterDevices = new SearchView(getSupportActionBar().getThemedContext());
        filterDevices.setQueryHint("Search");
        filterDevices.setOnQueryTextListener(onFilterDevices);

        menu.add(Menu.NONE, Menu.NONE, 1, "Search")
                .setIcon(R.drawable.abc_ic_search)
                .setActionView(filterDevices)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);

        return true;
    }

    SearchView.OnQueryTextListener onFilterDevices = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
            return false;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            devicesAdapter.getFilter().filter(newText);
            return false;
        }
    };

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
        if (bluetoothAdapter.isDiscovering()) bluetoothAdapter.cancelDiscovery();
        unregisterReceiver(BluetoothReceiver);
        Crouton.cancelAllCroutons();
    }

    private boolean switchBluetooth(boolean action){
        if (bluetoothAdapter != null){
            if (action) {
                if (!bluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                } else {
                    //bt is on
                    return true;
                }
            } else if ( bluetoothAdapter.isEnabled() ) return bluetoothAdapter.disable();
        } else {
            // No fucking BT support
            Crouton.makeText(this, R.string.no_bt_adapter, Style.ALERT).show();
            return false;
        }
        return true;
    }

    private void listAllPairedDevices(){
        pairedDevices = new ArrayList<BluetoothDevice>(bluetoothAdapter.getBondedDevices());
        for (BluetoothDevice pairedDevice : pairedDevices){
            displayDevice(pairedDevice);
        }
    }

    private void discoverNearbyDevices(){
        if (bluetoothAdapter.isEnabled()) {
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            Boolean discoveryStarted = bluetoothAdapter.startDiscovery();
            if (discoveryStarted) {
                Log.d("MainActivity", "Discovery started");
            } else {
                Crouton.makeText(this, R.string.discovery_start_error, Style.ALERT).show();
            }
        } else {
            switchBluetooth(true);
        }
    }

    private void listDevicesRoutine(){
        switchBluetooth(true);
        listAllPairedDevices();
        discoverNearbyDevices();
    }

    private void displayDevice(BluetoothDevice device){
        if (availableDevices.indexOf(device) == -1) {
            //device not already on list
            availableDevices.add(device);
            devicesAdapter.add(device.getName());
        }
    }
}

