package edu.rmit.sepm.airsoundpollution;

import android.content.Context;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

/**
 * Created by minhq on 02-Sep-16.
 */
public class UploadTask extends AsyncTask<String, Integer, Double> {

    private Toast statusToast;

    @Override
    protected Double doInBackground(String... params) {
        uploadData(params[0]);
        return null;
    }

    public void uploadData(String internalFile) {

    }

    public HttpURLConnection getConnection() {
        String newUrl = "http://welove.earth:1201/api/receiver";            // set up then connect
        HttpURLConnection httpcon = null;
        try {
            httpcon = (HttpURLConnection) ((new URL(newUrl).openConnection()));
            httpcon.setDoOutput(true);
            httpcon.setRequestProperty("Content-Type", "application/json");
            httpcon.setRequestProperty("Accept", "application/json");
            httpcon.setRequestMethod("POST");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return httpcon;
    }

    private String toJSON(String dataPoint) {
        String[] dataArray = dataPoint.split(",");
        if (dataArray.length == DataSig.values().length) {
            try {
                JSONObject air = new JSONObject();
                air.accumulate(DataSig.no2.name(), dataArray[DataSig.no2.ordinal()]);
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

                JSONObject grandJson = new JSONObject();
                grandJson.accumulate("UUID", UUID.randomUUID());
                grandJson.accumulate("deviceID", MainActivity.imei);
                grandJson.accumulate(DataSig.capturedAt.name(), strToTimestamp(dataArray[DataSig.capturedAt.ordinal()]));
                grandJson.accumulate("data", data);

                return grandJson.toString();
            } catch (JSONException e) {

                statusToast.setText(e.toString());
                statusToast.show();
                e.printStackTrace();
            }
        }
        return null;
    }

    public String strToTimestamp(String input) {
        return null;
    }
}
