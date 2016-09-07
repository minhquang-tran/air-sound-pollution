package edu.rmit.sepm.airsoundpollution;

import android.Manifest;
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
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
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
import java.util.TimeZone;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private final static String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_READ_PHONE_STATE = 1;
    private static final int REQUEST_SCAN_DEVICE = 2;

    private String imei;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_READ_PHONE_STATE);
        }

        status = (TextView) findViewById(R.id.text_status);
        data = (TextView) findViewById(R.id.text_data);
        internalFile = new File(getFilesDir(), fileName);
        try {
            imei = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
            status.setText("Device ID: " + imei);
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            status.setText(getString(R.string.status_bt_na));
            findViewById(R.id.button_connect).setEnabled(false);
        }
        findViewById(R.id.button_service).setEnabled(false);
        findViewById(R.id.button_receive).setEnabled(false);

//        JSONObject testJSON = null;
//        try {
//            JSONObject air = new JSONObject();
//            air.accumulate("no2", 123);
//            air.accumulate("pm2", 456);
//            air.accumulate("o3", 789);
//
//            JSONObject sound = new JSONObject();
//            sound.accumulate("level", "over");
//            sound.accumulate("gain", 2.5);
//            sound.accumulate("dB", null);
//
//            JSONObject gps = new JSONObject();
//            gps.accumulate("lat", 10.12304);
//            gps.accumulate("lng", 110.012031);
//
//            JSONObject data = new JSONObject();
//            data.accumulate("ahqi", air);
//            data.accumulate("sound", sound);
//            data.accumulate("gps", gps);
//
//            testJSON = new JSONObject();
//            testJSON.accumulate("UUID", UUID.randomUUID());
//            testJSON.accumulate("deviceID", imei);
//            testJSON.accumulate("capturedAt", 1572407685);
//            testJSON.accumulate("data", data);
//
//        } catch (JSONException e) {
//            status.setText(e.toString());
//            e.printStackTrace();
//        }
//        data.setText(testJSON.toString());



        // Initializes Bluetooth adapter.
        //final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        //bleAdapter = bluetoothManager.getAdapter();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_READ_PHONE_STATE:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    imei = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
                    status.setText("Device ID: " + imei);
                }
                break;

            default:
                break;
        }
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
            try {
                unbindService(mServiceConnection);
            } catch(IllegalArgumentException e) {
                e.printStackTrace();
            }
        if (mBluetoothLeService != null)
            mBluetoothLeService.disconnect();
        if (bW != null) {
            try {
                bW.flush();
                bW.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void connect_bluetooth(View view) {
        // Start activity to scan for compatible device
        Intent scanDeviceIntent = new Intent(this, DeviceScanActivity.class);
        startActivityForResult(scanDeviceIntent, REQUEST_SCAN_DEVICE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Receive device info (MAC Address)
        if (requestCode == REQUEST_SCAN_DEVICE && resultCode == RESULT_OK) {
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

        try {
            if (!fetching) {
                fetching = true;
                if (airSoundCharacteristic == null) {
                    Toast.makeText(getApplicationContext(), "Characteristic not found", Toast.LENGTH_LONG).show();
                    return;
                }

                if (!internalFile.exists()) {
                    if (internalFile.createNewFile())
                        Toast.makeText(getApplicationContext(), "File created: " + internalFile.getPath(), Toast.LENGTH_LONG).show();
                } else
                    Toast.makeText(getApplicationContext(), "New data appending to: " + internalFile.getPath(), Toast.LENGTH_LONG).show();

                bW = new BufferedWriter(new FileWriter(internalFile, true));
//            bW.write("0\n1\n2\n3\n4\n5\n6\n7\n8\n9");

                Log.i(TAG, airSoundCharacteristic.toString());
                mBluetoothLeService.setCharacteristicNotification(airSoundCharacteristic, true);
                ((Button) findViewById(R.id.button_receive)).setText(R.string.string_stop);

            } else {
                fetching = false;
                mBluetoothLeService.setCharacteristicNotification(airSoundCharacteristic, false);
                ((Button) findViewById(R.id.button_receive)).setText(R.string.string_receive);
                bW.flush();
                bW.close();
                Log.i(TAG, "Flushed & closed");

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //mBluetoothLeService.readCharacteristic(airSoundCharacteristic);

    }

    // Write received data tp file
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

                    if (fetching) {
                        receivedText = intent.getStringExtra(BluetoothLeService.EXTRA_DATA).replaceAll(" ", "");
                        //byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                        Log.i(TAG, receivedText);
                        status.setText(receivedText);
                        bW.write(receivedText);
                        //status.setText(new String(data));
                        //bW.write(new String(data));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };


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
        data.setText(output.replaceAll(" ", ""));
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

    // Find the specific characteristic that the device used to send data
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


    // Upload file content to server
    private class UploadTask extends AsyncTask<String, String, String> {
        private final String ERROR_FILE_NA = getString(R.string.status_file_not_exist);
        private final String ERROR_FILE_EMPTY = "File is empty!";
        private final String ERROR_TERMINATED = "RESPONSE NOT OK. TERMINATED";
        private final String TASK_DONE = "Task done";

        BufferedReader bR;

        int linesCount;
        int lineNo;

        @Override
        protected String doInBackground(String... params) {
            return uploadData();
        }


        protected void onProgressUpdate(String... values) {
            status.setText("Sending line " + lineNo + " of " + linesCount);
            data.setText(values[0]);
        }

        protected void onPostExecute(String result) {
            Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
            status.setText("Done!");
            data.setText("");
            Log.i(TAG, result);
        }

        private String uploadData() {

            HttpURLConnection httpCon;
            OutputStream os;
            BufferedWriter writer;
            String body;
            int responseCode;

            try {

                if (!internalFile.exists()) {
                    return ERROR_FILE_NA;
                }

                bR = new BufferedReader(new FileReader(internalFile));
                linesCount = 0;
                while (bR.readLine() != null) linesCount++;
                bR.close();
                if (linesCount == 0) {
                    return ERROR_FILE_EMPTY;

                }

                bR = new BufferedReader(new FileReader(internalFile));

                lineNo = 0;
                while ((body = toJSON(bR.readLine())) != null) {
                    //String body = toJSON(bR.nextLine());
                    lineNo++;
                    publishProgress(body);
                    Log.i(TAG, body);
                    httpCon = getConnection();
                    httpCon.connect();
                    os = httpCon.getOutputStream();
                    writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                    writer.write(body);
                    writer.close();
                    responseCode = httpCon.getResponseCode();
                    Log.i(TAG, "" + responseCode);
                    os.close();
                    httpCon.disconnect();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        //delete line
                    } else {
                        return ERROR_TERMINATED;
                    }

//                    Log.i(TAG, fileScanner.nextLine());
                }

//                httpCon.disconnect();

            } catch (IOException e) {
                return e.toString();
            }
            return TASK_DONE;


        }

        private HttpURLConnection getConnection() {
            String newUrl = "http://welove.earth:1201/api/receiver";            // set up then connect
            HttpURLConnection httpCon = null;
            try {
                httpCon = (HttpURLConnection) ((new URL(newUrl).openConnection()));
                httpCon.setDoOutput(true);
                httpCon.setRequestProperty("Content-Type", "application/json");
                //httpCon.setRequestProperty("Accept", "application/json");
                httpCon.setRequestMethod("POST");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return httpCon;
        }

        private String toJSON(String dataPoint) {
            if (dataPoint == null) return null;
            String[] dataArray = dataPoint.replaceAll(" ", "").split(",");
            if (dataArray.length == DataSig.values().length) {
                try {
                    // + AIR
                    JSONObject air = new JSONObject();
                    // NO2
                    air.accumulate("no2", -1);
                    // PM2.5
                    air.accumulate(DataSig.pm2.name(), dataArray[DataSig.pm2.ordinal()]);
                    // O3
                    air.accumulate("o3", -1);

                    // + SOUND
                    JSONObject sound = new JSONObject();
                    // Level
                    sound.accumulate(DataSig.level.name(), (dataArray[DataSig.level.ordinal()].equals("1")) ? "over" : "under");
                    // Gain
                    sound.accumulate("gain", -1);
                    // dB
                    sound.accumulate("dB", -1);

                    // + COORDS
                    JSONObject gps = new JSONObject();
                    // Latitude
                    gps.accumulate(DataSig.lat.name(), dataArray[DataSig.lat.ordinal()]);
                    //Longitude
                    gps.accumulate(DataSig.lng.name(), dataArray[DataSig.lng.ordinal()]);

                    // ++ Data assembly
                    JSONObject data = new JSONObject();
                    data.accumulate("ahqi", air);
                    data.accumulate("sound", sound);
                    data.accumulate("gps", gps);

                    // ++ Extras
                    JSONObject grandJson = new JSONObject();
                    // UUID
                    grandJson.accumulate("UUID", UUID.randomUUID());
                    // IMEI
                    grandJson.accumulate("deviceID", imei);
                    // Timestamp
                    grandJson.accumulate(DataSig.capturedAt.name(), /*dataArray[DataSig.capturedAt.ordinal()]*/strToTimestamp(dataArray[DataSig.capturedAt.ordinal()]));
                    // Data
                    grandJson.accumulate("data", data);

                    return grandJson.toString();
                } catch (JSONException e) {
                    Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
            return null;
        }

        // TIme data to EPOCH timestamp (milliseconds from 1970/01/01
        private long strToTimestamp(String input) {
//            input = "19700101000000.000";
            SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss.SSS", Locale.US);
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
            try {
                Date date = df.parse(input);

                return date.getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return 0;
        }
    }

    // Used to filter received broadcasts
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
}


