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
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private final static String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int SCAN_DEVICE_REQUEST = 2;
    static String imei;
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    public String fileName = "pollution_data.csv";
    JSONObject testJSON;
    private TextView status;
    private TextView data;
    //private BluetoothAdapter bleAdapter;
    private File internalFile;
    private BufferedWriter bW;
    private String mDeviceAddress = "";
    private String receivedText = "";
    private BluetoothLeService mBluetoothLeService;
    private ServiceConnection mServiceConnection;
    private UUID airSoundUUID = UUID.fromString(Attributes.AIR_SOUND_SOLUTION);
    private boolean mConnected = false;
    private boolean fetching = false;
    private BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "Action = " + action);
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                //updateConnectionState(R.string.connected);
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                //updateConnectionState(R.string.disconnected);
                //data.setText("");
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {

                try {
                    //Log.i(TAG,"AVAILABLE");

                    receivedText = intent.getStringExtra(BluetoothLeService.EXTRA_DATA).replaceAll(" ", "");
                    //byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                    //Log.i(TAG,receivedText);
                    status.setText(receivedText);
                    bW.write(receivedText);
                    //status.setText(new String(data));
                    //bW.write(new String(data));
                } catch (IOException e) {
                    status.setText(e.toString());
                    e.printStackTrace();
                }


            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    //Method below are for printing out the data in internal file. Used for debugging purpose
    public static String getStringFromFile(File fl) throws Exception {
        FileInputStream fin = new FileInputStream(fl);
        BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        fin.close();
        return sb.toString();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        status = (TextView) findViewById(R.id.text_status);
        data = (TextView) findViewById(R.id.text_data);
        internalFile = new File(getFilesDir(), fileName);
        imei = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();

        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            status.setText(getString(R.string.status_bt_na));
            findViewById(R.id.button_connect).setEnabled(false);
        }
        findViewById(R.id.button_service).setEnabled(false);
        findViewById(R.id.button_receive).setEnabled(false);

//        JSONObject testJSON = null;
        try {
            JSONObject air = new JSONObject();
            air.accumulate("no2", 123);
            air.accumulate("pm2", 456);
            air.accumulate("o3", 789);

            JSONObject sound = new JSONObject();
            sound.accumulate("level", "over");
            sound.accumulate("gain", 2.5);
            sound.accumulate("dB", null);

            JSONObject gps = new JSONObject();
            gps.accumulate("lat", 10.12304);
            gps.accumulate("lng", 110.012031);

            JSONObject data = new JSONObject();
            data.accumulate("ahqi", air);
            data.accumulate("sound", sound);
            data.accumulate("gps", gps);

            testJSON = new JSONObject();
            testJSON.accumulate("UUID", UUID.randomUUID());
            testJSON.accumulate("deviceID", imei);
            testJSON.accumulate("capturedAt", 1572407685);
            testJSON.accumulate("data", data);

        } catch (JSONException e) {
            status.setText(e.toString());
            e.printStackTrace();
        }
        data.setText(testJSON.toString());


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
        if (mServiceConnection != null)
            unbindService(mServiceConnection);
        mBluetoothLeService = null;
        if (bW != null) {
            try {
                bW.flush();
                bW.close();
            } catch (IOException e) {
                status.setText(e.toString());
                e.printStackTrace();
            }
        }
    }

    public void connect_bluetooth(View view) {
        Intent scanDeviceIntent = new Intent(this, DeviceScanActivity.class);
        startActivityForResult(scanDeviceIntent, SCAN_DEVICE_REQUEST);

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
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == SCAN_DEVICE_REQUEST && resultCode == RESULT_OK) {
            String deviceName = data.getStringExtra(EXTRAS_DEVICE_NAME);
            mDeviceAddress = data.getStringExtra(EXTRAS_DEVICE_ADDRESS);
            status.setText(deviceName + " " + mDeviceAddress);
            if (!mDeviceAddress.isEmpty()) {
                findViewById(R.id.button_service).setEnabled(true);
            }
        }

    }

    //Opening connection & service to receive data from broadcast
    public void bind_service(View view) {

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        findViewById(R.id.button_receive).setEnabled(true);

        //registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());


        //final boolean result = mBluetoothLeService.connect(mDeviceAddress);
        //Log.d(TAG, "Connect request result=" + result);

        //read data from file

        //update local storage
        //Toast.makeText(this, "abc", Toast.LENGTH_LONG).show();

        //create file if not existed


        //append new line

        /*try {
            //future for each loop here
            bW.write("0\n1\n2\n3\n4\n5\n6\n7\n8\n9");
            bW.newLine();

            bW.flush();
            bW.close();
            status.setText(getString(R.string.status_received));

        } catch (IOException e) {
            status.setText(e.toString());
        }*/


    }

    public void receive_data(View view) {

        BluetoothGattCharacteristic airSoundCharacteristic = findCharacteristic(airSoundUUID, mBluetoothLeService.getSupportedGattServices());
        if (airSoundCharacteristic == null) {
            Toast.makeText(getApplicationContext(), "Characteristic not found", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            if (!internalFile.exists()) {
                if (internalFile.createNewFile())
                    Toast.makeText(getApplicationContext(), "File created: " + internalFile.getPath(), Toast.LENGTH_LONG).show();
            } else
                Toast.makeText(getApplicationContext(), "New data appending to: " + internalFile.getPath(), Toast.LENGTH_LONG).show();

            bW = new BufferedWriter(new FileWriter(internalFile, true));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.i(TAG, airSoundCharacteristic.toString());

        if (!fetching) {
            mBluetoothLeService.setCharacteristicNotification(airSoundCharacteristic, true);
            ((Button) findViewById(R.id.button_receive)).setText(R.string.string_stop);
            fetching = true;
        } else {
            mBluetoothLeService.setCharacteristicNotification(airSoundCharacteristic, false);
            ((Button) findViewById(R.id.button_receive)).setText(R.string.string_receive);
            fetching = false;
        }

        //mBluetoothLeService.readCharacteristic(airSoundCharacteristic);

    }

    // Code to manage Service lifecycle.


    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.

    public void view_data(View view) {
        String output = null;
        try {
            if (!internalFile.exists()) {
                Toast.makeText(getApplicationContext(), R.string.status_file_not_exist, Toast.LENGTH_LONG).show();
                return;
            }
            output = getStringFromFile(internalFile);
            status.setText(internalFile.getPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
        data.setText(output);
    }

    public void upload_data(View view) {
        new UploadTask().execute();

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

    private BluetoothGattCharacteristic findCharacteristic(UUID target, List<BluetoothGattService> gattServices) {
        if (gattServices == null) return null;
        Log.i(TAG, "" + gattServices.size());
        Log.i(TAG, "finding " + target);

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {


            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                Log.i(TAG, "vs " + gattCharacteristic.getUuid());

                if (target.equals(gattCharacteristic.getUuid()))
                    return gattCharacteristic;

            }
        }
        return null;
    }

    public TextView getDataView() {
        return this.data;
    }

    private class UploadTask extends AsyncTask<String, Void, String> {
        private final String TASK_DONE = "Task done";
        private final String ERROR_FILE_NA = getString(R.string.status_file_not_exist);
        private final String ERROR_FILE_EMPTY = "File is empty!";
        private final String ERROR_TERMINATED = "RESPONSE NOT OK. TERMINATED";

        Scanner fileScanner;

        @Override
        protected String doInBackground(String... params) {
            return uploadData();
        }

        protected void onPostExecute(String result) {
                Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
                Log.i(TAG, result);
        }

        private String uploadData() {

            HttpURLConnection httpcon;
            OutputStream os;
            BufferedWriter writer;
            int responseCode;

            try {

                if (!internalFile.exists()) {
                    return ERROR_FILE_NA;
                }
                fileScanner = new Scanner(internalFile);
                if (!fileScanner.hasNextLine()) {
                    return ERROR_FILE_EMPTY;
                }

                while (fileScanner.hasNextLine()) {
                    String body = toJSON(fileScanner.nextLine());
                    if (body != null) {
                        Log.i(TAG,body);
                        httpcon = getConnection();
                        httpcon.connect();
                        os = httpcon.getOutputStream();
                        writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                        //writer.write(body);
                        writer.close();
                        responseCode = httpcon.getResponseCode();
                        Log.i(TAG, "" + responseCode);
                        os.close();
                        httpcon.disconnect();
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            //delete line
                        } else {
                            return ERROR_TERMINATED;
                        }
                    }
//                    Log.i(TAG, fileScanner.nextLine());
                }



//                String testString = testJSON.toString();
//                Log.i(TAG, testString);
//
//                for (int i = 0; i<1; i++) {
//                    httpcon = getConnection();
//                    httpcon.connect();
//                    os = httpcon.getOutputStream();
//                    writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
//                    writer.write(testString);
//                    writer.close();
//                    int res = httpcon.getResponseCode();
//                    Log.i(TAG, "" + res);
//                    os.close();
//
//                }


//                httpcon.disconnect();

            } catch (IOException e) {
                return e.toString();
            }
            return TASK_DONE;


        }

        private HttpURLConnection getConnection() {
            String newUrl = "http://welove.earth:1201/api/receiver";            // set up then connect
            HttpURLConnection httpcon = null;
            try {
                httpcon = (HttpURLConnection) ((new URL(newUrl).openConnection()));
                httpcon.setDoOutput(true);
                httpcon.setRequestProperty("Content-Type", "application/json");
                //httpcon.setRequestProperty("Accept", "application/json");
                httpcon.setRequestMethod("POST");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return httpcon;
        }

        private String toJSON(String dataPoint) {
            String[] dataArray = dataPoint.replaceAll(" ","").split(",");
            if (dataArray.length == DataSig.values().length) {
                try {
                    JSONObject air = new JSONObject();
                    air.accumulate("no2", 123);
                    air.accumulate(DataSig.pm2.name(), dataArray[DataSig.pm2.ordinal()]);
                    air.accumulate("o3", 789);

                    JSONObject sound = new JSONObject();
                    sound.accumulate(DataSig.level.name(), ( dataArray[DataSig.level.ordinal()].equals("1") ) ? "over" : "under");
                    sound.accumulate("gain", 2.5);
                    sound.accumulate("dB", null);

                    JSONObject gps = new JSONObject();
                    gps.accumulate(DataSig.lat.name(), dataArray[DataSig.lat.ordinal()]);
                    gps.accumulate(DataSig.lng.name(), dataArray[DataSig.lng.ordinal()]);

                    JSONObject data = new JSONObject();
                    data.accumulate("ahqi", air);
                    data.accumulate("sound", sound);
                    data.accumulate("gps", gps);

                    JSONObject grandJson = new JSONObject();
                    grandJson.accumulate("UUID", UUID.randomUUID());
                    grandJson.accumulate("deviceID", MainActivity.imei);
                    grandJson.accumulate(DataSig.capturedAt.name(), /*dataArray[DataSig.capturedAt.ordinal()]*/strToTimestamp(dataArray[DataSig.capturedAt.ordinal()]));
                    grandJson.accumulate("data", data);

                    return grandJson.toString();
                } catch (JSONException e) {
                    Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
            return null;
        }

        private long strToTimestamp(String input) {
            SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss.SSS", Locale.US);
            try {
                Date date = df.parse(input);
                return date.getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return 0;
        }
    }

}


