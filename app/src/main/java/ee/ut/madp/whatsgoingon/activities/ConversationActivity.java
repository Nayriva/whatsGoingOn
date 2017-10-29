package ee.ut.madp.whatsgoingon.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.adapters.MessageAdapter;
import ee.ut.madp.whatsgoingon.chat.ChatApplication;
import ee.ut.madp.whatsgoingon.chat.GroupParticipantsAdapter;
import ee.ut.madp.whatsgoingon.chat.Observable;
import ee.ut.madp.whatsgoingon.chat.Observer;
import ee.ut.madp.whatsgoingon.constants.FirebaseConstants;
import ee.ut.madp.whatsgoingon.helpers.ChatHelper;
import ee.ut.madp.whatsgoingon.models.ChatChannel;
import ee.ut.madp.whatsgoingon.models.ChatMessage;
import ee.ut.madp.whatsgoingon.models.Group;
import ee.ut.madp.whatsgoingon.models.GroupParticipant;
import ee.ut.madp.whatsgoingon.models.User;

import static ee.ut.madp.whatsgoingon.R.string.add_members;
import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.PARCEL_CHAT_CHANNEL;

public class ConversationActivity extends AppCompatActivity implements Observer {
    public static final String TAG = ConversationActivity.class.getSimpleName();

    @BindView(R.id.recyclerView) RecyclerView recyclerView;
    @BindView(R.id.et_message) EditText editTextMessage;

