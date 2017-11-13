
package ee.ut.madp.whatsgoingon.fragments;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mvc.imagepicker.ImagePicker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ee.ut.madp.whatsgoingon.comparators.ChatChannelComparator;
import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.adapters.ChatChannelAdapter;
import ee.ut.madp.whatsgoingon.ApplicationClass;
import ee.ut.madp.whatsgoingon.adapters.GroupParticipantsAdapter;
import ee.ut.madp.whatsgoingon.chat.Observable;
import ee.ut.madp.whatsgoingon.chat.Observer;
import ee.ut.madp.whatsgoingon.comparators.GroupParticipantComparator;
import ee.ut.madp.whatsgoingon.constants.FirebaseConstants;
import ee.ut.madp.whatsgoingon.helpers.ChatHelper;
import ee.ut.madp.whatsgoingon.helpers.DateHelper;
import ee.ut.madp.whatsgoingon.helpers.ImageHelper;
import ee.ut.madp.whatsgoingon.helpers.MessageNotificationHelper;
import ee.ut.madp.whatsgoingon.helpers.UserHelper;
import ee.ut.madp.whatsgoingon.models.ChatChannel;
import ee.ut.madp.whatsgoingon.models.ChatMessage;
import ee.ut.madp.whatsgoingon.models.Group;
import ee.ut.madp.whatsgoingon.models.GroupParticipant;

import static android.widget.AdapterView.OnClickListener;

public class ChatChannelsFragment extends Fragment implements Observer {

    private static final String TAG = "chat.ChannelsActivity";

