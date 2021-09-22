package ca.ubc.zachrivard.self.test.roadmap.api;

import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Trip {

    @SerializedName("id")
    @Expose
    private String id;

    @SerializedName("Destination")
    @Expose
    private Coordinate destination;

    @SerializedName("Origin")
    @Expose
    private Coordinate origin;

    @SerializedName("TimeStamp")
    @Expose
    private Long timeStamp;

    @SerializedName("active")
    @Expose
    @Nullable
    private Boolean active;

    @SerializedName("driverID")
    @Expose
    @Nullable
    private String driverID;

    @SerializedName("driverName")
    @Expose
    @Nullable
    private String driverName;

    @SerializedName("speed")
    @Expose
    @Nullable
    private Double speed;

    @SerializedName("vehicleID")
    @Expose
    @Nullable
    private String vehicleID;

    @SerializedName("messages")
    @Expose
    @Nullable
    private Map<String, Message> messages;

    @SerializedName("zone")
    @Expose
    @Nullable
    private Zone tripZone;

    private Trip() {}

    public Trip(JSONObject json, String id){


        this.id = id;

        try{

            JSONObject dest = json.getJSONObject("Destination");
            JSONObject orig = json.getJSONObject("Origin");

            destination = new Coordinate(
                    dest.getString("address"),
                    dest.getDouble("latitude"),
                    dest.getDouble("longitude"));

            origin = new Coordinate(
                    orig.getString("address"),
                    orig.getDouble("latitude"),
                    orig.getDouble("longitude")
            );

            timeStamp = json.getLong("TimeStamp");
            driverID = json.getString("driverID");
            speed = json.getDouble("speed");
            vehicleID = json.getString("vehicleID");


            Log.e("NEW_TRIP", this.toString());
        }catch (JSONException e){
            Log.e("TRIP", "Could not create trip object");
        }

    }

    public static class Message {
        // TODO(uriel): Check w/ team for required fields.

        @SerializedName("senderID")
        @Expose
        private String senderId;

        @SerializedName("text")
        @Expose
        private String text;

        public Message() {}

        public Message(String senderId, String text) {
            this.senderId = senderId;
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public String getSenderId() {
            return senderId;
        }

        @Override
        public String toString() {
            return "Message{" +
                    "senderId='" + senderId + '\'' +
                    ", text='" + text + '\'' +
                    '}';
        }
    }

    public static class Builder {

        private Trip trip = new Trip();

        public Builder() { }

        public Builder(Coordinate origin, Coordinate destination, Long timeStamp) {
            trip.origin = origin;
            trip.destination = destination;
            trip.timeStamp = timeStamp;
        }

        public Builder setOrigin(Coordinate origin) {
            trip.origin = origin;
            return this;
        }

        public Builder setDestination(Coordinate dest) {
            trip.destination = dest;
            return this;
        }

        public Builder setTimeStamp(Long timeStamp) {
            trip.timeStamp = timeStamp;
            return this;
        }

        public Builder setActive(boolean active) {
            trip.active = active;
            return this;
        }

        public Builder setDriverId(String id) {
            trip.driverID = id;
            return this;
        }

        public Builder setDriverName(String name) {
            trip.driverID = name;
            return this;
        }

        public Builder setVehicleId(String id) {
            trip.vehicleID = id;
            return this;
        }

        public Builder setSpeed(double speed) {
            trip.speed = speed;
            return this;
        }

        public Trip build() {
            return trip;
        }
    }

    public static class Coordinate {

        @SerializedName("address")
        @Expose
        private String address;
        @SerializedName("latitude")
        @Expose
        private double latitude;
        @SerializedName("longitude")
        @Expose
        private double longitude;

        public Coordinate(String address, double latitude, double longitude) {
            this.address = address;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public String getAddress() {
            return address;
        }
        public double getLatitude() {
            return latitude;
        }
        public double getLongitude() {
            return longitude;
        }
        public LatLng getLatLng() {return new LatLng(latitude, longitude);}

        @Override
        public String toString() {
            return "Coordinate{" +
                    "address='" + address + '\'' +
                    ", latitude=" + latitude +
                    ", longitude=" + longitude +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Coordinate)) return false;
            Coordinate that = (Coordinate) o;
            return Double.compare(that.latitude, latitude) == 0 &&
                    Double.compare(that.longitude, longitude) == 0 &&
                    Objects.equals(address, that.address);
        }

        @Override
        public int hashCode() {
            return Objects.hash(address, latitude, longitude);
        }
    }

    public static class Zone {

        @SerializedName("bottomLeft")
        @Expose
        private Coordinate bottomLeft;

        @SerializedName("bottomRight")
        @Expose
        private Coordinate bottomRight;

        @SerializedName("topLeft")
        @Expose
        private Coordinate topLeft;

        @SerializedName("topRight")
        @Expose
        private Coordinate topRight;

        public Zone(Coordinate bottomLeft, Coordinate bottomRight, Coordinate topLeft, Coordinate topRight) {
            this.bottomLeft = bottomLeft;
            this.bottomRight = bottomRight;
            this.topLeft = topLeft;
            this.topRight = topRight;
        }
    }

    public String getTripID() {
        return id;
    }

    public Coordinate getDestination() {
        return destination;
    }

    public Coordinate getOrigin() {
        return origin;
    }

    public Long getTimeStamp() {
        return timeStamp;
    }

    @Nullable
    public Boolean getActive() {
        return active;
    }

    @Nullable
    public String getDriverID() {
        return driverID;
    }

    @Nullable
    public String getDriverName() {
        return driverName;
    }

    @Nullable
    public Double getSpeed() {
        return speed;
    }

    @Nullable
    public String getVehicleID() {
        return vehicleID;
    }

    @Nullable
    public Zone getTripZone() {
        return tripZone;
    }

    @Nullable
    public Map<String, Message> getMessages() {
        return messages;
    }

    public void setDestination(Coordinate destination) {
        this.destination = destination;
    }

    public void setOrigin(Coordinate origin) {
        this.origin = origin;
    }

    public void setTimeStamp(Long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public void setActive(@Nullable Boolean active) {
        this.active = active;
    }

    public void setDriverID(@Nullable String driverID) {
        this.driverID = driverID;
    }

    public void setDriverName(@Nullable String driverName) {
        this.driverName = driverName;
    }

    public void setSpeed(@Nullable Double speed) {
        this.speed = speed;
    }

    public void setVehicleID(@Nullable String vehicleID) {
        this.vehicleID = vehicleID;
    }

    @Override
    public String toString() {
        return "Trip{" +
                "id='" + id + '\'' +
                ", destination=" + destination +
                ", origin=" + origin +
                ", timeStamp=" + timeStamp +
                ", active=" + active +
                ", driverID='" + driverID + '\'' +
                ", driverName='" + driverName + '\'' +
                ", speed=" + speed +
                ", vehicleID='" + vehicleID + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Trip)) return false;
        Trip trip = (Trip) o;
        return Objects.equals(id, trip.id) &&
                Objects.equals(destination, trip.destination) &&
                Objects.equals(origin, trip.origin) &&
                Objects.equals(timeStamp, trip.timeStamp) &&
                Objects.equals(active, trip.active) &&
                Objects.equals(driverID, trip.driverID) &&
                Objects.equals(driverName, trip.driverName) &&
                Objects.equals(speed, trip.speed) &&
                Objects.equals(vehicleID, trip.vehicleID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, destination, origin, timeStamp, active, driverID, driverName, speed, vehicleID);
    }
}

