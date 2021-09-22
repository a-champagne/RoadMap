package ca.ubc.zachrivard.self.test.roadmap;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.text.TextWatcher;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;

public class ManagerViewMessageThread extends Activity {
    private RecyclerView mMessageRecycler;
    private MessageListAdapter mMessageAdapter;
    private App mApplication;

    private String tripID;

    public static List<Message> messageList;
    private HashMap<Long, Message> messageMap;
    private String userType;

    private FirebaseDatabase database;
    DatabaseReference myRef;

    EditText editText;
    Button sendButton;

    private static final String TAG = ManagerViewMessageThread.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_message_thread);

        mApplication = (App) getApplication();
        this.messageList = new LinkedList<>();

        Intent intent = getIntent();
        String userType = "manager";

        editText = (EditText) findViewById(R.id.edittext_chatbox);
        sendButton = (Button) findViewById(R.id.button_chatbox_send);
        editText.addTextChangedListener(mTextWatcher);


        mMessageRecycler = (RecyclerView) findViewById(R.id.reyclerview_message_list);

        mMessageAdapter = new MessageListAdapter(this, this.messageList, "manager");

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        mMessageRecycler.setLayoutManager(layoutManager);
        mMessageRecycler.setAdapter(mMessageAdapter);



        this.tripID = intent.getStringExtra("tripID");
        getMessagesFromDatabase();


    }

    private void getMessagesFromDatabase() {
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("Trips").child(this.tripID).child("Messages");

        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Message> newMessageList = new LinkedList<>();
                for(DataSnapshot message : dataSnapshot.getChildren()) {
                    if(message.getKey() == null || message.getKey().equals("next_id")) continue;

                    Object message_data = message.child("Message_Data").getValue();
                    Object fromUserType = message.child("fromUserType").getValue();
                    Object timestamp = message.child("timestamp").getValue();

                    Message newMessage = new Message(
                            message_data == null ? "" : message_data.toString(),
                            fromUserType == null ? "driver" : fromUserType.toString(),
                            timestamp == null ? 0 : Long.parseLong(timestamp.toString())
                    );

                    newMessageList.add(newMessage);
                }

                displayMessages(newMessageList);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }

    private TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            // check Fields For Empty Values
            if(editText.getText().toString().length() > 0) {
                sendButton.setEnabled(true);
            }
            else {
                sendButton.setEnabled(false);
            }
        }
    };

    private void displayMessages(List<Message> newMessageList) {
        this.messageList.clear();

        for(Message message : newMessageList) {
            this.messageList.add(message);

        }


        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mMessageRecycler = (RecyclerView) findViewById(R.id.reyclerview_message_list);

                mMessageAdapter.notifyDataSetChanged();
                if(mMessageAdapter.getItemCount() > 0) {
                    mMessageRecycler.smoothScrollToPosition(mMessageAdapter.getItemCount());
                }
            }
        });

        Log.e(TAG, Integer.toString(mMessageAdapter.getItemCount()));
    }

    public void sendMessage(View view) {


            sendManagerMessage();

        editText.getText().clear();
    }

    private void sendManagerMessage() {
        String message_data = editText.getText().toString();

        myRef = database.getReference("Trips").child(this.tripID).child("Messages");
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Long TimeStamp = System.currentTimeMillis() / 1000;
                Message newMessage = new Message(message_data, "manager", TimeStamp);
                myRef.child(Long.toString(TimeStamp)).setValue(newMessage);
                getMessagesFromDatabase();
              }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }
}
