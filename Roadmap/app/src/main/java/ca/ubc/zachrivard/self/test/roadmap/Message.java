package ca.ubc.zachrivard.self.test.roadmap;

public class Message {
    public String Message_Data, fromUserType;
    public long TimeStamp;

    public Message() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public Message(String message_data, String fromUserType, long timestamp) {
        this.Message_Data = message_data;
        this.fromUserType = fromUserType;
        this.TimeStamp = timestamp;
    }
}
