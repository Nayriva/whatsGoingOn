package ee.ut.madp.whatsgoingon.fragments;


import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;

import java.text.Normalizer;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import ee.ut.madp.whatsgoingon.Observable;
import ee.ut.madp.whatsgoingon.Observer;
import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.chat.ChatApplication;
import ee.ut.madp.whatsgoingon.chat.ChatMessageAdapter;
import ee.ut.madp.whatsgoingon.models.ChatMessage;

public class ChatFragment extends Fragment implements Observer {

    private static final String TAG = "aa"; //TODO
    private ChatApplication application;
    private ListView messagesListView;
    private ArrayAdapter<ChatMessage> adapter;
    private EditText messageET;
    private String channelToJoin;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        application = (ChatApplication) getActivity().getApplication();
        application.addObserver(this);
    }

    public void setData(String data) {
        channelToJoin = data;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        messagesListView = (ListView) view.findViewById(R.id.messagesListView);
        adapter = new ChatMessageAdapter(getContext(), R.layout.message_list_item, application.getHistory());
        messagesListView.setAdapter(adapter);

        messageET = (EditText) view.findViewById(R.id.messageEditText);
        FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.sendFloatingActionButton);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });

        joinChannel();
        updateHistory();

        return view;
    }

    private void joinChannel() {
        application.useSetChannelName(channelToJoin);
        application.useJoinChannel();
    }

    private void updateHistory() {
        Log.i(TAG, "updateHistory()");
        adapter.clear();
        List<ChatMessage> messages = application.getHistory();
        for (ChatMessage message : messages) {
            adapter.add(message);
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public synchronized void update(Observable o, Object arg) {
        Log.i(TAG, "update(" + arg + ")");
        String qualifier = (String)arg;

        if (qualifier.equals(ChatApplication.APPLICATION_QUIT_EVENT)) {
            Message message = handler.obtainMessage(HANDLE_APPLICATION_QUIT_EVENT);
            handler.sendMessage(message);
        }

        if (qualifier.equals(ChatApplication.HISTORY_CHANGED_EVENT)) {
            Message message = handler.obtainMessage(HANDLE_HISTORY_CHANGED_EVENT);
            handler.sendMessage(message);
        }

        if (qualifier.equals(ChatApplication.USE_CHANNEL_STATE_CHANGED_EVENT)) {
            Message message = handler.obtainMessage(HANDLE_CHANNEL_STATE_CHANGED_EVENT);
            handler.sendMessage(message);
        }

        if (qualifier.equals(ChatApplication.ALLJOYN_ERROR_EVENT)) {
            Message message = handler.obtainMessage(HANDLE_ALLJOYN_ERROR_EVENT);
            handler.sendMessage(message);
        }
    }

    public void sendMessage() {
        Log.i(TAG, "sendMessage()");
        String message = String.valueOf(messageET.getText());
        application.newLocalUserMessage(message);
        messageET.setText("");
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HANDLE_HISTORY_CHANGED_EVENT: {
                    Log.i(TAG, "mHandler.handleMessage(): HANDLE_HISTORY_CHANGED_EVENT");
                    updateHistory();
                    break;
                } default:
                    break;
            }
        }
    };

    private static final int HANDLE_APPLICATION_QUIT_EVENT = 0;
    private static final int HANDLE_HISTORY_CHANGED_EVENT = 1;
    private static final int HANDLE_CHANNEL_STATE_CHANGED_EVENT = 2;
    private static final int HANDLE_ALLJOYN_ERROR_EVENT = 3;
}
