package ee.ut.madp.whatsgoingon.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.chat.ChatApplication;
import ee.ut.madp.whatsgoingon.helpers.DialogHelper;

public class ChatChannelsFragment extends Fragment {

    private static final String TAG = "chat.ChannelsActivity";

    private ArrayAdapter<String> chatsAdapter;
    private ChatApplication application;
    private FirebaseAuth firebaseAuth;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ListView channelsList;
    private Button btnAddChannel, btnStopChannel;
    private TextView channelsStatus;
    private String channelName;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        application = (ChatApplication) context.getApplicationContext();
        application.checkin();
        firebaseAuth = FirebaseAuth.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat_channels, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initialize(view);
    }

    private void findChannels() {
        Log.i(TAG, "findChannels()");
        chatsAdapter.clear();
        List<String> channels = application.getFoundChannels();
        for (String channel : channels) {
            int lastDot = channel.lastIndexOf('.');
            if (lastDot < 0) {
                continue;
            }
            chatsAdapter.add(channel.substring(lastDot + 1));
        }
        if (chatsAdapter.isEmpty()) {
            channelsStatus.setText(R.string.no_channels_found);
        } else {
            channelsStatus.setText(R.string.join_channel);
        }
        chatsAdapter.notifyDataSetChanged();
    }

    private void initialize(View view) {
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swiperefresh);
        setSwipeRefreshLayoutListener();

        channelsList = (ListView) view.findViewById(R.id.lw_channels_list);
        setChatListViewListener();

        btnAddChannel = (Button) view.findViewById(R.id.btn_add_channel);
        setBtnAddChannelListener();

        btnStopChannel = (Button) view.findViewById(R.id.btn_stop_channel);
        btnStopChannel.setEnabled(false);
        setBtnStopChannelListener();

        channelsStatus = (TextView) view.findViewById(R.id.tw_channels_status);

        //TODO set up proper list_item_view
        chatsAdapter = new ArrayAdapter<>(getContext(), android.R.layout.test_list_item);
        channelsList.setAdapter(chatsAdapter);
    }

    private void setBtnStopChannelListener() {
        btnStopChannel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (application.hostGetChannelName() != null) {
                    application.hostStopChannel();
                    btnStopChannel.setEnabled(false);
                    btnAddChannel.setEnabled(true);
                    chatsAdapter.remove(channelName);
                    if (chatsAdapter.isEmpty()) {
                        channelsStatus.setText(R.string.no_channels_found);
                    } else {
                        channelsStatus.setText(R.string.join_channel);
                    }
                }
            }
        });
    }

    private void setBtnAddChannelListener() {
        btnAddChannel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showChannelDialog(getContext());
            }
        });
    }

    private void setChatListViewListener() {
        Log.i(TAG, "setChatListViewListener");
        channelsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ChatFragment fragment = new ChatFragment();
                fragment.setData((String) parent.getItemAtPosition(position));
                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.containerView, fragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
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

    private void showChannelDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage("Enter name of channel");
        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);
        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                channelName = input.getText().toString();
            }
        });
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                if (channelName != null && !channelName.isEmpty()) {
                    application.hostSetChannelName(channelName);
                    application.hostInitChannel();
                    application.hostStartChannel();
                    btnAddChannel.setEnabled(false);
                    btnStopChannel.setEnabled(true);
                }
            }
        });
        builder.show();
    }
}
