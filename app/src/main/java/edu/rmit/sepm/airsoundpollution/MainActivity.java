package edu.rmit.sepm.airsoundpollution;

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

        File internalFile = new File(getFilesDir(), "pollution_data.csv");

        //create file if not existed
        if (!internalFile.exists()) {
            try {
                internalFile.createNewFile();
                //status.setText(getFilesDir().toString());

            } catch (IOException e) {
                e.printStackTrace();
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
            e.printStackTrace();
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
            e.printStackTrace();
            status.setText(R.string.status_file_not_found);
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
            e.printStackTrace();
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
