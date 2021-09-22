package ca.ubc.zachrivard.self.test.roadmap;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;


public class CreateTripActivity extends FragmentActivity {
    private static final String TAG = MapsActivity.class.getName();
    private App mApplication;
    private String apiKey = "AIzaSyBPYti_CalEH69wn_RX5nLrV6MwV7KOsR4";
    private LatLng origin;
    private LatLng dest;
    private String originAddress;
    private String destAddress;
    private String vehicleID;
    private String tripID;
    private DataSnapshot vehicleData;
    private Spinner vehicles;
    private FirebaseDatabase database;
    private DatabaseReference vehicleRef;
    private ImageButton createTrip;
    private boolean pressed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_trip);
        mApplication = (App) getApplication();
        vehicles = findViewById(R.id.vehicle_spinner);
        tripID = null;
        pressed = false;
        // start listening for firebase updates
        getDataFromFirebase();
        // initialize activity to use Places API
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), apiKey);
        }
        // set up origin autocomplete widget
        AutocompleteSupportFragment autocompleteOrigin = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_origin);
        autocompleteOrigin.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS));
        autocompleteOrigin.getView().setBackgroundColor(Color.GRAY);
        autocompleteOrigin.setHint("Enter A Starting Address  ");
        autocompleteOrigin.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                Log.d("place", place.toString());
                origin = place.getLatLng();
                originAddress = place.getAddress();
            }
            @Override
            public void onError(Status status) {
            }
        });

        // set up destination autocomplete widget
        AutocompleteSupportFragment autocompleteDest = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_dest);

        autocompleteDest.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS));
        autocompleteDest.getView().setBackgroundColor(Color.GRAY);
        autocompleteDest.setHint("Enter A Finishing Address");
        autocompleteDest.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                Log.d("place", place.toString());
                dest = place.getLatLng();
                destAddress = place.getAddress();
            }
            @Override
            public void onError(Status status) {
            }
        });
        createTrip = findViewById(R.id.create_button);
        createTrip.setOnClickListener(view-> {
            if (!pressed) {
                pressed = true;
                makeTrip();
            }
        });

    }


    // set up listeners that update locally cached data from database on database changes
    public void getDataFromFirebase() {
        database = FirebaseDatabase.getInstance();
        vehicleRef = database.getReference("Vehicles");
        vehicleRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                vehicleData = dataSnapshot;
                getVehicleIDs();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w("firebase", "Failed to read value.", databaseError.toException());
            }
        });
    }

    // set up a spinner with all current vehicle IDs for user to select from
    public void getVehicleIDs(){
        vehicleID = "Unassigned";
        List<String> IDs = new ArrayList<>();
        IDs.add("Unassigned");
        for (DataSnapshot id : vehicleData.getChildren()) {
            IDs.add(id.getKey());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, IDs);
        vehicles.setAdapter(adapter);
        vehicles.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                vehicleID = IDs.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                vehicleID = "Unassigned";
            }

        });
    }

    // use POST trip API to create a new trip, given a valid origin, destination, and vehicle ID
    public void makeTrip() {
        if (origin == null || dest == null) {
            Toast.makeText(getApplicationContext(), "Please Select an Origin and Destination", Toast.LENGTH_SHORT).show();
            return;
        }
        if (vehicleID.equals("Unassigned")) {
            pressed = false;
            Toast.makeText(getApplicationContext(), "Please Select a Vehicle", Toast.LENGTH_SHORT).show();
            return;
        }
        Trip.Coordinate originCoord = new Trip.Coordinate(originAddress, origin.latitude, origin.longitude);
        Trip.Coordinate destCoord = new Trip.Coordinate(destAddress, dest.latitude, dest.longitude);
        Trip trip = new Trip.Builder(originCoord, destCoord, System.currentTimeMillis()).setVehicleId(vehicleID).build();
        // remove this when response fixed

        mApplication.getBackendWebService().createTrip(trip).enqueue(new Callback<Trip>() {
            @Override
            public void onResponse(Call<Trip> call, Response<Trip> response) {
                Log.d(TAG, "Response: " + response.body().toString());
                tripID = response.body().getTripID();
                Intent intent = new Intent(getApplicationContext(), ManagerActivity.class);
                startActivity(intent.putExtra("tripID", tripID).putExtra("Activity", "CreateTripActivity"));
            }

            @Override
            public void onFailure(Call<Trip> call, Throwable t) {
                Log.d(TAG, "Failure: " + t.getMessage());
            }
        });
    }

}

