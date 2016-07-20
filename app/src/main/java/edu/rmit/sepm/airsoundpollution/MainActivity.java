package edu.rmit.sepm.airsoundpollution;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void connect_bluetooth(View view) {
        //bluetooth connection handling here

        //example on changing textview
        TextView status = (TextView) findViewById(R.id.status_text);
        status.setText(getString(R.string.status_pressed));

        //Alternate short version
        //((TextView) findViewById(R.id.status_text)).setText(getString(R.string.status_pressed));


    }

    public void receive_data(View view) {
        //change name to temp.* so device can create a new file to write

        //read data from file

        //update local storage

    }

    public void view_data(View view) {
    }

    public void upload_data(View view) {
    }
}
