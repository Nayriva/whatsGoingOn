
package ee.ut.madp.whatsgoingon.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.chat.ChatApplication;
import ee.ut.madp.whatsgoingon.chat.ChatChannelAdapter;
import ee.ut.madp.whatsgoingon.constants.FirebaseConstants;
import ee.ut.madp.whatsgoingon.models.ChatChannel;
import ee.ut.madp.whatsgoingon.models.Group;
import ee.ut.madp.whatsgoingon.models.User;

public class ChatChannelsFragment extends Fragment {

    private static final String TAG = "chat.ChannelsActivity";

    private ChatApplication application;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference usersRef;
    private DatabaseReference groupsRef;
    private ChatChannelAdapter chatChannelAdapter;

    private SwipeRefreshLayout swipeRefreshLayout;
    private ListView channelsList;
    private TextView channelsStatus;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        application = (ChatApplication) context.getApplicationContext();
        firebaseAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference().child(FirebaseConstants.FIREBASE_CHILD_USERS);
        groupsRef = FirebaseDatabase.getInstance().getReference().child(FirebaseConstants.FIREBASE_CHILD_GROUPS);
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

    private void getChannels() {
        Log.i(TAG, "findChannels()");
        chatChannelAdapter.clear();
        Set<String> channels = application.getChannels();
        for (final String channel: channels) {
            usersRef.child(channel).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    User channelOwner = dataSnapshot.getValue(User.class);
                    if (channelOwner != null) {
                        chatChannelAdapter.add(new ChatChannel(channel,
                                channelOwner.getName(), channelOwner.getPhoto()));
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
            groupsRef.child(channel).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Group groupChannel = dataSnapshot.getValue(Group.class);
                    if (groupChannel != null) {
                        chatChannelAdapter.add(new ChatChannel(channel,
                                groupChannel.getDisplayName(), groupChannel.getPhoto()));
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }

        if (chatChannelAdapter.isEmpty()) {
            channelsStatus.setText(R.string.no_channels_found);
        } else {
            channelsStatus.setText(R.string.join_channel);
        }
        chatChannelAdapter.notifyDataSetChanged();
    }

    private void initialize(View view) {
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swiperefresh);
        setSwipeRefreshLayoutListener();

        channelsList = (ListView) view.findViewById(R.id.lw_channels_list);
        setChatListViewListener();

        channelsStatus = (TextView) view.findViewById(R.id.tw_channels_status);

        //TODO set up proper list_item_view
        chatChannelAdapter = new ChatChannelAdapter(getContext(), R.layout.chat_list_item, new ArrayList<ChatChannel>());
        channelsList.setAdapter(chatChannelAdapter);
        getChannels();
    }

    private void setChatListViewListener() {
        Log.i(TAG, "setChatListViewListener");
        channelsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ChatFragment fragment = new ChatFragment();
                ChatChannel channel = (ChatChannel) parent.getItemAtPosition(position);
                boolean isGroup = application.isGroup(channel.getId());
                if (isGroup) {
                    String[] receivers = application.getGroupReceivers(channel.getId());
                    fragment.setData(channel, true, receivers);
                } else {
                    fragment.setData(channel, false, null);
                }

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
                getChannels();
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }
}
