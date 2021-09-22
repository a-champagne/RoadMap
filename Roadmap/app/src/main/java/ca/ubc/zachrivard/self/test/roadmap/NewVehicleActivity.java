package ca.ubc.zachrivard.self.test.roadmap;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import ca.ubc.zachrivard.self.test.roadmap.api.Trip;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.ubc.zachrivard.self.test.roadmap.api.Vehicle;
import ca.ubc.zachrivard.self.test.roadmap.bluetooth.BluetoothService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class NewVehicleActivity extends FragmentActivity implements BluetoothService.BluetoothObserver{
    private static final String TAG = MapsActivity.class.getName();
    private static final int REQUEST_ENABLE_BT = 391;


    private App mApplication;
    private String apiKey = "AIzaSyBPYti_CalEH69wn_RX5nLrV6MwV7KOsR4";
    private ImageButton createVehicle;
    private ImageButton setupRemote;
    private EditText vehicleIDEdit;
    private LatLng location;
    private boolean pressed;

    private StatusDialog mPopup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_vehicle);
        location = null;

        mApplication = (App) getApplication();
        BluetoothService.registerObserver(this);

        pressed = false;
        // set up activity to use Places API
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), apiKey);
        }
        // set up autocomplete fragment for vehicle starting location
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_loc);
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
        autocompleteFragment.getView().setBackgroundColor(Color.GRAY);
        autocompleteFragment.setHint("Enter Vehicle Location");
        // when someone selects a place, obtain the place's latitude and longitude and store it locally
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                location = place.getLatLng();
            }
            @Override
            public void onError(Status status) {
            }
        });
        vehicleIDEdit = findViewById(R.id.id_edit_text);
        // to set up a vehicle's remote module via bluetooth
        setupRemote = findViewById(R.id.setup_bluetooth);
        setupRemote.setOnClickListener(view->{
            String ID = vehicleIDEdit.getText().toString();
            // require that the vehicle ID string is not empty before attempting bluetooth setup
            if (!ID.equals("")) {
                enableBluetooth();
            }
        });
        // set up button for registering vehicle in database
        createVehicle = findViewById(R.id.create_vehicle);
        createVehicle.setOnClickListener(view->{
            String ID = vehicleIDEdit.getText().toString();
            // require that location and vehicle ID have been chosen
            if (!ID.equals("") && location != null && !pressed) {
                pressed = true;
                Vehicle vehicle = new Vehicle.Builder(ID, new Vehicle.Coordinate(location.latitude, location.longitude)).build();
                // use backend POST vehicle API to register a new vehicle in the database
                mApplication.getBackendWebService().createVehicle(vehicle).enqueue(new Callback<Vehicle>() {
                    @Override
                    public void onResponse(Call<Vehicle> call, Response<Vehicle> response) {
                        // code 400 means vehicle ID already exists
                        if (response.code()==400) {
                            Toast.makeText(getApplicationContext(), "Please choose a unique Vehicle ID", Toast.LENGTH_SHORT).show();
                            pressed = false;
                        } else {
                            // if successful add, bring us back to map view, and highlight the new vehicle
                            Intent intent = new Intent(getApplicationContext(), ManagerActivity.class);
                            startActivity(intent.putExtra("vehicleID", ID).putExtra("Activity", "NewVehicleActivity"));
                        }
                    }

                    @Override
                    public void onFailure(Call<Vehicle> call, Throwable t) {
                        Log.d(TAG, "Failure: " + t.getMessage());
                    }
                });
            }
        });

    }


    /*
     * Attempts to set up a new remote module with a specified vehicleID via bluetooth
     */
    private void setupNewVehicle(String vehicleID){

        JSONObject json = new JSONObject();
        JSONObject data = new JSONObject();

        try {
            json.put(BluetoothService.BluetoothObserver.KEY_REQUEST_TYPE,
                    BluetoothService.BluetoothObserver.REQUEST_BT_UPDATE_ID);

            data.put(BluetoothService.BluetoothObserver.KEY_VEHICLE_ID,
                    vehicleID);

            json.put(BluetoothService.BluetoothObserver.KEY_DATA, data);
            Log.e("Updated Vehicle ID:\n", json.toString(4));
            mApplication.getBluetoothService().issueCommand(json.toString());

        }catch (JSONException e) {}
    }

    /*
     * Set up Bluetooth adapter to communicate with remote unit
     */
    private void enableBluetooth(){
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if(adapter == null){
            Toast.makeText(this,
                    "Bluetooth not supported on this device!", Toast.LENGTH_SHORT).show();
            return;
        }

        if(adapter.isEnabled()) {
            mApplication.getBluetoothService().beginService();
        }else{
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);
        }
    }

    /*
     * Bluetooth event listener
     */
    @Override
    public void onBluetoothEvent(String event, JSONObject payload) {

        runOnUiThread(() -> {
            switch (event){
                case BluetoothService.BluetoothObserver.EVENT_DISCOVERY_BEGINNING: {

                    Log.e(TAG, BluetoothService.BluetoothObserver.EVENT_DISCOVERY_BEGINNING);
                    mPopup = new StatusDialog(this,"Looking for nearby vehicle");

                    mPopup.setCancelable(false);
                    mPopup.show();

                    break;
                }

                case BluetoothService.BluetoothObserver.EVENT_CONNECTION_ESTABLISHED: {

                    Log.e(TAG, BluetoothService.BluetoothObserver.EVENT_CONNECTION_ESTABLISHED);

                    if(mPopup != null) {
                        mPopup.dismiss();
                    }

                    setupNewVehicle(vehicleIDEdit.getText().toString());
                    vehicleIDEdit.setText("");
                    break;
                }

                case REQUEST_BT_UPDATE_ID: {

                    try{
                        String newID = payload.getString(BluetoothService.BluetoothObserver.KEY_DATA);

                        String toastMsg = "Vehicle ID updated to: " + newID;


                        Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show();

                    }catch (JSONException e){}

                    break;
                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case REQUEST_ENABLE_BT: {

                if(resultCode != Activity.RESULT_OK){
                    Toast.makeText(this,
                            "Please enable Bluetooth to setup new vehicle.", Toast.LENGTH_SHORT).show();
                    startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);
                } else {
                    mApplication.getBluetoothService().beginService();
                }

                break;
            }
        }
    }
}

