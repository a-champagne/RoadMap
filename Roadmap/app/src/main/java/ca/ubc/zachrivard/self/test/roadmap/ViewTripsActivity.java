package ca.ubc.zachrivard.self.test.roadmap;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import ca.ubc.zachrivard.self.test.roadmap.api.Trip;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ViewTripsActivity extends Activity {

    private static final String TAG = "ViewTripsActivity";
    private static final String apiKey = "AIzaSyBPYti_CalEH69wn_RX5nLrV6MwV7KOsR4";


    private App mApplication;

    private Map<String, Trip> mTripData;
    private TripAdapter mAdapter;

    private TextView mTitle;
    private TextView mSummaryText;
    private RecyclerView mTripList;
    private Switch mActiveSwitch;
    private ProgressBar mProgressBar;
    private DatabaseReference userRef;
    private DataSnapshot userData;


    private static ColorStateList mColorStateListActive;
    private static ColorStateList mColorStateListInactive;

    Map<String, String> polylineCache = new TreeMap<>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_object_list);

        mApplication = (App) getApplication();

        mColorStateListActive = ColorStateList.valueOf(getColor(R.color.quantum_googgreen));
        mColorStateListInactive = ColorStateList.valueOf(getColor(R.color.quantum_googred));

        mTitle = findViewById(R.id.object_type);
        mSummaryText = findViewById(R.id.summary_string);
        mTripList = findViewById(R.id.object_list);
        mActiveSwitch = findViewById(R.id.active_switch);
        mProgressBar = findViewById(R.id.progress_bar);
        Switch activeSwitch = findViewById(R.id.active_switch);
        activeSwitch.setText("Active Only");
        mTitle.setText("Trips");
        mAdapter = new TripAdapter(new TreeMap<>());
        mTripList.setAdapter(mAdapter);
        mTripList.setLayoutManager(new LinearLayoutManager(this));

        mActiveSwitch.setTextOn("Active Only");
        mActiveSwitch.setTextOff("Active Only");


        mActiveSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Map<String, Trip> active = getActiveTrips();
                mAdapter.setTripData(active);
            } else {
                mAdapter.setTripData(mTripData);
            }
        });
        getUserDataFirebase();
        refreshData(null);
    }

    private Map<String, Trip> getActiveTrips() {
        Map<String, Trip> active = mTripData.entrySet()
                .stream()
                .filter((entry) -> entry.getValue().getActive())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return new TreeMap<>(active);
    }

    public void refreshData(View v) {
        mSummaryText.setText("Loading...");
        mProgressBar.setVisibility(View.VISIBLE);
        mApplication.getBackendWebService().getAllTrips().enqueue(new Callback<Map<String, Trip>>() {
            @Override
            public void onResponse(Call<Map<String, Trip>> call, Response<Map<String, Trip>> response) {
                mTripData = new TreeMap<>(response.body());
                runOnUiThread(ViewTripsActivity.this::refreshUi);
                mProgressBar.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(Call<Map<String, Trip>> call, Throwable t) {
                Toast.makeText(ViewTripsActivity.this, "Failed to refresh list.", Toast.LENGTH_LONG)
                        .show();
                mProgressBar.setVisibility(View.GONE);
                mSummaryText.setText("Failed to load data.");
            }
        });
    }

    public void refreshUi() {
        int total = mTripData.size();
        long active = mTripData.values()
                .stream()
                .filter(Trip::getActive)
                .count();

        mSummaryText.setText(String.format("Total: (%d). Active: (%d)", total, active));

        mAdapter.setTripData(mTripData);
    }

    private void getUserDataFirebase() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference userRef = database.getReference("Users");

        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                userData = dataSnapshot;
                refreshData(null);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "Failed to read value.", databaseError.toException());
            }
        });
    }

    public class TripAdapter extends RecyclerView.Adapter<ViewTripsActivity.TripHolder> {

        private List<String> ids = new ArrayList<>();
        private Map<String, Trip> tripData;

        public TripAdapter(Map<String, Trip> tripData) {
            setTripData(tripData);
        }

        public void setTripData(Map<String, Trip> tripData) {
            this.tripData = tripData;
            ids.clear();
            ids.addAll(tripData.keySet());
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public TripHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.trip_list_item, viewGroup, false);
            return new TripHolder(view);
        }



        private String getUserNameFromID(String id){
            try {
                for (DataSnapshot user : userData.getChildren()) {
                    Log.d("id vs id", id + "    "+ user.getKey());
                    if (user.getKey().equals(id)) {
                        Log.d("they are equal", user.child("first_name").getValue().toString() +" "+ user.child("last_name").getValue().toString());
                        return user.child("first_name").getValue().toString() +" "+ user.child("last_name").getValue().toString();
                    }
                }
            } catch (Exception e) {
                return "NO_DRIVER";
            }
            return "NO_DRIVER";
        }
        @Override
        public void onBindViewHolder(@NonNull TripHolder tripHolder, int i) {

            String id = ids.get(i);
            Trip trip = tripData.get(id);

            tripHolder.setTripId(id);
            String name = getUserNameFromID(trip.getDriverID());
            Log.d("driverId", trip.getDriverID());
            if ("NO_DRIVER".equals(name)) {
                Log.d("now here", "doing this actually");
                name = "Unassigned";
            }
            tripHolder.origin.setText(trip.getOrigin().getAddress());
            tripHolder.destination.setText(trip.getDestination().getAddress());
            tripHolder.vehicle.setText(trip.getVehicleID());
            tripHolder.statusButton.setBackgroundTintList(
                    trip.getActive()
                    ? mColorStateListActive
                    : mColorStateListInactive
            );
            tripHolder.driverName.setText(name);
            loadMapsImage(trip, tripHolder.mapImage);
        }

        @Override
        public int getItemCount() {
            return tripData.size();
        }
    }

    private void loadMapsImage(Trip trip, ImageView imageView) {
        try {
            if (polylineCache.containsKey(trip.getTripID())) {
                String staticMapsUrl = getStaticMapsUrl(polylineCache.get(trip.getTripID()));
                Glide.with(ViewTripsActivity.this).load(staticMapsUrl).into(imageView);
            } else {
                mApplication.getBackendWebService().getTripMetadata(
                        trip.getOrigin().getLatitude() + "," + trip.getOrigin().getLongitude(),
                        trip.getDestination().getLatitude() + "," + trip.getDestination().getLongitude(),
                        apiKey
                ).enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        String polylineEncoding = "enc:" + response.body()
                                .get("routes")
                                .getAsJsonArray()
                                .get(0)
                                .getAsJsonObject()
                                .get("overview_polyline")
                                .getAsJsonObject()
                                .get("points")
                                .getAsString();

                        polylineCache.put(trip.getTripID(), polylineEncoding);

                        String staticMapsUrl = getStaticMapsUrl(polylineEncoding);
                        Glide.with(ViewTripsActivity.this).load(staticMapsUrl).into(imageView);
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                    }
                });
            }
        } catch (JsonParseException e) {
            e.printStackTrace();
        }
    }

    private String getStaticMapsUrl(String polyline) {
        String format = "https://maps.googleapis.com/maps/api/staticmap?path=%s&size=200x200&maptype=roadmap&key=%s";

        return String.format(
                format,
                polyline,
                apiKey
        );
    }

    public class TripHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        ImageView mapImage;
        TextView driverName;
        TextView origin;
        TextView destination;
        TextView vehicle;
        FloatingActionButton statusButton;

        private String tripId;

        public TripHolder(@NonNull View itemView) {
            super(itemView);

            mapImage = itemView.findViewById(R.id.maps_image);
            driverName = itemView.findViewById(R.id.driver_name);
            origin = itemView.findViewById(R.id.origin);
            destination = itemView.findViewById(R.id.destination);
            vehicle = itemView.findViewById(R.id.vehicle);
            statusButton = itemView.findViewById(R.id.status_button);

            itemView.setOnClickListener(this);
        }

        public void setTripId(String tripId) {
            this.tripId = tripId;
        }

        @Override
        public void onClick(View v) {
            if (tripId == null) return;

            Intent intent = new Intent(getApplicationContext(), ManagerActivity.class)
                    .putExtra("tripID", tripId)
                    .putExtra("Activity", "ViewTripsActivity");
            startActivity(intent);
        }
    }
}
