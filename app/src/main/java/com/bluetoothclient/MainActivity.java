package com.bluetoothclient;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.*;
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

    private final String TAG = " ---> MainActivity";
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

        pullToDiscover = (PullToRefreshLayout) findViewById(R.id.devices_list_parent);
        ActionBarPullToRefresh.from(MainActivity.this)
                .allChildrenArePullable()
                .listener(onPullToDiscover)
                .setup(pullToDiscover);

        devicesAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        devicesListView = (ListView) findViewById(R.id.devices_list);
        devicesListView.setAdapter(devicesAdapter);

        availableDevices = new ArrayList<BluetoothDevice>();
        pairedDevices = new ArrayList<BluetoothDevice>(bluetoothAdapter.getBondedDevices());

        devicesListView.setOnItemClickListener(onDeviceClick);

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
            discoverNearbyDevices();
        }
    };

    AdapterView.OnItemClickListener onDeviceClick = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            bluetoothAdapter.cancelDiscovery();
            Intent intent = new Intent(MainActivity.this, ConnectedActivity.class);
            BluetoothDevice selectedDevice = availableDevices.get(position);
            intent.putExtra("MAC_ADDRESS", selectedDevice.getAddress());
            startActivity(intent);
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_ENABLE_BT){
            if (resultCode == RESULT_OK) listDevicesRoutine();
            else {
                stopDiscovering();
                Toast.makeText(MainActivity.this, R.string.bt_access_required, Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver BluetoothReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent){
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice foundDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                displayDevice(foundDevice);
            }

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                Log.d(TAG, "Discovery Started");
            }
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                stopDiscovering();
                Log.d(TAG, "Discovery Finished");
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem cancelDiscoveryAction;
        MenuItem filterDevicesAction;

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        final SearchView filterDevices = new SearchView(getSupportActionBar().getThemedContext());
        filterDevices.setQueryHint("Filter devices");
        filterDevices.setOnQueryTextListener(onFilterDevices);

        cancelDiscoveryAction =  menu.findItem(R.id.action_cancel_discovery);
        filterDevicesAction = menu.findItem(R.id.action_filter_devices);

        filterDevicesAction.setActionView(filterDevices);
        cancelDiscoveryAction.setVisible(false);

        if (bluetoothAdapter.isDiscovering()){
            filterDevicesAction.setVisible(false);
            cancelDiscoveryAction.setVisible(true);
        }
        return super.onCreateOptionsMenu(menu);
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

        if (id == R.id.action_cancel_discovery){
            stopDiscovering();
            return  true;
        }


        return super.onOptionsItemSelected(item);
    }

    protected  void onDestroy(){
        super.onDestroy();
        if (bluetoothAdapter.isDiscovering()) bluetoothAdapter.cancelDiscovery();
        unregisterReceiver(BluetoothReceiver);
        Crouton.cancelAllCroutons();
    }

    private void switchBluetooth(boolean action){
        if (bluetoothAdapter != null){
            if (action) {
                if (!bluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
            } else if ( bluetoothAdapter.isEnabled() ) bluetoothAdapter.disable();
        } else {
            // No fucking BT support
            Crouton.makeText(this, R.string.no_bt_adapter, Style.ALERT).show();
        }
    }

    private void listAllPairedDevices(){
        pairedDevices = new ArrayList<BluetoothDevice>(bluetoothAdapter.getBondedDevices());
        for (BluetoothDevice pairedDevice : pairedDevices){
            displayDevice(pairedDevice);
        }
    }

    private void discoverNearbyDevices(){
        if (bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.startDiscovery();
            invalidateOptionsMenu(); //used to show/hide cancelDiscovery button
            pullToDiscover.setRefreshing(true);
        } else {
            stopDiscovering();
            switchBluetooth(true);
        }
    }

    private void listDevicesRoutine(){
        switchBluetooth(true);
        if (bluetoothAdapter.isEnabled()) {
            listAllPairedDevices();
            if (availableDevices.isEmpty()) discoverNearbyDevices(); //start discovering automatically only if no paired devices.
        }
    }

    private void stopDiscovering(){
        pullToDiscover.setRefreshComplete();
        if(bluetoothAdapter.isDiscovering()) {
            invalidateOptionsMenu(); //used to show/hide cancelDiscovery button
            //http://stackoverflow.com/questions/10692755/how-do-i-hide-a-menu-item-in-the-actionbar#answer-13584471
            bluetoothAdapter.cancelDiscovery();
            Crouton.makeText(MainActivity.this, R.string.discovery_complete, Style.CONFIRM).show();
        }
    }

    private void displayDevice(BluetoothDevice device){

        if (device.getName() != null){
            if (availableDevices.indexOf(device) == -1) {
                //device not already on list
                Log.d(TAG, "Found "+device.getName());
                availableDevices.add(device);
                devicesAdapter.add(device.getName());
                devicesAdapter.notifyDataSetChanged();
            }
            else {
                Log.d(TAG, "Device already in list");
            }
        }
        else {
            Log.e(TAG, "Device name is null");
        }

    }
}

