package ca.ubc.zachrivard.self.test.roadmap;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ManagerViewEmergencyActivity extends Activity {

    private String requestID;
    private String vehicleID;

    private Button markFixedButton;
    private TextView requestDescriptionTextView;
    private TextView vehicleIDTextView;

    private FirebaseDatabase database;
    DatabaseReference myRef; // Maybe make this private and in the maintenance request list view

    private static final String TAG = ManagerViewEmergenciesActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_emergency);

        this.markFixedButton = (Button) findViewById(R.id.markFixedButton);
        this.requestDescriptionTextView = (TextView) findViewById(R.id.requestDescriptionText);
        this.vehicleIDTextView = (TextView) findViewById(R.id.requestVehicleID);

        Intent intent = getIntent();
        String vehicleID = intent.getStringExtra("vehicleID");
        String requestID = intent.getStringExtra("emergencyID");
        String description = intent.getStringExtra("description");

        this.requestDescriptionTextView.setText(description);
        this.vehicleIDTextView.setText(vehicleID);
        this.requestID = requestID;
        this.vehicleID = vehicleID;

        Log.e(TAG, vehicleID + ", " + requestID + ", " + description);
    }

    public void setResolved(View view) {
        database = FirebaseDatabase.getInstance();
        database.getReference("Emergency_Alerts").child(this.requestID).child("Status").setValue("Closed");

        // TODO: Should we have something to change it to not resolved if it's resolved?

        Intent intent = new Intent(this, ManagerViewEmergenciesActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}