    @BindView(R.id.swiperefresh) SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.rv_channels_list) RecyclerView recyclerView;
    @BindView(R.id.tv_channels_status) TextView channelsStatus;

    private ApplicationClass application;
    private DatabaseReference groupsRef;
    private ChatChannelAdapter chatChannelAdapter;
    private String groupPhotoResult;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        application = (ApplicationClass) getActivity().getApplicationContext();
        groupsRef = FirebaseDatabase.getInstance().getReference().child(FirebaseConstants.FIREBASE_CHILD_GROUPS);

        downloadGroups();
    }

    @Override
    public void onResume() {
        super.onResume();
        application.addObserver(this);
        getChannels();
        setUpLastMessages();
    }

    @Override
    public void onPause() {
        super.onPause();
        application.deleteObserver(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_channels, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecyclerView();
        getChannels();
        setSwipeRefreshLayoutListener();
        setUpLastMessages();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Bitmap bitmap = ImagePicker.getImageFromResult(getActivity(), requestCode, resultCode, data);
        if (bitmap != null) {
            groupPhotoResult = ImageHelper.encodeBitmap(bitmap);
        }
    }

    private void setupRecyclerView() {
        chatChannelAdapter = new ChatChannelAdapter(getContext(), new ArrayList<ChatChannel>());
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(chatChannelAdapter);
    }

    private void getChannels() {
        chatChannelAdapter.clearChannels();
        Set<ChatChannel> channels = application.getChannels();
        for (ChatChannel chatChannel : channels) {
            chatChannelAdapter.addChannel(chatChannel);
        }
        if (channels.isEmpty()) {
            channelsStatus.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            channelsStatus.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
        setUpLastMessages();
        Collections.sort(chatChannelAdapter.getChannels(), new ChatChannelComparator());
        chatChannelAdapter.notifyDataSetChanged();
    }

    private void downloadGroups() {
        groupsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot groupSnapshot: dataSnapshot.getChildren()) {
                        Group group = groupSnapshot.getValue(Group.class);
                    if (group != null) {
                        if (group.getReceivers().contains(UserHelper.getCurrentUserId())) {
                            application.createGroup(group.getId(), group.getReceivers().toArray(new String[0]));
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                //left blank intentionally
            }
        });
    }

    @OnClick(R.id.fab_add_channel)
    public void addGroup() {
        final Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.dialog_add_group);
        dialog.setTitle(getString(R.string.select_participants));

        Button pickGroupPhoto = (Button) dialog.findViewById(R.id.btn_pick_group_photo);
        Button dialogConfirm = (Button) dialog.findViewById(R.id.btn_pick_participants_ok);
        Button dialogDismiss = (Button) dialog.findViewById(R.id.btn_pick_participants_dismiss);
        ListView participantsList = (ListView) dialog.findViewById(R.id.lv_group_dialog_options_list);
        final EditText groupName = (EditText) dialog.findViewById(R.id.et_group_dialog_group_name);

        List<GroupParticipant> participants = new ArrayList<>();
        for (ChatChannel tmp : chatChannelAdapter.getChannels()) {
            if (! application.isGroup(tmp.getId())) {
                participants.add(new GroupParticipant(tmp.getId(), tmp.getName(), tmp.getPhoto(), false));
            }
        }
        Collections.sort(participants, new GroupParticipantComparator());

        final GroupParticipantsAdapter dialogAdapter = new GroupParticipantsAdapter(getContext(),
                R.layout.dialog_group_list_item, participants);
        participantsList.setAdapter(dialogAdapter);

        pickGroupPhoto.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                ImagePicker.pickImage(getActivity());
            }
        });

        dialogConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (createGroup(String.valueOf(groupName.getText()), dialogAdapter.getItems())) {
                    dialog.dismiss();
                }
            }
        });

        dialogDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        dialog.show();

    }

    private boolean createGroup(String groupName, List<GroupParticipant> participants) {
        if (groupName == null || groupName.isEmpty()) {
            Toast.makeText(getContext(), R.string.no_group_name, Toast.LENGTH_SHORT).show();
            return false;
        }
        List<String> selected = new ArrayList<>();
        for (GroupParticipant item: participants) {
            if (item.isSelected()) {
                selected.add(item.getId());
            }
        }
        selected.add(ApplicationClass.loggedUser.getId());
        if (selected.size() <= 2) {
            Toast.makeText(getContext(), R.string.group_too_small, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (groupPhotoResult == null) {
            groupPhotoResult = ImageHelper.encodeBitmap(
                    BitmapFactory.decodeResource(getResources(), R.drawable.user));
        }
        String gid =  UUID.randomUUID().toString().replaceAll("-", "");
        Group group = new Group(gid, groupName, groupPhotoResult, selected);
        FirebaseDatabase.getInstance().getReference().child(FirebaseConstants.FIREBASE_CHILD_GROUPS).child(gid).setValue(group);
        application.createGroup(gid, selected.toArray(new String[0]));
        return true;
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

    private void setUpLastMessages() {
        for (ChatChannel chatChannel: chatChannelAdapter.getChannels()) {
            ChatMessage lastMessage = application.getLastMessage(chatChannel.getId());
            if (lastMessage != null) {
                if (chatChannel.isGroup()) {
                    chatChannel.setLastMessage(lastMessage.getDisplayName() + ": " + lastMessage.getMessageText());
                } else {
                    chatChannel.setLastMessage(lastMessage.getMessageText());
                }
                chatChannel.setLastMessageTime(DateHelper.parseTimeFromLong(lastMessage.getMessageTime()));
                chatChannelAdapter.notifyDataSetChanged();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void update(Observable o, int qualifier, String data) {
        switch (qualifier) {
            //ADDING CHANNELS
            case ApplicationClass.USER_CHANNEL_DISCOVERED: //NEW CHANNEL HAS BEEN DISCOVERED
            case ApplicationClass.GROUP_CHANNEL_DISCOVERED: {
                //data contains id of channel
                ChatChannel foundChannel = application.getChannel(data);
                if (recyclerView.getVisibility() == View.GONE) {
                    recyclerView.setVisibility(View.VISIBLE);
                    channelsStatus.setVisibility(View.GONE);
                }
                chatChannelAdapter.addChannel(foundChannel);
                chatChannelAdapter.notifyDataSetChanged();
            } break;
            //DELETING CHANNELS
            case ApplicationClass.USER_CHANNEL_LEAVING:
            case ApplicationClass.GROUP_DELETED: {
                //data contains id of channel
                for (ChatChannel channel : chatChannelAdapter.getChannels()) {
                    if (channel.getId().equals(data)) {
                        chatChannelAdapter.removeChannel(channel);
                        break;
                    }
                }
                chatChannelAdapter.notifyDataSetChanged();
            } break;
            case ApplicationClass.ONE_TO_ONE_MESSAGE_RECEIVED:
            case ApplicationClass.GROUP_MESSAGE_RECEIVED: {
                ChatChannel chatChannel = chatChannelAdapter.getChannelById(data);
                ChatMessage lastMessage = application.getLastMessage(data);
                if (chatChannel != null && lastMessage != null) {
                    updateLastMessage(chatChannel, lastMessage);
                    MessageNotificationHelper.showNotification(getContext(), chatChannel.getName(),
                            chatChannel.getLastMessage());
                }
            } break;
        }
    }

    private void updateLastMessage(ChatChannel chatChannel, ChatMessage lastMessage) {
        if (chatChannel.isGroup()) {
            if (ChatHelper.isImageText(lastMessage.getMessageText())) {
                chatChannel.setLastMessage(lastMessage.getDisplayName() + ": " + "Sent a picture");
            } else {
                chatChannel.setLastMessage(lastMessage.getDisplayName() + ": " + lastMessage.getMessageText());
            }
        } else {
            if (ChatHelper.isImageText(lastMessage.getMessageText())) {
                chatChannel.setLastMessage(lastMessage.getDisplayName() + ": " + "Sent a picture");
            } else {
                chatChannel.setLastMessage(lastMessage.getMessageText());
            }
        }
        chatChannel.setLastMessageTime(DateHelper.parseTimeFromLong(lastMessage.getMessageTime()));
        Collections.sort(chatChannelAdapter.getChannels(), new ChatChannelComparator());
        chatChannelAdapter.notifyDataSetChanged();
    }
}
