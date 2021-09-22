package ca.ubc.zachrivard.self.test.roadmap;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Geocoder;
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
import java.util.List;
import java.util.Locale;

public class ManagerMessages extends Activity {
    private ListView messagesListView;
    private HashMap<Integer, DataSnapshot> indexToMessageThread;
    private HashMap<Integer, String> indexToVehicle;
    private List<String> messageThreads;
    private Geocoder geo;
    private Context mContext;


    private FirebaseDatabase database;
    DatabaseReference myRef;

    private static final String TAG = ManagerMessages.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_messages);


        mContext = this;
        this.indexToMessageThread = new HashMap<Integer, DataSnapshot>();
        geo =  new Geocoder(this, Locale.getDefault());

        this.messagesListView = (ListView) findViewById(R.id.managerMessages);
        this.messageThreads = new ArrayList<String>();

        getMessagesFromDatabase();
    }

    private void displayMessagesFromFirebase(List<String> newMessageThreads, HashMap<Integer, DataSnapshot> newIndexes) {
        this.messageThreads = newMessageThreads;
        this.indexToMessageThread = newIndexes;
        ArrayAdapter adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, this.messageThreads);
        this.messagesListView.setAdapter(adapter);

        this.messagesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.e(TAG, Integer.toString(position));
                Intent intent = new Intent(mContext, ManagerViewMessageThread.class);
                DataSnapshot selectedTrip = indexToMessageThread.get(position);

                intent.putExtra("tripID", selectedTrip.getKey());
                intent.putExtra("userType", "manager");
                startActivity(intent);
            }
        });
    }

    public void getMessagesFromDatabase() {
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("Trips");

        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<String> newMessageThreads = new ArrayList<String>();
                HashMap<Integer, DataSnapshot> newIndexes = new HashMap<>();
                int index = 0;
                Log.e(TAG, "Sphincter");
                for(DataSnapshot trip : dataSnapshot.getChildren()) {
                    if (trip == null) continue;
                    StringBuilder currentTrip = new StringBuilder();
                    Log.e(TAG, "_____________________________" );
                    Log.e(TAG, trip.toString());

                    String vehicleID = (String) trip.child("vehicleID").getValue();
                    try {
                        double destinationLatitude = Double.parseDouble(trip.child("Destination").child("latitude").getValue().toString());
                        double destinationLongitude =  Double.parseDouble(trip.child("Destination").child("longitude").getValue().toString());

                        String destinationAddress = geo.getFromLocation(destinationLatitude, destinationLongitude, 1)
                                .get(0).getAddressLine(0);

                        currentTrip.append("Vehicle: " + vehicleID + ", \nDestination: " + destinationAddress);
                        String tripsListEntry = currentTrip.toString();
                        Log.e(TAG, tripsListEntry);
                        newMessageThreads.add(tripsListEntry);
                        newIndexes.put(index, trip);
                    } catch(Exception e) { Log.e(TAG, e.getMessage()); continue;}

                    ++index;
                }

                displayMessagesFromFirebase(newMessageThreads, newIndexes);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }
}
