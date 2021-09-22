package ca.ubc.zachrivard.self.test.roadmap;

import java.util.*;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.view.View;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Button;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import ca.ubc.zachrivard.self.test.roadmap.api.Trip;
import ca.ubc.zachrivard.self.test.roadmap.bluetooth.BluetoothService;

public class MaintenanceRequestActivity extends Activity implements AdapterView.OnItemSelectedListener, BluetoothService.BluetoothObserver{

    private final String TAG =  MaintenanceRequestActivity.class.getName();

    private final String[] types = {"Engine", "Tires", "Windows", "Lights", "Mechanical", "Other"};
    private Map<String, Integer> requestTypes = new HashMap<String, Integer>(); // Might not need this actually
    private List<String> typesList;
    private String selectedType;

    private Button submitButton, cancelButton;
    private EditText description;
    private Spinner typesDropdown;

    private App mApplication;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maintenance_request_view);

        mApplication = (App) getApplication();
        BluetoothService.registerObserver(this);

        this.typesDropdown = (Spinner) findViewById(R.id.typeMaintenance);
        this.submitButton = (Button) findViewById(R.id.submitMaintenanceButton);
        this.cancelButton = (Button) findViewById(R.id.cancelMaintenanceButton);
        this.description = (EditText) findViewById(R.id.descriptionMaintenance);

        this.typesList = new ArrayList<>();

        for(int i = 0; i < types.length; i++){
            this.requestTypes.put(types[i], i); // Might get rid of this if we're getting rid of the requestTypes map
            this.typesList.add(types[i]);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, this.typesList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        this.typesDropdown.setAdapter(adapter);
        this.typesDropdown.setOnItemSelectedListener(this);

        this.selectedType = "Engine";
    }

    public void onNothingSelected(AdapterView<?> parent) {

    }

    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String type = (String) parent.getSelectedItem();
        Log.e(TAG, type);

        changeMaintenanceRequestType(type);
    }

    public void submitMaintenanceRequest(View view) {
        String description = this.description.getText().toString();
        Log.e(TAG, description);
        // TODO: Send description and maintenance request type to the pi via bluetooth
        sendMaintenacneRequest();

        finish();
    }

    public void cancelMaintenanceRequestDraft(View view) {
        description.getText().clear();

        finish();
    }

    private void changeMaintenanceRequestType(String type) {
        this.selectedType = type;
    }

    private void sendMaintenacneRequest(){

        Trip trip = MapsActivity.getTrip();
        //These will have to be put into the given activities to send requests
        JSONObject json = new JSONObject();
        JSONObject data = new JSONObject();

        try {
            //BT_EMERG
            json.put(BluetoothService.BluetoothObserver.KEY_REQUEST_TYPE,
                    BluetoothService.BluetoothObserver.REQUEST_BT_MAINTENANCE);

            data.put(BluetoothService.BluetoothObserver.KEY_TRIP_ID,
                    trip.getTripID());
            data.put(BluetoothService.BluetoothObserver.KEY_PROBLEM_TYPE,
                    types[typesDropdown.getSelectedItemPosition()]);
            data.put(BluetoothService.BluetoothObserver.KEY_PROBLEM_DESCRIPTION,
                    description.getText().toString());
            data.put(BluetoothService.BluetoothObserver.KEY_TIME_STAMP,
                    System.currentTimeMillis() / 1000L);
            data.put(BluetoothService.BluetoothObserver.KEY_VEHICLE_ID,
                    trip.getVehicleID());
            data.put(BluetoothService.BluetoothObserver.KEY_DRIVER_ID,
                    trip.getDriverID());
            data.put(BluetoothService.BluetoothObserver.KEY_READ_STATUS,
                    false);
            data.put(BluetoothService.BluetoothObserver.KEY_STATUS,
                    "open");
            data.put(BluetoothService.BluetoothObserver.KEY_TIME_STAMP,
                    System.currentTimeMillis() / 1000L);


            json.put(BluetoothService.BluetoothObserver.KEY_DATA, data);
            Log.e("JSON", json.toString(4));
            mApplication.getBluetoothService().issueCommand(json.toString());

        }catch (JSONException e){

        }

    }

    @Override
    public void onBluetoothEvent(String event, JSONObject payload) {
        runOnUiThread(() -> {
            switch (event) {

                case BluetoothService.BluetoothObserver.EVENT_MAINTENANCE: {

                    //Maybe wait for confirmation?
                    break;
                }
            }
        });
    }
}
