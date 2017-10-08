package ee.ut.madp.whatsgoingon.chat;

import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import java.util.List;

import ee.ut.madp.whatsgoingon.Observable;
import ee.ut.madp.whatsgoingon.Observer;
import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.models.ChatMessage;

public class ChatDetailActivity extends FragmentActivity implements Observer {

    private static final String TAG = "chat.ChatDetailActivity";

    private ListView messagesListView;
    private ArrayAdapter<ChatMessage> adapter;
    private ChatApplication application;
    private EditText messageEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_detail);

        application = (ChatApplication) getApplication();
        application.addObserver(this);

        messagesListView = (ListView) findViewById(R.id.messagesListView);
        adapter = new ChatMessageAdapter(this, R.layout.message_list_item, application.getHistory());
        messagesListView.setAdapter(adapter);

        messageEditText = (EditText) findViewById(R.id.messageEditText);

        updateHistory();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        application = (ChatApplication)getApplication();
        application.deleteObserver(this);
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.i(TAG, "onSaveInstanceState()");
        super.onSaveInstanceState(outState);
        outState.putString("messageETText", String.valueOf(messageEditText.getText()));
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.i(TAG, "onRestoreInstanceState()");
        super.onRestoreInstanceState(savedInstanceState);
        messageEditText.setText(savedInstanceState.getString("messageETText", ""));
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

    public void sendMessage(View view) {
        Log.i(TAG, "sendMessage()");
        EditText messageET = (EditText) findViewById(R.id.messageEditText);
        String message = String.valueOf(messageET.getText());
        application.newLocalUserMessage(message);
        messageET.setText("");
    }
}
