package ca.ubc.zachrivard.self.test.roadmap;

import java.util.List;

import android.support.v7.widget.RecyclerView;
import android.content.Context;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.TextView;
import android.view.View;
import android.view.LayoutInflater;

public class MessageListAdapter extends RecyclerView.Adapter {
    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;

    private Context mContext;
    private List<Message> mMessageList;
    private String userType;

    private static final String TAG = ManagerMessages.class.getName();

    public MessageListAdapter(Context context, List<Message> messageList, String userType) {
        mContext = context;
        mMessageList = messageList;
        this.userType = userType;
    }

    @Override
    public int getItemCount() {
        Log.e(TAG, "Get Item Count");
        return mMessageList.size() ;
    }

    public void updateMessagesList(List<Message> newMessages) {
        Log.e(TAG, "Update messages list");
        mMessageList.clear();
        mMessageList.addAll(newMessages);
        notifyDataSetChanged();
    }

    // Determines the appropriate ViewType according to the sender of the message.
    @Override
    public int getItemViewType(int position) {
        Log.e(TAG, "get item view type");
        Message message = (Message) mMessageList.get(position);
        String fromUserType = message.fromUserType;
        Log.e(TAG, "BIND");

        if ((fromUserType.equals("driver") && this.userType.equals("driver")) ||
             fromUserType.equals("manager") && this.userType.equals("manager")) {
            // If the current user is the sender of the message
            return VIEW_TYPE_MESSAGE_SENT;
        }
        else {
            // If some other user sent the message
            return VIEW_TYPE_MESSAGE_RECEIVED;
        }
    }

    // Inflates the appropriate layout according to the ViewType.
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        Log.e(TAG, Integer.toString(viewType));
        Log.e(TAG, "on create view holder");

        if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageHolder(view);
        } else if (viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageHolder(view);
        }

        return null;
    }

    // Passes the message object to a ViewHolder so that the contents can be bound to UI.
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Log.e(TAG, "on bind view holder");
        Message message = (Message) mMessageList.get(position);
        switch (holder.getItemViewType()) {
            case VIEW_TYPE_MESSAGE_SENT:
                ((SentMessageHolder) holder).bind(message);
                break;
            case VIEW_TYPE_MESSAGE_RECEIVED:
                ((ReceivedMessageHolder) holder).bind(message);
        }
    }

    public static class SentMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText;

        SentMessageHolder(View itemView) {
            super(itemView);
            Log.e(TAG, "sent message constructor");
            messageText = (TextView) itemView.findViewById(R.id.text_message_body);
        }

        void bind(Message message) {
            Log.e(TAG, "sent message binder");
            messageText.setText(message.Message_Data);
        }
    }

    public static class ReceivedMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText, nameText;

        ReceivedMessageHolder(View itemView) {
            super(itemView);
            Log.e(TAG, "received message constructor");
            messageText = (TextView) itemView.findViewById(R.id.text_message_body);
        }

        void bind(Message message) {
            messageText.setText(message.Message_Data);
        }
    }
}
