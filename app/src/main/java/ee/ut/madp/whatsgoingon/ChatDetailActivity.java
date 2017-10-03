package ee.ut.madp.whatsgoingon;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import java.util.List;

public class ChatDetailActivity extends AppCompatActivity implements Observer {

    private static final String TAG = "chat.ChatDetailActivity";

    private ArrayAdapter<ChatMessage> adapter;
    private ChatApplication application;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_detail);

        application = (ChatApplication) getApplication();
        application.addObserver(this);

        ListView messagesListView = (ListView) findViewById(R.id.messagesListView);
        adapter = new ChatMessageAdapter(getApplicationContext(), R.layout.message_list_item, null);
        messagesListView.setAdapter(adapter);

        username = getIntent().getStringExtra("username");

        updateHistory();
    }

    public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        application = (ChatApplication)getApplication();
        application.deleteObserver(this);
        super.onDestroy();
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
        EditText messageET = (EditText) findViewById(R.id.messageEditText);
        String messageText = String.valueOf(messageET.getText());
        String messageAuthor = username;
        ChatMessage newMessage = new ChatMessage(messageText, messageAuthor);
        application.newLocalUserMessage(newMessage);
        messageET.setText("");
    }
}
