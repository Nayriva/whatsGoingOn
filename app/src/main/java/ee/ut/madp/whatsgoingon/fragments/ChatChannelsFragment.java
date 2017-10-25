
package ee.ut.madp.whatsgoingon.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mvc.imagepicker.ImagePicker;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.chat.ChatApplication;
import ee.ut.madp.whatsgoingon.chat.ChatChannelAdapter;
import ee.ut.madp.whatsgoingon.chat.GroupParticipantsAdapter;
import ee.ut.madp.whatsgoingon.chat.Observable;
import ee.ut.madp.whatsgoingon.chat.Observer;
import ee.ut.madp.whatsgoingon.constants.FirebaseConstants;
import ee.ut.madp.whatsgoingon.helpers.ChatHelper;
import ee.ut.madp.whatsgoingon.helpers.ImageHelper;
import ee.ut.madp.whatsgoingon.models.ChatChannel;
import ee.ut.madp.whatsgoingon.models.Group;
import ee.ut.madp.whatsgoingon.models.GroupParticipant;
import ee.ut.madp.whatsgoingon.models.User;

import static android.widget.AdapterView.*;

public class ChatChannelsFragment extends Fragment implements Observer {

    private static final String TAG = "chat.ChannelsActivity";

    private ChatApplication application;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference usersRef;
    private DatabaseReference groupsRef;
    private ChatChannelAdapter chatChannelAdapter;
    private String groupPhotoResult;

    private SwipeRefreshLayout swipeRefreshLayout;
    private ListView channelsList;
    private TextView channelsStatus;
    private FloatingActionButton addGroupButton;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        application = (ChatApplication) context.getApplicationContext();
        firebaseAuth = FirebaseAuth.getInstance();
        application.addObserver(this);
        usersRef = FirebaseDatabase.getInstance().getReference().child(FirebaseConstants.FIREBASE_CHILD_USERS);
        groupsRef = FirebaseDatabase.getInstance().getReference().child(FirebaseConstants.FIREBASE_CHILD_GROUPS);

        downloadGroups();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat_channels, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getActivity().setTitle(R.string.chat_item);
        initialize(view);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Bitmap bitmap = ImagePicker.getImageFromResult(getActivity(), requestCode, resultCode, data);
        if (bitmap != null) {
            groupPhotoResult = ImageHelper.encodeBitmap(bitmap);
        }

    }

    private void initialize(View view) {
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swiperefresh);
        channelsList = (ListView) view.findViewById(R.id.lw_channels_list);
        addGroupButton = (FloatingActionButton) view.findViewById(R.id.fab_add_channel);
        channelsStatus = (TextView) view.findViewById(R.id.tw_channels_status);

        chatChannelAdapter = new ChatChannelAdapter(getContext(), R.layout.chat_list_item, new ArrayList<ChatChannel>());
        channelsList.setAdapter(chatChannelAdapter);
        getChannels();
        setSwipeRefreshLayoutListener();
        setChatListViewListener();
        setAddGroupButtonListener();
    }

    private void getChannels() {
        chatChannelAdapter.clear();
        Set<String> channels = application.getChannels();
        for (final String channel: channels) {
            if (application.isGroup(channel)) {
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
                        //intentionally left blank
                    }
                });
            } else {
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
                        //intentionally left blank
                    }
                });
            }
        }

        if (channels.isEmpty()) {
            channelsStatus.setText(R.string.no_channels_found);
        } else {
            channelsStatus.setText(R.string.join_channel);
        }
        chatChannelAdapter.notifyDataSetChanged();
    }

    private void downloadGroups() {
        groupsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot groupSnapshot: dataSnapshot.getChildren()) {
                    Group group = groupSnapshot.getValue(Group.class);
                    if (group != null) {
                        if (group.getReceivers().contains(firebaseAuth.getCurrentUser().getUid())) {
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

    private void setAddGroupButtonListener() {
        addGroupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Dialog dialog = new Dialog(getContext());
                dialog.setContentView(R.layout.dialog_add_group);
                dialog.setTitle(getString(R.string.select_participants));

                Button pickGroupPhoto = (Button) dialog.findViewById(R.id.btn_pick_group_photo);
                Button dialogConfirm = (Button) dialog.findViewById(R.id.btn_pick_participants_ok);
                Button dialogDismiss = (Button) dialog.findViewById(R.id.btn_pick_participants_dismiss);
                ListView participantsList = (ListView) dialog.findViewById(R.id.lv_group_dialog_options_list);
                final EditText groupName = (EditText) dialog.findViewById(R.id.et_group_dialog_group_name);

                List<ChatChannel> channels = chatChannelAdapter.getItems();
                List<GroupParticipant> participants = new ArrayList<>();
                for (ChatChannel tmp : channels) {
                    if (! application.isGroup(tmp.getId())) {
                        participants.add(new GroupParticipant(tmp.getId(), tmp.getName(), tmp.getPhoto(), false));
                    }
                }

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
        });
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
        if (selected.size() <= 2) {
            Toast.makeText(getContext(), R.string.group_too_small, Toast.LENGTH_SHORT).show();
            return false;
        }
        String gid =  UUID.randomUUID().toString().replaceAll("-", "");
        Group group = new Group(gid, groupName, groupPhotoResult, selected);
        FirebaseDatabase.getInstance().getReference().child(FirebaseConstants.FIREBASE_CHILD_GROUPS).child(gid).setValue(group);
        application.createGroup(gid, selected.toArray(new String[0]));
        return true;
    }

    private void setChatListViewListener() {
        Log.i(TAG, "setChatListViewListener");
        channelsList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ChatChannel channel = (ChatChannel) parent.getItemAtPosition(position);
                ChatFragment fragment = new ChatFragment();
                boolean isGroup = application.isGroup(channel.getId());
                if (isGroup) {
                    String[] receivers = application.getGroupReceivers(channel.getId());
                    fragment.setData(channel, true, receivers);
                } else {
                    fragment.setData(channel, false, null);
                }
                channel.setNewMessage(false);
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

    @Override
    public void update(Observable o, String qualifier, String data) {
        switch (qualifier) {
            case ChatApplication.GROUP_RECEIVERS_CHANGED:
            case ChatApplication.GROUP_DELETED: {
                getChannels();
            } break;
            case ChatApplication.MESSAGE_RECEIVED: {
                String type = ChatHelper.getMessageType(data);
                String sender = null;
                if (type.equals("S")) {
                    sender = ChatHelper.oneToOneMessageSender(data);
                } else if (type.equals("G")) {
                    sender = ChatHelper.groupMessageSender(data);
                }

                for (ChatChannel channel : chatChannelAdapter.getItems()) {
                    if (channel.getId().equals(sender)) {
                        channel.setNewMessage(true);
                        break;
                        //TODO maybe show notification
                    }
                }
            }
        }
    }
}
