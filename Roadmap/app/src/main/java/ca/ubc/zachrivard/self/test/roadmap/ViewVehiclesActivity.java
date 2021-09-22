package ca.ubc.zachrivard.self.test.roadmap;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import ca.ubc.zachrivard.self.test.roadmap.api.Trip;
import ca.ubc.zachrivard.self.test.roadmap.api.Vehicle;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ViewVehiclesActivity extends Activity {

    public static final String TAG = "ViewVehiclesActivity";

    private App mApplication;

    private Map<String, Vehicle> mVehicleData;
    private VehicleAdapter mAdapter;

    private TextView mSummaryText;
    private RecyclerView mVehicleList;
    private Switch mActiveSwitch;
    private ProgressBar mProgressBar;

    private static ColorStateList mColorStateListAvailable;
    private static ColorStateList mColorStateListUnavailable;

    private Geocoder mGeocoder;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_object_list);

        mApplication = (App) getApplication();
        mGeocoder = new Geocoder(this);

        mColorStateListAvailable = ColorStateList.valueOf(getColor(R.color.quantum_googgreen));
        mColorStateListUnavailable = ColorStateList.valueOf(getColor(R.color.quantum_googred));

        mSummaryText = findViewById(R.id.summary_string);
        mVehicleList = findViewById(R.id.object_list);
        mActiveSwitch = findViewById(R.id.active_switch);
        mProgressBar = findViewById(R.id.progress_bar);

        mAdapter = new VehicleAdapter(new TreeMap<>());
        mVehicleList.setAdapter(mAdapter);
        mVehicleList.setLayoutManager(new LinearLayoutManager(this));

        mActiveSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Map<String, Vehicle> active = getAvailableVehicles();
                mAdapter.setVehicleData(active);
            } else {
                mAdapter.setVehicleData(mVehicleData);
            }
        });

        refreshData(null);
    }

    private Map<String, Vehicle> getAvailableVehicles() {
        Map<String, Vehicle> active = mVehicleData.entrySet()
                .stream()
                .filter((entry) -> entry.getValue().getTripId().equals("NONE"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return new TreeMap<>(active);
    }

    public void refreshData(View v) {
        mSummaryText.setText("Loading...");
        mProgressBar.setVisibility(View.VISIBLE);
        mApplication.getBackendWebService().getAllVehicles().enqueue(new Callback<Map<String, Vehicle>>() {
            @Override
            public void onResponse(Call<Map<String, Vehicle>> call, Response<Map<String, Vehicle>> response) {
                mVehicleData = new TreeMap<>(response.body());
                runOnUiThread(ViewVehiclesActivity.this::refreshUi);
                mProgressBar.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(Call<Map<String, Vehicle>> call, Throwable t) {
                Toast.makeText(ViewVehiclesActivity.this, "Failed to refresh list.", Toast.LENGTH_LONG)
                        .show();
                mProgressBar.setVisibility(View.GONE);
                mSummaryText.setText("Failed to load data.");
            }
        });
    }

    public void refreshUi() {
        int total = mVehicleData.size();
        long available = mVehicleData.values()
                .stream()
                .filter(vehicle -> vehicle.getTripId().equals("NONE"))
                .count();

        mSummaryText.setText(String.format("Total: (%d). Available: (%d)", total, available));

        mAdapter.setVehicleData(mActiveSwitch.isChecked() ? getAvailableVehicles() : mVehicleData);
    }


    public class VehicleAdapter extends RecyclerView.Adapter<VehicleHolder> {

        private List<String> ids = new ArrayList<>();
        private Map<String, Vehicle> vehicleData;

        public VehicleAdapter(Map<String, Vehicle> vehicleData) {
            setVehicleData(vehicleData);
        }

        public void setVehicleData(Map<String, Vehicle> vehicleData) {
            this.vehicleData = vehicleData;
            ids.clear();
            ids.addAll(vehicleData.keySet());
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VehicleHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.vehicle_list_item, viewGroup, false);
            return new VehicleHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VehicleHolder vehicleHolder, int i) {

            String id = ids.get(i);
            Vehicle vehicle = vehicleData.get(id);
            ColorStateList csl = vehicle.getTripId().equals("NONE")
                    ? mColorStateListAvailable
                    : mColorStateListUnavailable;

            vehicleHolder.vehicleId.setText(id);
            vehicleHolder.currentLocation.setText(getAddress(vehicle.getLocation()));
            vehicleHolder.status.setBackgroundTintList(csl);

            if (vehicle.getTripId().equals("NONE")) {
                vehicleHolder.tripLabel.setVisibility(View.GONE);
                vehicleHolder.tripDetails.setVisibility(View.GONE);
            } else {
                vehicleHolder.tripLabel.setVisibility(View.VISIBLE);
                vehicleHolder.tripDetails.setVisibility(View.VISIBLE);
                vehicleHolder.tripDetails.setText("Loading trip details...");
                mApplication.getBackendWebService().getTrip(vehicle.getTripId())
                        .enqueue(new Callback<Trip>() {
                            @Override
                            public void onResponse(Call<Trip> call, Response<Trip> response) {
                                Trip trip = response.body();
                                try {
                                    runOnUiThread(
                                            () -> vehicleHolder.tripDetails.setText(trip.getOrigin().getAddress() + " to " + trip.getDestination().getAddress())
                                    );
                                } catch (Exception e){}
                            }

                            @Override
                            public void onFailure(Call<Trip> call, Throwable t) {

                            }
                        });
            }
        }

        private String getAddress(Vehicle.Coordinate location) {
            try {
                return mGeocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1)
                        .get(0)
                        .getAddressLine(0);
            } catch (IOException e) {
                return String.format("%.4f x %.4f", location.getLongitude(), location.getLatitude());
            }
        }

        @Override
        public int getItemCount() {
            return vehicleData.size();
        }
    }

    public class VehicleHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView vehicleId;
        TextView tripLabel;
        TextView tripDetails;
        TextView currentLocation;
        FloatingActionButton status;

        public VehicleHolder(@NonNull View itemView) {
            super(itemView);

            vehicleId = itemView.findViewById(R.id.driver_name);
            currentLocation = itemView.findViewById(R.id.current_location);
            status = itemView.findViewById(R.id.available_button);
            tripLabel = itemView.findViewById(R.id.trip_label);
            tripDetails = itemView.findViewById(R.id.trip_details);
            itemView.setOnClickListener(this);
        }


        @Override
        public void onClick(View v) {
            Log.d(TAG, "onClick");
            Intent intent = new Intent(getApplicationContext(), ManagerActivity.class)
                    .putExtra("vehicleID", vehicleId.getText().toString())
                    .putExtra("Activity", "ViewVehiclesActivity");
            startActivity(intent);
        }
    }


}
