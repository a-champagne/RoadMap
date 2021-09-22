package ca.ubc.zachrivard.self.test.roadmap;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import ca.ubc.zachrivard.self.test.roadmap.api.Trip;

public class SelectTripDialog extends Dialog {

    ArrayList<Trip> trips;
    Context context;

    public SelectTripDialog(Activity activity, ArrayList<Trip> trips){
        super(activity);

        this.trips = trips;
        context = activity.getApplicationContext();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.driver_select_trip);


        ListView tripsList = (ListView) findViewById(R.id.tripsList);

        Trip tripsArray[] = new Trip[trips.size()];
        tripsArray = trips.toArray(tripsArray);
        SelectTripAdapter adapter = new SelectTripAdapter(context, R.layout.driver_trip_row, tripsArray);

        tripsList.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        TextView noTrips = findViewById(R.id.noTripsText);

        if(tripsArray.length  == 0){
            noTrips.setVisibility(View.VISIBLE);
            this.setCancelable(true);
        }else {
            noTrips.setVisibility(View.GONE);
            this.setCancelable(false);
        }


        Trip[] finalTripsArray = tripsArray;
        tripsList.setOnItemClickListener((adapterView, view, i, l) -> {
            Log.e("Adapter", "I got clicked! " + String.valueOf(finalTripsArray[i].getTripID()));
            this.dismiss();
            MapsActivity.setTrip(finalTripsArray[i]);
        });




    }
}
