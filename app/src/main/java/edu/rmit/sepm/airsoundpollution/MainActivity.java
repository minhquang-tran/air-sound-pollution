package edu.rmit.sepm.airsoundpollution;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
import java.io.PrintWriter;

public class MainActivity extends AppCompatActivity {
    TextView status;
    TextView data;
    BluetoothAdapter bleAdapter;
    File internalFile;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int SCAN_DEVICE_REQUEST = 2;


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

        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bleAdapter = bluetoothManager.getAdapter();
    }

    public void connect_bluetooth(View view) {

        //bluetooth connection handling here
        //BluetoothAdapter bleAdapter = BluetoothAdapter.getDefaultAdapter();
            //enable bluetooth if not
            if (bleAdapter == null || !bleAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }

        if(bleAdapter.isEnabled() && bleAdapter != null) {
            Intent scanDeviceIntent = new Intent(this, DeviceScanActivity.class);
            startActivityForResult(scanDeviceIntent, SCAN_DEVICE_REQUEST);
        }

        /*
            Set<BluetoothDevice> pairedDevices = bleAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                //status.setText("" + pairedDevices.size());
                for (BluetoothDevice device : pairedDevices) {
                    //TODO: Determine if device is Arduino with device.get***
                    BluetoothDevice mDevice = device;

                    mConnectThread = new ConnectThread(mDevice);
                    mConnectThread.start();
                    status.setText(getString(R.string.status_connected));
                }
            }
        */

        //example on changing textview
        //status.setText(getString(R.string.status_pressed));
        //Alternate short version
        //((TextView) findViewById(R.id.text_status)).setText(getString(R.string.status_pressed));


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == SCAN_DEVICE_REQUEST && resultCode == RESULT_OK) {
            BluetoothDevice device = data.getParcelableExtra("Device");
            status.setText(device.getUuids().toString());
        }

    }

    public void receive_data(View view) {

        //change name to temp.* so device can create a new file to write

        //read data from file

        //update local storage
        //Toast.makeText(this, "abc", Toast.LENGTH_LONG).show();

        //create file if not existed
        if (!internalFile.exists()) {
            try {
                if (internalFile.createNewFile())
                    Toast.makeText(this, "File created: " + internalFile.getPath(), Toast.LENGTH_LONG).show();

            } catch (IOException e) {
                status.setText(e.toString());
            }
        } else
            Toast.makeText(this, "New data appended to: " + internalFile.getPath(), Toast.LENGTH_LONG).show();


        //append new line
        try {
            BufferedWriter bW = new BufferedWriter(new FileWriter(internalFile, true));
            //future for each loop here
            bW.write("0\n1\n2\n3\n4\n5\n6\n7\n8\n9");
            bW.newLine();

            bW.flush();
            bW.close();
            status.setText(getString(R.string.status_received));

        } catch (IOException e) {
            status.setText(e.toString());
        }

    }

    public void view_data(View view) {

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

        //Empty file
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(internalFile);
            pw.close();
        } catch (FileNotFoundException e) {
            status.setText(e.toString());
        }

    }

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


