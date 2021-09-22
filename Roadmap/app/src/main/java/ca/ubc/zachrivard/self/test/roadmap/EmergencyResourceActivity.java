package ca.ubc.zachrivard.self.test.roadmap;

import android.content.Intent;
import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import ca.ubc.zachrivard.self.test.roadmap.api.Trip;
import ca.ubc.zachrivard.self.test.roadmap.bluetooth.BluetoothService;


public class EmergencyResourceActivity extends Activity implements BluetoothService.BluetoothObserver{

    Button submitEmergencyRequestButton;
    Button cancelEmergencyRequestButton;
    EditText emergencyDescription;

    private App mApplication;

    private static final String TAG = EmergencyResourceActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_emergency_request);

        mApplication = (App) getApplication();
        BluetoothService.registerObserver(this);

        submitEmergencyRequestButton = (Button) findViewById(R.id.submitEmergencyRequest);
        submitEmergencyRequestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitEmergencyRequest();
                finish();
            }
        });
        cancelEmergencyRequestButton = (Button) findViewById(R.id.cancelEmergencyRequest);
        cancelEmergencyRequestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        emergencyDescription = (EditText) findViewById(R.id.emergencyDescription);
    }

    private void submitEmergencyRequest(){

        Trip trip = MapsActivity.getTrip();
        //These will have to be put into the given activities to send requests
        JSONObject json = new JSONObject();
        JSONObject data = new JSONObject();

        try {
            //BT_EMERG
            json.put(BluetoothService.BluetoothObserver.KEY_REQUEST_TYPE,
                    BluetoothService.BluetoothObserver.REQUEST_BT_EMERGENCY);

            data.put(BluetoothService.BluetoothObserver.KEY_TRIP_ID,
                    trip.getTripID());
            data.put(BluetoothService.BluetoothObserver.KEY_MESSAGE,
                    emergencyDescription.getText().toString());
            data.put(BluetoothService.BluetoothObserver.KEY_TIME_STAMP,
                    System.currentTimeMillis() / 1000L);
            data.put(BluetoothService.BluetoothObserver.KEY_VEHICLE_ID,
                    trip.getVehicleID());
            data.put(BluetoothService.BluetoothObserver.KEY_DRIVER_ID,
                    trip.getDriverID());
            data.put(BluetoothService.BluetoothObserver.KEY_READ_STATUS,
                    false);


            json.put(BluetoothService.BluetoothObserver.KEY_DATA, data);
            Log.e("JSON", json.toString(4));
            mApplication.getBluetoothService().issueCommand(json.toString());

            Toast.makeText(this, "Report Submitted", Toast.LENGTH_SHORT).show();

        }catch (JSONException e){

        }
    }


    @Override
    public void onBluetoothEvent(String event, JSONObject payload) {
        runOnUiThread(() -> {
            switch (event) {

                case BluetoothService.BluetoothObserver.EVENT_EMERGENCY: {

                    //Maybe wait for confirmation?
                    break;
                }
            }
        });
    }
}
