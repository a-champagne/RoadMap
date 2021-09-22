package ca.ubc.zachrivard.self.test.roadmap;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.text.TextWatcher;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;

import ca.ubc.zachrivard.self.test.roadmap.bluetooth.BluetoothService;

public class DriverViewMessageThread extends Activity implements BluetoothService.BluetoothObserver {
    private RecyclerView mMessageRecycler;
    private MessageListAdapter mMessageAdapter;
    private App mApplication;

    private String userType;
    private List<Message> messageList;
    private HashMap<Long, Message> messageMap;

    EditText editText;
    Button sendButton;

    private static final String TAG = ManagerViewMessageThread.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_message_thread);

        mApplication = (App) getApplication();
        this.messageList = new LinkedList<>();
        this.userType = mApplication.getUserType();

        editText = (EditText) findViewById(R.id.edittext_chatbox);
        sendButton = (Button) findViewById(R.id.button_chatbox_send);
        editText.addTextChangedListener(mTextWatcher);


        mMessageRecycler = (RecyclerView) findViewById(R.id.reyclerview_message_list);

        mMessageAdapter = new MessageListAdapter(this, this.messageList, "driver");

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        mMessageRecycler.setLayoutManager(layoutManager);
        mMessageRecycler.setAdapter(mMessageAdapter);

        BluetoothService.registerObserver(this);
        mApplication.getBluetoothService().beginService();

        this.messageMap = new HashMap<>();
        getMessagesFromBluetooth();
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


    private void getMessagesFromBluetooth() {
        //These will have to be put into the given activities to send requests
        JSONObject json = new JSONObject();

        try {
            // BT_GET_MESSAGES
            json.put(BluetoothService.BluetoothObserver.KEY_REQUEST_TYPE,
                    BluetoothService.BluetoothObserver.REQUEST_BT_GET_MESSAGES);

            Log.e("JSON", json.toString(4));
            mApplication.getBluetoothService().issueCommand(json.toString());

        }catch (JSONException e){

        }
    }

    private void displayMessages(List<Message> newMessageList) {
        this.messageList.clear();

        for(Message message : newMessageList) {
            this.messageList.add(message);
        }

        Log.e(TAG, "ANUS");
        Log.e(TAG,Integer.toString(this.messageList.size()));

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mMessageRecycler = (RecyclerView) findViewById(R.id.reyclerview_message_list);
                mMessageAdapter.notifyDataSetChanged();
                if(mMessageAdapter.getItemCount() > 0) {
                    mMessageRecycler.smoothScrollToPosition(mMessageAdapter.getItemCount() - 1);
                }
            }
        });

        Log.e(TAG, Integer.toString(mMessageAdapter.getItemCount()));
    }

    public void sendMessage(View view) {
        sendDriverMessage();
        editText.getText().clear();
    }

    private void sendDriverMessage() {
        JSONObject json = new JSONObject();
        JSONObject data = new JSONObject();
        String message_data = editText.getText().toString();

        try {
            // BT_GET_MESSAGES
            json.put(BluetoothService.BluetoothObserver.KEY_REQUEST_TYPE,
                    BluetoothService.BluetoothObserver.REQUEST_BT_SEND_MESSAGE);

            data.put(BluetoothService.BluetoothObserver.KEY_MESSAGE_DATA,
                    message_data);

            data.put(BluetoothService.BluetoothObserver.KEY_FROM_USER_TYPE,
                    this.userType);

            data.put(BluetoothService.BluetoothObserver.KEY_TIME_STAMP,
                    System.currentTimeMillis() / 1000);

            json.put(BluetoothService.BluetoothObserver.KEY_DATA, data);

            Log.e("JSON", json.toString(4));

            mApplication.getBluetoothService().issueCommand(json.toString());

        }catch (JSONException e){

        }
    }

    public void onBluetoothEvent(String event, JSONObject payload) {
        runOnUiThread(() -> {
            switch (event) {

                case BluetoothService.BluetoothObserver.EVENT_SINGLE_MESSAGE: {
                    Log.e(TAG, "Got a response");

                    try {

                        JSONObject json = new JSONObject(payload.toString());
                        Log.e(TAG, "HERE");

                        JSONObject messageJSON = new JSONObject(json.get("DATA").toString());

                        Long timestamp = Long.parseLong(messageJSON.get("TimeStamp").toString());

                        if(!messageMap.containsKey(timestamp)) {
                            List<Message> newMessageList = new LinkedList<>(this.messageList);
                            String message_data = messageJSON.get("Message_Data").toString();
                            String userType = messageJSON.get("fromUserType").toString();

                            Message newMessage = new Message(message_data, userType, timestamp);
                            newMessageList.add(newMessage);
                            messageMap.put(timestamp, newMessage);

                            displayMessages(newMessageList);
                        }
                    }
                    catch (JSONException e ) {
                        e.printStackTrace();
                    }

                    break;
                }

                case BluetoothService.BluetoothObserver.EVENT_MESSAGE: {

                    Log.e(TAG, "Got a response");

                    try {
                        JSONObject json = new JSONObject(payload.toString());
                        List<Message> messageList = new LinkedList<>();
                        JSONArray jsonMessages = json.getJSONArray("MESSAGES");

                        Log.e(TAG, jsonMessages.toString());

                        for(int i = 0; i < jsonMessages.length(); i++){
                            JSONObject message = new JSONObject(jsonMessages.getJSONObject(i).getString("value"));
                            String message_data = message.get("Message_Data").toString();
                            Long timestamp = Long.parseLong(message.get("TimeStamp").toString());
                            String fromUserType = message.get("fromUserType").toString();

                            Message newMessage = new Message(message_data, fromUserType, timestamp);
                            messageMap.put(timestamp, newMessage);
                            messageList.add(newMessage);
                        }

                        displayMessages(messageList);
                    }catch (JSONException e ){}

                    break;
                }
            }
        });
    }
}
