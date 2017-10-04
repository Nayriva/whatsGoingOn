package ee.ut.madp.whatsgoingon;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import java.util.List;

public class ChannelsActivity extends AppCompatActivity implements Observer {

    private static final String TAG = "chat.ChannelsActivity";
    private final int LOGIN_REQUEST_CODE = 1;

    private ListView chatsListView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ArrayAdapter<String> chatsAdapter;
    private ChatApplication application;
    private boolean loggedIn = false;
    private String username;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_screen_host);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (loggedIn) {
            setContentView(R.layout.main_screen);
            isLoggedInInitialize();
        } else {
            setContentView(R.layout.main_screen_host);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LOGIN_REQUEST_CODE && resultCode == RESULT_OK) {
            username = data.getStringExtra("username");
            loggedIn = data.getBooleanExtra("loggedIn", false);
        }
    }

    @Override
    public void onDestroy() {
        application = (ChatApplication)getApplication();
        application.deleteObserver(this);
        super.onDestroy();
    }

    private void findChannels() {
        chatsAdapter.clear();
        List<String> channels = application.getFoundChannels();
        if (channels.isEmpty()) {
            chatsAdapter.add("Nothing to show...");
        }
        for (String channel : channels) {
            int lastDot = channel.lastIndexOf('.');
            if (lastDot < 0) {
                continue;
            }
            chatsAdapter.add(channel.substring(lastDot + 1));
        }
        chatsAdapter.notifyDataSetChanged();
    }

    private void setUpChannel() {
        application.hostSetChannelName(username);
        application.hostInitChannel();
        application.hostStartChannel();
    }

    private void setChatListViewListener() {
        chatsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent newChat = new Intent(getApplicationContext(), ChatDetailActivity.class);
                newChat.putExtra("username", username);
                startActivity(newChat);
            }
        });
    }

    public void startLogin(View view) {
        Intent logIn = new Intent(this, LoginActivity.class);
        startActivityForResult(logIn, LOGIN_REQUEST_CODE);
    }

    private void isLoggedInInitialize() {
        application = (ChatApplication) getApplication();
        application.checkin();
        application.addObserver(this);

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                findChannels();
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        chatsListView = (ListView) findViewById(R.id.chatListView);
        setChatListViewListener();
        chatsAdapter = new ArrayAdapter<>(this, android.R.layout.test_list_item); //TODO
        chatsListView.setAdapter(chatsAdapter);
        if (chatsAdapter.isEmpty()) {
            chatsAdapter.add("Nothing to show...");
        }

        if (application.hostGetChannelName() == null) {
            setUpChannel();
        }
        findChannels();
    }

    @Override
    public synchronized void update(Observable o, Object arg) {
        Log.i(TAG, "update(" + arg + ")");
        String qualifier = (String)arg;

        if (qualifier.equals(ChatApplication.APPLICATION_QUIT_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_APPLICATION_QUIT_EVENT);
            mHandler.sendMessage(message);
        }

        if (qualifier.equals(ChatApplication.HOST_CHANNEL_STATE_CHANGED_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_CHANNEL_STATE_CHANGED_EVENT);
            mHandler.sendMessage(message);
        }

        if (qualifier.equals(ChatApplication.ALLJOYN_ERROR_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_ALLJOYN_ERROR_EVENT);
            mHandler.sendMessage(message);
        }
    }

    public static final int DIALOG_ALLJOYN_ERROR_ID = 3;

    private static final int HANDLE_APPLICATION_QUIT_EVENT = 0;
    private static final int HANDLE_CHANNEL_STATE_CHANGED_EVENT = 1;
    private static final int HANDLE_ALLJOYN_ERROR_EVENT = 2;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HANDLE_APPLICATION_QUIT_EVENT: {
                    Log.i(TAG, "mHandler.handleMessage(): HANDLE_APPLICATION_QUIT_EVENT");
                    finish();
                } break;
                case HANDLE_ALLJOYN_ERROR_EVENT : {
                    Log.i(TAG, "mHandler.handleMessage(): HANDLE_ALLJOYN_ERROR_EVENT");
                    alljoynError();
                } break;
                default:
                    break;
            }
        }
    };

    private void alljoynError() {
        if (application.getErrorModule() == ChatApplication.Module.GENERAL ||
                application.getErrorModule() == ChatApplication.Module.USE) {
            showDialog(DIALOG_ALLJOYN_ERROR_ID);
        }
    }
}
