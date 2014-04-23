package com.bluetoothclient;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;


public class ConnectedActivity extends ActionBarActivity {

    private final String TAG = "ConnectedActivity";
    private final String MY_UUID = "a1a738e0-c3b3-11e3-9c1a-0800200c9a66";
    //private final String MY_UUID = "94f39d29-7d6d-437d-973b-fba39e49d4ee";
    private final  BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothDevice mBluetoothDevice;
    private String mac_address = null;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread = null;
    public static final int BOND_BONDED = 12;

    private EditText chatMessage;

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

        Button pingButton = (Button) findViewById(R.id.pingButton);
        Button closeConnection = (Button) findViewById(R.id.closeConnection);

        chatMessage = (EditText) findViewById(R.id.chatMessage);

        pingButton.setOnClickListener(onPingButtonClick);
        closeConnection.setOnClickListener(onCloseConnectionClick);

        mConnectThread = new ConnectThread(mBluetoothDevice);
        mConnectThread.start();
    }

    View.OnClickListener onPingButtonClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mConnectedThread != null) {
                String c = chatMessage.getText().toString();
                chatMessage.setText("");
                mConnectedThread.write(c.getBytes());
            } else {
                Log.i(TAG, "Connection to socket in process. Please wait");
            }
        }
    };

    View.OnClickListener onCloseConnectionClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mConnectedThread != null) {
                mConnectedThread.cancel();
                Intent intent = new Intent(ConnectedActivity.this, MainActivity.class);
                startActivity(intent);
            } else {
                Log.i(TAG, "Connection to socket in process. Please wait");
            }

        }
    };

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
            while(true) {
                bondState = mmDevice.getBondState();
                if (bondState == BOND_BONDING) {
                    Log.i(TAG, "Bonding in process");
                } else if (bondState == BOND_BONDED) {
                    Log.i(TAG, "Bond complete");
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
        Log.i(TAG, "Paired. You may now kiss the bride.");
        mConnectedThread.start();
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private TextView chatMessage;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            chatMessage = (TextView) findViewById(R.id.chatMessage);

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
                    //Toast.makeText(MainActivity.this, new String(buffer, "UTF-8"), Toast.LENGTH_LONG).show();
                    Log.i(TAG,"Received : "+ new String(buffer, "UTF-8"));
                    chatMessage.setText(new String(buffer, "UTF-8"));

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
