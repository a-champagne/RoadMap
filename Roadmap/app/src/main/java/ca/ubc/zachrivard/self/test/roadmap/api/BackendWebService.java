package ca.ubc.zachrivard.self.test.roadmap.api;

import com.google.gson.JsonObject;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface BackendWebService {

    // TODO(uriel): Move this to a more appropriate location (config file?).
    String API_BASE_URL = "https://us-central1-cpen391-test.cloudfunctions.net/api/";

    // TRIP API ENDPOINTS

    @GET("trip/{id}")
    Call<Trip> getTrip(@Path("id") String id);

    @GET("trip/")
    Call<Map<String, Trip>> getAllTrips();

    @POST("trip")
    Call<Trip> createTrip(@Body Trip trip);

    @PUT("trip/{id}")
    Call<Trip> updateTrip(@Path("id") String id, @Body Trip trip);

    @DELETE("trip/{id}")
    Call<JsonObject> deleteTrip(@Path("id") String id);

    @POST("trip/{id}/message")
    Call<JsonObject> sendTripMessage(@Path("id") String tripId, @Body Trip.Message message);

    @GET("trip/{id}/message/all")
    Call<Map<Long, Trip.Message>> getTripMessages(@Path("id") String tripId);

    @GET("trip/{id}/message/since={since}")
    Call<Map<Long, Trip.Message>> getTripMessages(@Path("id") String tripId, @Path("since") Long since);

    @POST("trip/{id}/zone/")
    Call<Trip.Zone> setTripZone(@Path("id") String tripId, @Body Trip.Zone tripZone);

    // VEHICLE API ENDPOINTS

    @GET("vehicle/{id}")
    Call<Vehicle> getVehicle(@Path("id") String id);

    @GET("vehicle/")
    Call<Map<String, Vehicle>> getAllVehicles();

    @POST("vehicle")
    Call<Vehicle> createVehicle(@Body Vehicle vehicle);

    @PUT("vehicle/{id}")
    Call<Vehicle> updateVehicle(@Path("id") String id, @Body Vehicle vehicle);

    @DELETE("vehicle/{id}")
    Call<JsonObject> deleteVehicle(@Path("id") String id);

    // USER ENDPOINT
    @GET("user_type/{id}")
    Call<UserType> getUserType(@Path("id") String userId);

    // STATIC MAPS API

    @GET("https://maps.googleapis.com/maps/api/directions/json?&mode=driving")
    Call<JsonObject> getTripMetadata(
            @Query("origin") String origin,
            @Query("destination") String destination,
            @Query("key") String apiKey
    );
    
    /** Example usage:
     *
        Trip.Coordinate origin = new Trip.Coordinate("<origin>", 0, 1);
        Trip.Coordinate destination = new Trip.Coordinate("<dest>", 2, 3);

        Trip trip = new Trip.Builder(origin, destination, System.currentTimeMillis())
                .setDriverId("<driverid>")
                .setDriverName("Joe")
                .build();

        mApplication.getBackendWebService().createTrip(trip).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                Log.d(TAG, "Response: " + response.toString());
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.d(TAG, "Failure: " + t.getMessage());
            }
        });

     */

    /**
     * mApplication.getBackendWebService().createTrip(trip).enqueue(new Callback<Trip>() {
     *             @Override
     *             public void onResponse(Call<Trip> call, Response<Trip> r1) {
     *                 Log.d(TAG, "CREATE: " + r1.body());
     *                 tripID = r1.body().getTripID();
     * //                Intent intent = new Intent(getApplicationContext(), ManagerActivity.class);
     * //                startActivity(intent.putExtra("tripID", tripID).putExtra("tripCreated", true));
     *
     *                 mApplication.getBackendWebService().getTrip(tripID).enqueue(new Callback<Trip>() {
     *                     @Override
     *                     public void onResponse(Call<Trip> call, Response<Trip> r2) {
     *                         Log.d(TAG, "Equals: " + r2.body().equals(r1.body()));
     *                         Log.d(TAG, "GET: " + r2.body());
     *                         Trip trip = r2.body();
     *                         trip.setVehicleID("TEST_UPDATE");
     *                         mApplication.getBackendWebService().updateTrip(tripID, trip).enqueue(new Callback<Trip>() {
     *                             @Override
     *                             public void onResponse(Call<Trip> call, Response<Trip> r3) {
     *                                 Log.d(TAG, "UPDATE: " + r3.body());
     *                             }
     *
     *                             @Override
     *                             public void onFailure(Call<Trip> call, Throwable t) {
     *                                 Log.d(TAG, "UPDATE FAIL: " + t.getMessage());
     *                             }
     *                         });
     *                     }
     *
     *                     @Override
     *                     public void onFailure(Call<Trip> call, Throwable t) {
     *                         Log.d(TAG, "GET: " + t.getMessage());
     *                     }
     *                 });
     *
     *                 ((App)getApplication()).getBackendWebService()
     *                 .setTripZone("25", new Trip.Zone(
     *                         new Trip.Coordinate(null, 1, 2),
     *                         new Trip.Coordinate(null, 3, 4),
     *                         new Trip.Coordinate(null, 5, 6),
     *                         new Trip.Coordinate(null, 7, 8)
     *                 ))
     *                 .enqueue(new Callback<JsonObject>() {
     *                     @Override
     *                     public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
     *                         Log.d(TAG, response + " : " + response.body());
     *                     }
     *
     *                     @Override
     *                     public void onFailure(Call<JsonObject> call, Throwable t) {
     *                         Log.d(TAG, "fail: " + t.getLocalizedMessage());
     *                     }
     *                 });
     */
}
