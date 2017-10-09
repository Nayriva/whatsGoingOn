package ee.ut.madp.whatsgoingon.fragments;

import android.content.Context;
import android.content.Intent;
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

import com.google.firebase.auth.FirebaseAuth;

import java.text.Normalizer;
import java.util.List;

import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.chat.ChatApplication;

public class ChatChannelsFragment extends Fragment {

    private static final String TAG = "chat.ChannelsActivity";

    private ListView channelsList;
    private SwipeRefreshLayout swipeRefreshLayout;

    private ArrayAdapter<String> chatsAdapter;
    private ChatApplication application;
    private FirebaseAuth firebaseAuth;

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
        View view = inflater.inflate(R.layout.fragment_chat_channels, container, false);
        initialize(view);
        return view;
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

    private void initialize(View view) {
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swiperefresh);
        setSwipeRefreshLayoutListener();

        channelsList = (ListView) view.findViewById(R.id.channels_list_view);
        setChatListViewListener();

        //TODO set up proper list_item_view
        chatsAdapter = new ArrayAdapter<>(getContext(), android.R.layout.test_list_item);
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
}
