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

public class ManagerViewEmergenciesActivity extends Activity {
    private ListView emergencyListView;
    private HashMap<Integer, DataSnapshot> indexToEmergency;
    private HashMap<Integer, String> indexToVehicle;
    private List<String> emergencies;
    private FirebaseDatabase database;
    DatabaseReference myRef;

    private static final String TAG = ManagerViewEmergenciesActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_emergencies);

        this.indexToEmergency = new HashMap<Integer, DataSnapshot>();
        this.indexToVehicle = new HashMap<>();


        this.emergencyListView = (ListView) findViewById(R.id.managerEmergencies);
        this.emergencies = new LinkedList<String>();

        getEmergenciesFromFirebase();
    }

    private void displayRequestsFromFirebase(List<String> newEmergencies,
                                             HashMap<Integer, DataSnapshot> newEmergencyIndexes,
                                             HashMap<Integer, String> newVehicleIndexes) {

        this.emergencies = newEmergencies;
        this.indexToEmergency = newEmergencyIndexes;
        this.indexToVehicle = newVehicleIndexes;

        ArrayAdapter adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, this.emergencies);
        this.emergencyListView.setAdapter(adapter);

        this.emergencyListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.e(TAG, Integer.toString(position));
                navigateToEmergencyActivity(position);
                // TODO: switch activities to emergencies for specific vehicle
            }
        });
    }

    public void getEmergenciesFromFirebase() {
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("Emergency_Alerts");

        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<String> newEmergencies = new ArrayList<String>();
                HashMap<Integer, DataSnapshot> newEmergencyIndexes = new HashMap<>();
                HashMap<Integer, String> newVehicleIndexes = new HashMap<>();

                // TODO: Refactor to use Caelin's Trip and Vehicle objects
                int index = 0;
                for(DataSnapshot request : dataSnapshot.getChildren()) {

                        StringBuilder requestText = new StringBuilder();
                        String vehicleID =  (String) request.child("VehicleID").getValue().toString();
                        String description = request.child("Message").getValue().toString();
                        String descriptionPreview = description.length() > 35 ? description.substring(0, 34) + "..." : description;
                        requestText.append("Vehicle: " + vehicleID + "\nDescription: " + descriptionPreview);

                        if((boolean)request.child("ReadStatus").getValue() == false) {
                            requestText.append("\n(UNREAD)");
                        }
                        try {
                            if (request.child("Status").getValue().toString().equals("Closed")) {
                                requestText.append("\n(RESOLVED)");
                            } else {
                                requestText.append("\n(UNRESOLVED)");
                            }
                        } catch (Exception e) { }

                        newEmergencies.add(requestText.toString());

                        newEmergencyIndexes.put(index, request);
                        newVehicleIndexes.put(index, vehicleID);
                        index++;
                    }




                displayRequestsFromFirebase(newEmergencies, newEmergencyIndexes, newVehicleIndexes);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }

    private void navigateToEmergencyActivity(int index) {
        DataSnapshot emergency = this.indexToEmergency.get(index);
        String vehicleID = this.indexToVehicle.get(index);
        Intent intent = new Intent(this, ManagerViewEmergencyActivity.class);

        intent.putExtra("vehicleID", vehicleID);
        intent.putExtra("emergencyID", emergency.getKey());
        intent.putExtra("description", emergency.child("Message").getValue().toString());

        myRef.child(emergency.getKey()).child("ReadStatus").setValue(true);

        startActivity(intent);
    }
}
