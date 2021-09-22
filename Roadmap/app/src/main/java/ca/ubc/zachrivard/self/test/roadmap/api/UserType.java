package ca.ubc.zachrivard.self.test.roadmap.api;

import com.google.gson.annotations.SerializedName;

public class UserType {

    @SerializedName("user_id")
    private String userId;

    @SerializedName("user_type")
    private String userType;

    public String getUserType() {
        return userType;
    }

    public String getUserId() {
        return userId;
    }

    public UserType(String userId, String userType) {
        this.userId = userId;
        this.userType = userType;
    }
}