    private MessageAdapter messageAdapter;
    private List<ChatMessage> chatMessageList = new ArrayList<>();
    private FirebaseAuth firebaseAuth;
    private DatabaseReference groupsRef;
    private DatabaseReference usersRef;
    private ChatApplication application;
    private ChatChannel chatChannel;
    private boolean isGroup;
    private String[] receivers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);
        ButterKnife.bind(this);

        application = (ChatApplication) getApplication();
        firebaseAuth = FirebaseAuth.getInstance();
        application.addObserver(this);
        groupsRef = FirebaseDatabase.getInstance().getReference().child(FirebaseConstants.FIREBASE_CHILD_GROUPS);
        usersRef = FirebaseDatabase.getInstance().getReference().child(FirebaseConstants.FIREBASE_CHILD_USERS);

        if (getIntent().hasExtra(PARCEL_CHAT_CHANNEL)) {
            chatChannel = getIntent().getParcelableExtra(PARCEL_CHAT_CHANNEL);

            isGroup = chatChannel.isGroup();
            setTitle(chatChannel.getName());
            if (isGroup) {
                this.receivers = application.getGroupReceivers(chatChannel.getId());
            }
        }

        setupRecyclerView();

        updateHistory();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.group_menu_add_member: {
                showAddMemberDialog();
                return true;
            }
            case R.id.group_menu_leave_group: {
                showLeaveChannelDialog();
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public synchronized void update(Observable o, int qualifier, String data) {
        switch(qualifier) {
            case ChatApplication.ONE_TO_ONE_MESSAGE_RECEIVED:
            case ChatApplication.GROUP_MESSAGE_RECEIVED:
            {
                if (data.equals(chatChannel.getId())) {
                    updateHistory();
                } else {
                    //TODO show notification of incoming message
                }
            } break;
            case ChatApplication.GROUP_RECEIVERS_CHANGED: {
                if (data.equals(chatChannel.getId())) {
                    receivers = application.getGroupReceivers(chatChannel.getId());
                }
            } break;
            case ChatApplication.GROUP_DELETED: {
                if (data.equals(chatChannel.getId())) {
                    Toast.makeText(ConversationActivity.this, "Group has been deleted due to too few members...",
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
            } break;
            default:
                break;
        }
    }

    @OnClick(R.id.bt_send)
    public void sendMessage() {
        String messageText = String.valueOf(editTextMessage.getText());
        if (messageText == null || messageText.isEmpty()) {
            return;
        }
        String sender = firebaseAuth.getCurrentUser().getUid();
        String message;
        if (isGroup) {
            message = ChatHelper.groupMessage(sender, firebaseAuth.getCurrentUser().getDisplayName(),
                    chatChannel.getId(), receivers, messageText);
        } else {
            message = ChatHelper.oneToOneMessage(sender, firebaseAuth.getCurrentUser().getDisplayName(),
                    chatChannel.getId(), messageText);
        }
        application.sendMessage(message);
        editTextMessage.setText("");
        updateHistory();
    }

    private void setupRecyclerView() {
        messageAdapter = new MessageAdapter(this, chatMessageList);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(messageAdapter);
    }

    private void updateHistory() {
        chatMessageList.clear();
        chatMessageList.addAll(application.getHistory(chatChannel.getId()));
        messageAdapter.notifyDataSetChanged();
    }

    private void showAddMemberDialog() {
        final Dialog dialog = new Dialog(ConversationActivity.this);
        dialog.setContentView(R.layout.dialog_add_group_member);
        dialog.setTitle(add_members);

        Button buttonOk = (Button) dialog.findViewById(R.id.btn_add_group_members_ok);
        Button buttonCancel = (Button) dialog.findViewById(R.id.btn_add_group_members_cancel);
        ListView peopleListView = (ListView) dialog.findViewById(R.id.lv_add_group_members_list);
        Set<ChatChannel> channelsNearDevice = application.getChannels();
        for (ChatChannel chatChannel: channelsNearDevice) { //remove groups
            if (application.isGroup(chatChannel.getId())) {
                channelsNearDevice.remove(chatChannel);
            } else {
                for (String receiver: receivers) { //remove receivers
                    if (chatChannel.getId().equals(receiver)) {
                        channelsNearDevice.remove(chatChannel);
                    }
                }
            }

        }

        final List<GroupParticipant> possibleParticipants = new ArrayList<>();

        for (ChatChannel member: channelsNearDevice) {
            usersRef.child(member.getId()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    User user = dataSnapshot.getValue(User.class);
                    possibleParticipants.add(new GroupParticipant(user.getId(),
                            user.getName(), user.getPhoto(), false));
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
        final GroupParticipantsAdapter adapter = new GroupParticipantsAdapter(ConversationActivity.this,
                R.layout.dialog_group_list_item, possibleParticipants);
        peopleListView.setAdapter(adapter);

        buttonOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<GroupParticipant> allData = adapter.getItems();
                List<GroupParticipant> selected = new ArrayList<>();
                for (GroupParticipant participant : allData) {
                    if (participant.isSelected()) {
                        selected.add(participant);
                    }
                }
                if (selected.size() > 0) {
                    addGroupMembers(selected);
                    dialog.dismiss();
                } else {
                    Toast.makeText(ConversationActivity.this, R.string.no_new_group_member_selected, Toast.LENGTH_SHORT).show();
                }
            }
        });

        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private void addGroupMembers(List<GroupParticipant> selected) {
        String groupId = chatChannel.getId();
        List<String> newParticipants = new ArrayList<>();
        Collections.addAll(newParticipants, receivers);
        for (GroupParticipant participant: selected) {
            newParticipants.add(participant.getId());
        }
        application.deleteGroup(groupId, false);
        application.createGroup(groupId, newParticipants.toArray(new String[0]));
        Group updatedGroup = new Group(groupId, chatChannel.getName(), chatChannel.getPhoto(), newParticipants);
        groupsRef.child(groupId).setValue(updatedGroup);
    }

    private void showLeaveChannelDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.leave_group_question));
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                leaveGroup();
                dialogInterface.dismiss();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.show();
    }

    private void leaveGroup() {
        String groupId = chatChannel.getId();
        final List<String> newReceivers = new ArrayList<>();
        for(String receiver : receivers) {
            if (! receiver.equals(firebaseAuth.getCurrentUser().getUid())) {
                newReceivers.add(receiver);
            }
        }
        application.deleteGroup(groupId, true);
        if (newReceivers.size() < 3) {
            groupsRef.child(groupId).removeValue();
        } else {
            Group newGroup = new Group(groupId, chatChannel.getName(), chatChannel.getPhoto(), newReceivers);
            groupsRef.child(groupId).setValue(newGroup);
            application.createGroup(groupId, newReceivers.toArray(new String[0]));
        }
    }
}
