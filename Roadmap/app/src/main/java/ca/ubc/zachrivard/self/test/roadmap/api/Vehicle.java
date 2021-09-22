package ca.ubc.zachrivard.self.test.roadmap.api;

import android.support.annotation.Nullable;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class Vehicle {

    @SerializedName("Location")
    @Expose
    private Coordinate location;

    @SerializedName("Location_Log")
    @Expose
    @Nullable
    private Map<Long, Coordinate> locationLog;

    @SerializedName("DriverID")
    @Expose
    private String driverId;

    @SerializedName("TripID")
    private String tripId;

    @SerializedName("VehicleID")
    @Expose
    private String vehicleId;

    @SerializedName("Available")
    @Expose
    private Boolean available;

    @SerializedName("Status")
    @Expose
    private String status;



    private Vehicle() {}

    public static class Builder {

        private Vehicle vehicle = new Vehicle();

        public Builder(String vehicleID, Coordinate location) {
            vehicle.vehicleId = vehicleID;
            vehicle.location = location;
        }
        public Vehicle build() {
            return vehicle;
        }
    }


    public static class Coordinate {

        @SerializedName("latitude")
        @Expose
        private double latitude;

        @SerializedName("longitude")
        @Expose
        private double longitude;

        @SerializedName("speed")
        @Expose
        private double speed;


        public Coordinate(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public double getLatitude() {
            return latitude;
        }
        public double getLongitude() {
            return longitude;
        }
        public double getSpeed() {
            return speed;
        }
    }

    public Coordinate getLocation() {
        return location;
    }

    @Nullable
    public Map<Long, Coordinate> getLocationLog() {
        return locationLog;
    }

    public String getDriverId() {
        return driverId;
    }

    public String getTripId() {
        return tripId;
    }

    public Boolean getAvailable() {
        return available;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "Vehicle{" +
                "location=" + location +
                ", locationLog=" + locationLog +
                ", driverId='" + driverId + '\'' +
                ", tripId='" + tripId + '\'' +
                ", available=" + available +
                ", status='" + status + '\'' +
                '}';
    }
}
