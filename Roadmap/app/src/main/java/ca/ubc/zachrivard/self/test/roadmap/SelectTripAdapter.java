package ca.ubc.zachrivard.self.test.roadmap;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import ca.ubc.zachrivard.self.test.roadmap.api.Trip;

public class SelectTripAdapter extends ArrayAdapter<Trip> {


    Context mContext;
    int mLayoutResId;
    Trip[] mData;

    public SelectTripAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull Trip[] objects) {
        super(context, resource, objects);
        mContext = context;
        mLayoutResId = resource;
        mData = objects;
    }

    @Override
    public int getCount() {
        return mData.length;
    }

    @Override
    public Trip getItem(int i) {
        return super.getItem(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View row, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        row = inflater.inflate(mLayoutResId, parent,false);


        Trip trip = mData[position];

        TextView title = (TextView) row.findViewById(R.id.tripTitle);
        title.setText("Trip " + String.valueOf(mData[position].getTripID()));

        TextView origin = (TextView) row.findViewById(R.id.originTitle);
        origin.setText("Origin: " + trip.getOrigin().getAddress());

        TextView destination = (TextView) row.findViewById(R.id.destinationTitle);
        destination.setText("Destination: " + trip.getDestination().getAddress());

        row.setBackground(mContext.getDrawable(R.drawable.list_item_border));

        return row;
    }
}
