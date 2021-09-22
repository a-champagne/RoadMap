package ca.ubc.zachrivard.self.test.roadmap;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ca.ubc.zachrivard.self.test.roadmap.api.Trip;
import ca.ubc.zachrivard.self.test.roadmap.bluetooth.BluetoothService;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        BluetoothService.BluetoothObserver{

    private static final String TAG = MapsActivity.class.getName();

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private static final int REQUEST_ENABLE_BT = 391;

    private static final int BT_LOCATION_DELAY_MS = 5000;

    public static Trip selectedTrip = null;


    /*
     * listener for bluetooth event
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case REQUEST_ENABLE_BT: {

                if(resultCode == Activity.RESULT_OK){
                    mApplication.getBluetoothService().beginService();
                }else{
                    Toast.makeText(this, "Bluetooth needs to be enabled. Please enable Bluetooth.", Toast.LENGTH_SHORT).show();
                    startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);
                }
            }
        }
    }

    private GoogleMap mMap;
    private ArrayList<LatLng> MarkerPoints;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation = new Location("");
    private Marker mCurrLocationMarker;
    private LocationRequest mLocationRequest;
    private Polygon Zone;
    private ArrayList<LatLng> zonePoints;
    private final double zoneDeviation = 0.01;

    private App mApplication;
    private StatusDialog mPopup;
    private boolean autoSnapMap = true;
    private ImageButton messageButton;
    private ImageButton loginButton;
    private ImageButton emergencyButton;
    private ImageButton stopButton;
    private ImageButton maintenanceButton;

    public static void setTrip(Trip trip){
        selectedTrip = trip;
    }
    public static Trip getTrip(){return selectedTrip; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mApplication = (App) getApplication();
        BluetoothService.registerObserver(this);



        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }
        // Initializing
        MarkerPoints = new ArrayList<>();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);



        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if(adapter == null){
            Toast.makeText(this,
                    "Bluetooth not supported on this device!", Toast.LENGTH_SHORT).show();
            return;
        }




        if(adapter.isEnabled()) {
            mApplication.getBluetoothService().beginService();
        } else {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);
        }

        //TODO: set these listeners appropriately
        messageButton = findViewById(R.id.message);
        messageButton.setOnClickListener(view->{
            Intent intent = new Intent(this, DriverViewMessageThread.class);
            startActivity(intent);
            /* driver messages
            Intent intent = new Intent(this, )
            startActivity(intent);
            */

            //TODO: Cormac, add in activity call here
            runOnUiThread(()-> {
                Toast.makeText(this, "Going to messages...", Toast.LENGTH_SHORT).show();
            });
        });

        loginButton = findViewById(R.id.login);
        loginButton.setOnClickListener(view->{
            runOnUiThread(()-> {
                Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();
            });

            returnToLogin(750);

        });

        emergencyButton = findViewById(R.id.emergency);
        emergencyButton.setOnClickListener(view->{
            Intent intent = new Intent(this, EmergencyResourceActivity.class);
            startActivity(intent);
        });

        stopButton = findViewById(R.id.stop);
        stopButton.setOnClickListener(view->{
            stopTrip();
        });

        maintenanceButton = findViewById(R.id.maintenance);
        maintenanceButton.setOnClickListener(view->{
            Intent intent = new Intent(this, MaintenanceRequestActivity.class);
            startActivity(intent);
        });

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(false);
            }
        }
        else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(false);
        }

        // Setting onclick event listener for the map
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng point) {

              }
        });

        mMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
            @Override
            public void onCameraMoveStarted(int i) {
                if(i == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE){
                    autoSnapMap = false;
                }
            }
        });

    }

    private String getUrl(LatLng origin, LatLng dest) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;


        // Sensor enabled
        String sensor = "sensor=false";

        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor + "&mode=driving&key=AIzaSyBPYti_CalEH69wn_RX5nLrV6MwV7KOsR4";
        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;


        return url;
    }

    /**
     * A method to download json data from url
     */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();
            Log.d("downloadUrl", data);
            br.close();

        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    // set the current route and plot it, from origin to dest
    private void setRoute(LatLng origin, LatLng dest) {
        MarkerPoints.add(origin);
        MarkerPoints.add(dest);
        MarkerOptions optionsOrigin = new MarkerOptions();
        MarkerOptions optionsDest = new MarkerOptions();

        // Setting the position of the marker
        optionsOrigin.position(origin);
        optionsDest.position(dest);
        optionsOrigin.icon(BitmapDescriptorFactory.fromResource(R.drawable.start_flag));
        optionsDest.icon(BitmapDescriptorFactory.fromResource(R.drawable.end_flag));

        mMap.addMarker(optionsOrigin);
        mMap.addMarker(optionsDest);

        String url = getUrl(origin, dest);
        Log.d("onMapClick", url);
        FetchUrl FetchUrl = new FetchUrl();
        FetchUrl.execute(url);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(origin));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
    }

    // plot a location marker at our current location
    private void setCurrentLocation(LatLng loc) {
        mLastLocation.setLatitude(loc.latitude);
        mLastLocation.setLongitude(loc.longitude);
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(loc);
        markerOptions.title("Current Position");
        if (inZone(loc))
            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.truck_inzone));
        else
            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.truck_outzone));


        mCurrLocationMarker = mMap.addMarker(markerOptions);

        if(autoSnapMap) {
            mMap.moveCamera(CameraUpdateFactory.newLatLng(loc));
            //move map camera
            mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
        }

        //stop location updates
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    // draws the approved driving zone based on a route
    private void drawZone(ArrayList<LatLng> points) {
        if (Zone != null) Zone.remove();
        LatLng lowest = points.get(0);
        LatLng highest = points.get(0);
        LatLng leftmost = points.get(0);
        LatLng rightmost = points.get(0);
        for (LatLng p : points) {
            if (p.latitude < lowest.latitude)
                lowest = p;
            if (p.latitude > highest.latitude)
                highest = p;
            if (p.longitude < leftmost.longitude)
                leftmost = p;
            if (p.longitude > rightmost.longitude)
                rightmost = p;
        }
        LatLng topLeft = new LatLng(highest.latitude + zoneDeviation, leftmost.longitude - zoneDeviation);
        LatLng bottomLeft = new LatLng(lowest.latitude - zoneDeviation, leftmost.longitude - zoneDeviation);
        LatLng topRight = new LatLng(highest.latitude + zoneDeviation, rightmost.longitude + zoneDeviation);
        LatLng bottomRight = new LatLng(lowest.latitude - zoneDeviation, rightmost.longitude + zoneDeviation);

        zonePoints = new ArrayList<>();
        zonePoints.add(topLeft);
        zonePoints.add(topRight);
        zonePoints.add(bottomRight);
        zonePoints.add(bottomLeft);
        Polygon p = mMap.addPolygon(new PolygonOptions().clickable(false).addAll(zonePoints));
        p.setFillColor(Color.argb(50,20, 255, 30));
        p.setStrokeColor(Color.argb(90, 50, 255, 60));
        Zone = p;

        if(mCurrLocationMarker == null){
            return;
        }
        if (inZone(mCurrLocationMarker.getPosition())) {
            setCurrentLocation(mCurrLocationMarker.getPosition());
        }
    }

    // returns true if loc is inside current zone or if no zone exists, else returns false
    private boolean inZone(LatLng l) {
        if (Zone == null) Log.d("zone is null", "zone is null");
        return Zone != null && (l.latitude < zonePoints.get(0).latitude && l.latitude > zonePoints.get(2).latitude &&
                l.longitude > zonePoints.get(0).longitude && l.longitude < zonePoints.get(1).longitude);
    }

    // Fetches data from url passed
    private class FetchUrl extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try {
                // Fetching the data from web service
                data = downloadUrl(url[0]);
                Log.d("Background Task data", data.toString());
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);

        }
    }

    /**
     * A class to parse the Google Places in JSON format
     */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                Log.d("ParserTask",jsonData[0]);
                DataParser parser = new DataParser();
                Log.d("ParserTask", parser.toString());

                // Starts parsing data
                routes = parser.parse(jObject);
                Log.d("ParserTask","Executing routes");
                Log.d("ParserTask",routes.toString());

            } catch (Exception e) {
                Log.d("ParserTask",e.toString());
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;

            // Traversing through all the routes
            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(10);
                lineOptions.color(Color.RED);

                Log.d("onPostExecute","onPostExecute lineoptions decoded");

            }

            // Drawing polyline in the Google Map for the i-th route
            if(lineOptions != null) {

                mMap.addPolyline(lineOptions);
                drawZone(points);
            }
            else {
                Log.d("onPostExecute","without Polylines drawn");
            }
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {


    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    public boolean checkLocationPermission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted. Do the
                    // contacts-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }

                } else {

                    // Permission denied, Disable the functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other permissions this app might request.
            // You can add here other case statements according to your requirement.
        }
    }


    @Override
    public void onBluetoothEvent(String event, JSONObject payload) {
        runOnUiThread(() -> {
            switch (event) {

                case BluetoothService.BluetoothObserver.REQUEST_BT_CONNECTED: {

                    Log.e(TAG, "Got a response upon connection!");

                    if(mPopup != null)
                        mPopup.dismiss();

                    try {
                        Log.e(TAG, payload.toString(4));

                        selectDriverTrip(payload.getJSONArray("TRIPS"));


                    }catch (JSONException e ){}

                    break;
                }

                case BluetoothService.BluetoothObserver.EVENT_LOCATION_UPDATE: {

                    try {

                        double latitude = payload.getDouble(BluetoothService.BluetoothObserver.KEY_LATITUDE);
                        double longitude = payload.getDouble(BluetoothService.BluetoothObserver.KEY_LONGITUDE);
                        double speed = payload.getDouble(BluetoothService.BluetoothObserver.KEY_SPEED);

                        String EW =  payload.getString(BluetoothService.BluetoothObserver.KEY_EW);
                        String NS =  payload.getString(BluetoothService.BluetoothObserver.KEY_NS);


                        Log.e(TAG, "Location: " + latitude + "," + longitude);

                        LatLng location = new LatLng(latitude, longitude);

                        Log.e(TAG, "New Location: " + location.toString());
                        setCurrentLocation(location);


                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing BT event!");
                    }

                    break;
                }

                case BluetoothService.BluetoothObserver.EVENT_DISCOVERY_BEGINNING: {

                    Log.e(TAG, BluetoothService.BluetoothObserver.EVENT_DISCOVERY_BEGINNING);
                    //Toast.makeText(this, )
                    mPopup = new StatusDialog(this,"Looking for known vehicles...");
                    mPopup.setCancelable(false);
                    mPopup.show();

                    break;
                }

                case BluetoothService.BluetoothObserver.EVENT_CONNECTION_ESTABLISHED: {

                    Log.e(TAG, BluetoothService.BluetoothObserver.EVENT_CONNECTION_ESTABLISHED);

                    mPopup.setHeaderMessage("Gathering trips...");

                    break;
                }

                case BluetoothService.BluetoothObserver.REQUEST_BT_STOP_TRIP: {

                    Toast.makeText(this, "Trip Terminated. Exiting Map View", Toast.LENGTH_LONG).show();
                    returnToLogin(750);

                }

            }
        });
    }

    public void bluetoothChooseTrip(){

        JSONObject json = new JSONObject();
        JSONObject data = new JSONObject();

        assert (selectedTrip != null);

        setRoute(selectedTrip.getOrigin().getLatLng(), selectedTrip.getDestination().getLatLng());

        try {
            json.put(BluetoothService.BluetoothObserver.KEY_REQUEST_TYPE,
                    BluetoothService.BluetoothObserver.REQUEST_BT_CHOOSE_TRIP);

            data.put(BluetoothService.BluetoothObserver.KEY_TRIP_ID,
                    selectedTrip.getTripID());
            data.put(BluetoothService.BluetoothObserver.KEY_DRIVER_ID,
                    selectedTrip.getDriverID());
            data.put(BluetoothService.BluetoothObserver.KEY_TIME_STAMP,
                    System.currentTimeMillis() / 1000L);

            json.put(BluetoothService.BluetoothObserver.KEY_DATA, data);
            Log.e("Selected Trip:\n", json.toString(4));
            mApplication.getBluetoothService().issueCommand(json.toString());



        }catch (JSONException e) {}
    }

    private void bluetoothRequestLocation(){

        JSONObject json = new JSONObject();
        JSONObject data = new JSONObject();

        try {
            json.put(BluetoothService.BluetoothObserver.KEY_REQUEST_TYPE,
                    BluetoothService.BluetoothObserver.REQUEST_BT_LOCATIONDATA);

            json.put(BluetoothService.BluetoothObserver.KEY_DATA, data);
            Log.e("JSON", json.toString(4));
            mApplication.getBluetoothService().issueCommand(json.toString());

        }catch (JSONException e) {

        }

    }


    private void selectDriverTrip(JSONArray trips){

        ArrayList<Trip> vehicleTrips = new ArrayList<>();
        for(int i = 0; i < trips.length(); i++){
            try {

                JSONObject jsonTrip = trips.getJSONObject(i);
                if(jsonTrip == null){
                    continue;
                }
                vehicleTrips.add(new Trip(jsonTrip, String.valueOf(jsonTrip.getString("id"))));
            }catch (JSONException e){

            }
        }

        SelectTripDialog dialog = new SelectTripDialog(this, vehicleTrips);
        dialog.show();

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {

                dialog.dismiss();

                if(selectedTrip != null){
                    bluetoothChooseTrip();

                    Handler handler = new Handler();
                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {

                            bluetoothRequestLocation();
                            handler.postDelayed(this,BT_LOCATION_DELAY_MS );
                        }
                    };
                    runnable.run();
//                    startActivity(new Intent(getApplicationContext(), EmergencyResourceActivity.class));
                }
            }
        });

    }

    private void stopTrip(){

        JSONObject json = new JSONObject();
        JSONObject data = new JSONObject();

        try {
            json.put(BluetoothService.BluetoothObserver.KEY_REQUEST_TYPE,
                    BluetoothService.BluetoothObserver.REQUEST_BT_STOP_TRIP);

            data.put(BluetoothService.BluetoothObserver.KEY_TRIP_ID,
                    selectedTrip.getTripID());
            data.put(BluetoothService.BluetoothObserver.KEY_TIME_STAMP,
                    System.currentTimeMillis() / 1000L);


            json.put(BluetoothService.BluetoothObserver.KEY_DATA, data);
            Log.e("JSON", json.toString(4));
            mApplication.getBluetoothService().issueCommand(json.toString());

        }catch (JSONException e) {

        }

    }

    private void returnToLogin(int delayMS){
        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                FirebaseAuth.getInstance().signOut();
                finish();
                startActivity(new Intent(MapsActivity.this, LoginActivity.class));
            }
        };

        handler.postDelayed(runnable, 750);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mApplication.getBluetoothService().closeConnection();
        if(mPopup != null) {
            mPopup.dismiss();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if(mPopup != null) {
            mPopup.dismiss();
        }
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        super.dump(prefix, fd, writer, args);
    }
}