package com.bluetoothclient;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class MainActivity extends ActionBarActivity {

    private final String TAG = "MainActivity";
    private final int REQUEST_ENABLE_BT = 1;
    private final String MY_UUID = "a1a738e0-c3b3-11e3-9c1a-0800200c9a66";

    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private List<BluetoothDevice> availableDevices;
    private ConnectThread mConnectThread;
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
            mConnectThread = new ConnectThread(availableDevices.get(position));
            mConnectThread.run();
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
        }
        return false;
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
        switchBluetooth(true);
        if (mBluetoothAdapter.isDiscovering()) {
            Toast.makeText(this, "Discovery in process", Toast.LENGTH_LONG).show();
        } else {
            Boolean discoveryStarted = mBluetoothAdapter.startDiscovery();
            if (discoveryStarted) {
                Log.i("MainActivity", "Discovery started");
                Toast.makeText(MainActivity.this, "Discovery started", Toast.LENGTH_LONG).show();
            } else {
                Log.i("MainActivity", "Discovery could not be started");
                Toast.makeText(MainActivity.this, "Discovery could not be started", Toast.LENGTH_LONG).show();
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
                try {
                    Log.i(TAG, device.getName() + " found");
                    if (availableDevices.indexOf(device) == -1) {
                        availableDevices.add(device);
                        devicesAdapter.add(device.getName());
                    } else {
                        Log.i(TAG, "Device already in list");
                    }

                } catch (NullPointerException e){
                    Log.e("MainActivity", "Device name is null");
                }

            }

            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                Toast.makeText(MainActivity.this, "Discovery complete", Toast.LENGTH_LONG).show();
                Log.i("MainActivity", "Discovery Finished");
                findViewById(R.id.discover).setEnabled(true);
            }

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.endsWith(action)){
                availableDevices.clear();
                devicesAdapter.clear();
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

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID));
            } catch (IOException e) {

            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                Log.d(TAG, "Connecting to socket");
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                Log.e(TAG, "Unable to connect socket, IOException");
                try {

                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }

            // Do work to manage the connection (in a separate thread)
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
        ConnectedThread mConnectedThread;
        mConnectedThread = new ConnectedThread(mSocket);
        mConnectedThread.run();
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
            int bytes; // bytes returned from read()

            Log.i(TAG, "ConnectedThread @run activated");

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    //mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                    Toast.makeText(MainActivity.this, "Something received", Toast.LENGTH_LONG).show();
                    Log.i(TAG, "Something received");
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
}

