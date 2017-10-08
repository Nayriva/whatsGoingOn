package ee.ut.madp.whatsgoingon.chat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;

import java.text.Normalizer;
import java.util.List;

import ee.ut.madp.whatsgoingon.Observable;
import ee.ut.madp.whatsgoingon.Observer;
import ee.ut.madp.whatsgoingon.R;

public class ChannelsActivity extends FragmentActivity implements Observer {

    private static final String TAG = "chat.ChannelsActivity";
    
    private ListView channelsList;
    private SwipeRefreshLayout swipeRefreshLayout;

    private ArrayAdapter<String> chatsAdapter;
    private ChatApplication application;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_chat_channels);
        application = (ChatApplication) getApplication();
        firebaseAuth = FirebaseAuth.getInstance();
        initialize();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initialize();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        application.deleteObserver(this);
        super.onDestroy();
    }

    private void findChannels() {
        Log.i(TAG, "findChannels()");
        chatsAdapter.clear();
        List<String> channels = application.getFoundChannels();
        if (channels.isEmpty()) {
            chatsAdapter.add("Nothing to show...");
            channelsList.setClickable(false);
        }
        for (String channel : channels) {
            int lastDot = channel.lastIndexOf('.');
            if (lastDot < 0) {
                continue;
            }
            channelsList.setClickable(true);
            chatsAdapter.add(channel.substring(lastDot + 1));
        }
        chatsAdapter.notifyDataSetChanged();
    }

    private void setUpChannel() {
        Log.i(TAG, "setUpChannel()");
        String username = firebaseAuth.getCurrentUser().getDisplayName();
        username = Normalizer
                .normalize(username, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("\\s+", "_");
        application.hostSetChannelName(username);
        application.hostInitChannel();
        application.hostStartChannel();
    }

    private void initialize() {
        application.checkin();
        application.addObserver(this);

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        setSwipeRefreshLayoutListener();

        channelsList = (ListView) findViewById(R.id.channels_list_view);
        setChatListViewListener();

        //TODO set up proper list_item_view
        chatsAdapter = new ArrayAdapter<>(this, android.R.layout.test_list_item);
        channelsList.setAdapter(chatsAdapter);
        if (chatsAdapter.isEmpty()) {
            channelsList.setClickable(false);
            chatsAdapter.add("Nothing to show...");
        }

        if (application.hostGetChannelName() == null) {
            setUpChannel();
        }

        findChannels();
    }

    private void setChatListViewListener() {
        Log.i(TAG, "setChatListViewListener");
        channelsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent();
                intent.putExtra("channelToJoin", parent.getItemIdAtPosition(position));
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    private void setSwipeRefreshLayoutListener() {
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                findChannels();
                swipeRefreshLayout.setRefreshing(false);
            }
        });
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

    private static final int HANDLE_APPLICATION_QUIT_EVENT = 0;
    private static final int HANDLE_CHANNEL_STATE_CHANGED_EVENT = 1;
    private static final int HANDLE_ALLJOYN_ERROR_EVENT = 2;
    public static final int DIALOG_ALLJOYN_ERROR_ID = 3;

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
        Log.i(TAG, "alljoynError");
        if (application.getErrorModule() == ChatApplication.Module.GENERAL ||
                application.getErrorModule() == ChatApplication.Module.USE) {
            showDialog(DIALOG_ALLJOYN_ERROR_ID);
        }
    }
}
