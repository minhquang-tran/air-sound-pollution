package edu.rmit.sepm.airsoundpollution;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

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
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void connect_bluetooth(View view) {
        TextView status = (TextView) findViewById(R.id.text_status);

        //bluetooth connection handling here
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            status.setText(getString(R.string.status_bt_na));
        } else {
            //enable bluetooth if not
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }

            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                //status.setText("" + pairedDevices.size());
                for (BluetoothDevice device : pairedDevices) {
                    //TODO: Determine if device is Arduino with device.get***
                    BluetoothDevice mDevice = device;

                    ConnectThread mConnectThread = new ConnectThread(mDevice);
                    mConnectThread.start();
                    status.setText(getString(R.string.status_connected));
                }
            }
        }

        //example on changing textview
        //status.setText(getString(R.string.status_pressed));
        //Alternate short version
        //((TextView) findViewById(R.id.text_status)).setText(getString(R.string.status_pressed));


    }

    public void receive_data(View view) {
        TextView status = (TextView) findViewById(R.id.text_status);

        //change name to temp.* so device can create a new file to write

        //read data from file

        //update local storage

        File internalFile = new File(getFilesDir(), "pollution_data.csv");

        //create file if not existed
        if (!internalFile.exists()) {
            try {
                internalFile.createNewFile();
                //status.setText(getFilesDir().toString());

            } catch (IOException e) {
                status.setText(e.toString());
            }
        } else status.setText("File existed at " + internalFile.getPath());

        //append new line
        try {
            BufferedWriter bW = new BufferedWriter(new FileWriter(internalFile, true));
            //future for each loop here
            bW.write("0\n1\n2\n3\n4\n5\n6\n7\n8\n9");
            bW.newLine();

            bW.flush();
            bW.close();

        } catch (IOException e) {
            status.setText(e.toString());
        }

    }

    public void view_data(View view) {
        TextView status = (TextView) findViewById(R.id.text_status);
        TextView data = (TextView) findViewById(R.id.text_data);
        //data.setMovementMethod(new ScrollingMovementMethod());

        File internalFile = new File(getFilesDir(), "pollution_data.csv");

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
        TextView status = (TextView) findViewById(R.id.text_status);
        File internalFile = new File(getFilesDir(), "pollution_data.csv");


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

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
        private final TextView status = (TextView) findViewById(R.id.text_status);

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            mmDevice = device;
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                status.setText(e.toString());
            }
            mmSocket = tmp;
        }

        public void run() {
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            mBluetoothAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
            } catch (IOException connectException) {
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    status.setText(closeException.toString());
                }
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                status.setText(e.toString());
            }
        }
    }
}


