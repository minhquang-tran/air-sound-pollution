package edu.rmit.sepm.airsoundpollution;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();


    private TextView status;
    private TextView data;
    //private BluetoothAdapter bleAdapter;
    private File internalFile;
    private BufferedWriter bW;

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private String mDeviceAddress = "";
    private String receivedText = "";
    private BluetoothLeService mBluetoothLeService;

    private ServiceConnection mServiceConnection;
    private UUID airSoundUUID = UUID.fromString(Attributes.AIR_SOUND_SOLUTION);
    //private BluetoothGattCharacteristic airSoundCharacteristic = new BluetoothGattCharacteristic (airSoundUUID,22,0);
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<>();
    private boolean mConnected = false;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int SCAN_DEVICE_REQUEST = 2;

    private BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG,"Action = " + action);
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                //updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                //updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                data.setText("");
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                try {
                    Log.i(TAG,"AVAILABLE");

                    receivedText = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                    status.setText(receivedText);
                    bW.write(receivedText);
                } catch (IOException e) {
                    status.setText(e.toString());
                }


            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        status = (TextView) findViewById(R.id.text_status);
        data = (TextView) findViewById(R.id.text_data);
        internalFile = new File(getFilesDir(), "pollution_data.csv");

        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            status.setText(getString(R.string.status_bt_na));
            findViewById(R.id.button_connect).setEnabled(false);
        }
        findViewById(R.id.button_receive).setEnabled(false);


        // Initializes Bluetooth adapter.
        //final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        //bleAdapter = bluetoothManager.getAdapter();
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
        try {
            bW.flush();
            bW.close();
        } catch (IOException e) {
            status.setText(e.toString());
        }
    }

    public void connect_bluetooth(View view) {
        Intent scanDeviceIntent = new Intent(this, DeviceScanActivity.class);
        startActivityForResult(scanDeviceIntent, SCAN_DEVICE_REQUEST);


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == SCAN_DEVICE_REQUEST && resultCode == RESULT_OK) {
            String deviceName = data.getStringExtra(EXTRAS_DEVICE_NAME);
            mDeviceAddress = data.getStringExtra(EXTRAS_DEVICE_ADDRESS);
            status.setText(deviceName + " " + mDeviceAddress);
            if (!mDeviceAddress.isEmpty()) {
                findViewById(R.id.button_receive).setEnabled(true);
            }
        }

    }


    //Opening connection & service to receive data from broadcast
    public void receive_data(View view) {



        try {
            if (!internalFile.exists()) {
                if (internalFile.createNewFile())
                    Toast.makeText(this, "File created: " + internalFile.getPath(), Toast.LENGTH_LONG).show();
            } else
                Toast.makeText(this, "New data appending to: " + internalFile.getPath(), Toast.LENGTH_LONG).show();

            bW = new BufferedWriter(new FileWriter(internalFile, true));
        } catch (IOException e) {
            status.setText(e.toString());
        }

        mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder service) {
                Log.i(TAG, "Service called");
                mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
                if (!mBluetoothLeService.initialize()) {
                    Log.e(TAG, "Unable to initialize Bluetooth");
                    finish();
                }
                // Automatically connects to the device upon successful start-up initialization.

                mBluetoothLeService.connect(mDeviceAddress);

                /*BluetoothGattCharacteristic airSoundCharacteristic = findCharacteristic(airSoundUUID, mBluetoothLeService.getSupportedGattServices());
                Log.i(TAG,airSoundCharacteristic.toString());
                mBluetoothLeService.readCharacteristic(airSoundCharacteristic);
                mBluetoothLeService.setCharacteristicNotification(airSoundCharacteristic, true);*/


            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                mBluetoothLeService = null;
                Log.e(TAG, "Disconnected from " + mDeviceAddress);

            }
        };

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        //registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());



        //final boolean result = mBluetoothLeService.connect(mDeviceAddress);
        //Log.d(TAG, "Connect request result=" + result);

        //read data from file

        //update local storage
        //Toast.makeText(this, "abc", Toast.LENGTH_LONG).show();

        //create file if not existed



        //append new line
        /*
        try {
            //future for each loop here
            bW.write("0\n1\n2\n3\n4\n5\n6\n7\n8\n9");
            bW.newLine();

            bW.flush();
            bW.close();
            status.setText(getString(R.string.status_received));

        } catch (IOException e) {
            status.setText(e.toString());
        }
        */

    }

    public void view_data(View view) {
        BluetoothGattCharacteristic airSoundCharacteristic = findCharacteristic(airSoundUUID, mBluetoothLeService.getSupportedGattServices());
        Log.i(TAG,airSoundCharacteristic.toString());
        mBluetoothLeService.readCharacteristic(airSoundCharacteristic);
        mBluetoothLeService.setCharacteristicNotification(airSoundCharacteristic, true);

        String output = null;
        try {
            output = getStringFromFile(internalFile);
            status.setText(internalFile.getPath());
        } catch (Exception e) {
            if (e instanceof FileNotFoundException) {
                status.setText(getString(R.string.status_file_not_exist));
            } else {
                status.setText(e.toString());
            }
        }
        data.setText(output);

        //scroll to bottom if necessary
    }

    public void upload_data(View view) {

        Scanner fileScanner;
        try {
            fileScanner = new Scanner(internalFile);
            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine();
                data.append(line + "\n");
            }
        } catch (FileNotFoundException e) {
            status.setText(e.toString());
        }

        //Empty file
        /*
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(internalFile);
            pw.close();
        } catch (FileNotFoundException e) {
            status.setText(e.toString());
        }
        */

    }

    // Code to manage Service lifecycle.


    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.

    private BluetoothGattCharacteristic findCharacteristic(UUID target, List<BluetoothGattService> gattServices) {
        if (gattServices == null) return null;
        Log.i(TAG,""+gattServices.size());
        Log.i(TAG,"finding "+ target);
        mGattCharacteristics = new ArrayList<>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {


            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                Log.i(TAG,"vs " + gattCharacteristic.getUuid());

                if (target.equals(gattCharacteristic.getUuid()))
                        return gattCharacteristic;

            }
        }
        return null;
    }



    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }



    //2 Methods below are for printing out the data in internal file. Used for debugging purpose
    //http://www.java2s.com/Code/Java/File-Input-Output/ConvertInputStreamtoString.htm
    public static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    public static String getStringFromFile(/*String filePath*/ File fl) throws Exception {
        //File fl = new File(filePath);
        FileInputStream fin = new FileInputStream(fl);
        String ret = convertStreamToString(fin);
        //Make sure you close all streams.
        fin.close();
        return ret;
    }

}


