package ca.ubc.zachrivard.self.test.roadmap;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class ManagerViewMaintenanceRequests extends Activity {
    private ListView maintenanceRequestsListView;
    private HashMap<Integer, DataSnapshot> indexToMaintenanceRequest;
    private HashMap<Integer, String> indexToVehicle;
    private List<String> maintenanceRequests;
    private FirebaseDatabase database;
    DatabaseReference myRef;

    private static final String TAG = ManagerViewMaintenanceRequests.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_maintenance_requests);

        this.indexToMaintenanceRequest = new HashMap<Integer, DataSnapshot>();
        this.indexToVehicle = new HashMap<>();


        this.maintenanceRequestsListView = (ListView) findViewById(R.id.managerMaintenanceRequests);
        this.maintenanceRequests = new LinkedList<String>();

        getMaintenanceRequestsFromFirebase();
    }

    private void displayRequestsFromFirebase(List<String> newMaintenanceRequests,
                                             HashMap<Integer, DataSnapshot> newRequestIndexes,
                                             HashMap<Integer, String> newVehicleIndexes) {

        this.maintenanceRequests = newMaintenanceRequests;
        this.indexToMaintenanceRequest = newRequestIndexes;
        this.indexToVehicle = newVehicleIndexes;

        ArrayAdapter adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, this.maintenanceRequests);
        this.maintenanceRequestsListView.setAdapter(adapter);

        this.maintenanceRequestsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.e(TAG, Integer.toString(position));
                navigateToRequestActivity(position);
                // TODO: switch activities to maintenance requests for specific vehicle
            }
        });
    }

    public void getMaintenanceRequestsFromFirebase() {
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("Maintenance_Alerts");

        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<String> newMaintenanceRequests = new ArrayList<String>();
                HashMap<Integer, DataSnapshot> newRequestIndexes = new HashMap<>();
                HashMap<Integer, String> newVehicleIndexes = new HashMap<>();

                // TODO: Refactor to use Caelin's Trip and Vehicle objects
                int index = 0;
                for(DataSnapshot vehicle : dataSnapshot.getChildren()) {
                    String vehicleID =  (String) vehicle.getKey();

                    for(DataSnapshot request : vehicle.getChildren()) {
                        StringBuilder requestText = new StringBuilder();
                        String description = request.child("ProblemDescription").getValue().toString();
                        String descriptionPreview = description.length() > 35 ? description.substring(0, 34) + "..." : description;
                        requestText.append("Vehicle: " + vehicleID + "\nDescription: " + descriptionPreview);

                        if((boolean)request.child("ReadStatus").getValue() == false) {
                            requestText.append("\n(UNREAD)");
                        }
                        if (request.child("Status").getValue().toString().equals("Closed")) {
                            requestText.append("\n(RESOLVED)");
                        } else {
                            requestText.append("\n(UNRESOLVED)");
                        }

                        newMaintenanceRequests.add(requestText.toString());
                        newRequestIndexes.put(index, request);
                        newVehicleIndexes.put(index, vehicleID);
                        index++;
                    }
                }

                displayRequestsFromFirebase(newMaintenanceRequests, newRequestIndexes, newVehicleIndexes);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }

    private void navigateToRequestActivity(int index) {
        DataSnapshot maintenanceRequest = this.indexToMaintenanceRequest.get(index);
        String vehicleID = this.indexToVehicle.get(index);
        Intent intent = new Intent(this, ManagerViewMaintenanceRequest.class);

        intent.putExtra("vehicleID", vehicleID);
        intent.putExtra("requestID", maintenanceRequest.getKey());
        intent.putExtra("description", maintenanceRequest.child("ProblemDescription").getValue().toString());

        myRef.child(vehicleID).child(maintenanceRequest.getKey()).child("ReadStatus").setValue(true);

        startActivity(intent);
    }
}
