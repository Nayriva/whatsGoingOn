
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

import java.util.Set;

import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.chat.ChatApplication;

public class ChatChannelsFragment extends Fragment {

    private static final String TAG = "chat.ChannelsActivity";

    private ArrayAdapter<String> chatsAdapter;
    private ChatApplication application;
    private FirebaseAuth firebaseAuth;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ListView channelsList;
    private TextView channelsStatus;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        application = (ChatApplication) context.getApplicationContext();
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

    private void getChannels() {
        Log.i(TAG, "findChannels()");
        chatsAdapter.clear();
        Set<String> channels = application.getChannels();

        for (String channel : channels) {
            chatsAdapter.add(channel);
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

        channelsStatus = (TextView) view.findViewById(R.id.tw_channels_status);

        //TODO set up proper list_item_view
        chatsAdapter = new ArrayAdapter<>(getContext(), android.R.layout.test_list_item);
        channelsList.setAdapter(chatsAdapter);
    }

    private void setChatListViewListener() {
        Log.i(TAG, "setChatListViewListener");
        channelsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ChatFragment fragment = new ChatFragment();
                String displayName = (String) parent.getItemAtPosition(position);
                boolean isGroup = application.isGroup(displayName);
                if (isGroup) {
                    String[] receivers = application.getGroupReceivers(displayName);
                    fragment.setData(displayName, true, receivers);
                } else {
                    fragment.setData(displayName, false, null);
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
