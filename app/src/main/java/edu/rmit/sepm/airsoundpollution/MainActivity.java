package edu.rmit.sepm.airsoundpollution;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void connect_bluetooth(View view) {
        TextView status = (TextView) findViewById(R.id.text_status);

        //bluetooth connection handling here

        //example on changing textview
        status.setText(getString(R.string.status_pressed));
        //Alternate short version
        //((TextView) findViewById(R.id.text_status)).setText(getString(R.string.status_pressed));


    }

    public void receive_data(View view) {
        TextView status = (TextView) findViewById(R.id.text_status);

        //change name to temp.* so device can create a new file to write

        //read data from file

        //update local storage
        String filePath = getFilesDir().toString() + "/pollution_data.csv";

        File internalFile = new File(filePath);

        //create file if not existed
        if(!internalFile.exists()) {
            try {
                internalFile.createNewFile();
                //status.setText(getFilesDir().toString());

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else status.setText("File existed at " + filePath);

        //append new line

    }

    public void view_data(View view){
        TextView status = (TextView) findViewById(R.id.text_status);
        TextView data = (TextView) findViewById(R.id.text_data);
        data.setMovementMethod(new ScrollingMovementMethod());

        String filePath = getFilesDir().toString() + "/pollution_data.csv";
        //String filePath = "data/pollution_data.csv";
        File internalFile = new File(filePath);

        String output = null;
        try {
            output = getStringFromFile(filePath);
            status.setText(filePath);
        } catch (Exception e) {
            e.printStackTrace();
            status.setText(R.string.status_file_not_found);
        }
        data.setText(output);
    }

    public void upload_data(View view) {

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

    public static String getStringFromFile (String filePath) throws Exception {
        File fl = new File(filePath);
        FileInputStream fin = new FileInputStream(fl);
        String ret = convertStreamToString(fin);
        //Make sure you close all streams.
        fin.close();
        return ret;
    }
}
