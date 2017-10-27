package ee.ut.madp.whatsgoingon.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import ee.ut.madp.whatsgoingon.chat.ChatApplication;
import ee.ut.madp.whatsgoingon.adapters.ChatMessageAdapter;
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

public class ChatFragment extends Fragment implements Observer {

    private ChatApplication application;

    @BindView(R.id.lv_chat_messages)
    ListView messagesListView;
    @BindView(R.id.et_message)
    EditText messageET;

    private ChatMessageAdapter adapter;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference groupsRef;
    private DatabaseReference usersRef;

    private ChatChannel chatChannel;
    private boolean isGroup;
    private String[] receivers;

    public ChatFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        application = (ChatApplication) getActivity().getApplication();
        firebaseAuth = FirebaseAuth.getInstance();
        groupsRef = FirebaseDatabase.getInstance().getReference().child(FirebaseConstants.FIREBASE_CHILD_GROUPS);
        usersRef = FirebaseDatabase.getInstance().getReference().child(FirebaseConstants.FIREBASE_CHILD_USERS);
        application.addObserver(this);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_conversation, container, false);
        getActivity().setTitle(chatChannel.getName());

        ButterKnife.bind(this, view);

        adapter = new ChatMessageAdapter(getContext(), R.layout.list_item_message, application.getHistory(chatChannel.getId()));
        messagesListView.setAdapter(adapter);

        updateHistory();

        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        application.deleteObserver(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (isGroup) {
            inflater.inflate(R.menu.group_chat_menu, menu);
        }
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
                super.onOptionsItemSelected(item);
                break;
        }
        return false;
    }

    private void showAddMemberDialog() {
        final Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.dialog_add_group_member);
        dialog.setTitle(add_members);

        Button buttonOk = (Button) dialog.findViewById(R.id.btn_add_group_members_ok);
        Button buttonCancel = (Button) dialog.findViewById(R.id.btn_add_group_members_cancel);
        ListView peopleListView = (ListView) dialog.findViewById(R.id.lv_add_group_members_list);
        Set<String> channelsNearDevice = application.getChannels();
        for (String receiver: receivers) {
            if (channelsNearDevice.contains(receiver)) {
                channelsNearDevice.remove(receiver);
            }
        }
        for (String channel : channelsNearDevice) {
            if (application.isGroup(channel)) {
                channelsNearDevice.remove(channel);
            }
        }

        final List<GroupParticipant> possibleParticipants = new ArrayList<>();

        for (String member: channelsNearDevice) {
            usersRef.child(member).addListenerForSingleValueEvent(new ValueEventListener() {
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
        final GroupParticipantsAdapter adapter = new GroupParticipantsAdapter(getContext(),
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
                    Toast.makeText(getContext(), R.string.no_new_group_member_selected, Toast.LENGTH_SHORT).show();
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
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
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
        Fragment fragment = new ChatChannelsFragment();
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.containerView, fragment).commit();
    }

    @OnClick(R.id.bt_send)
    public void sendMessage() {
        String messageText = String.valueOf(messageET.getText());
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
        messageET.setText("");
        updateHistory();
    }


    private void updateHistory() {
        adapter.clear();
        List<ChatMessage> history = application.getHistory(chatChannel.getId());
        for (ChatMessage message: history) {
            adapter.add(message);
        }
        adapter.notifyDataSetChanged();
        messagesListView.smoothScrollToPosition(history.size() - 1);
    }

    public void setData(ChatChannel chatChannel, boolean isGroup, String[] receivers) {
        this.chatChannel = chatChannel;
        this.isGroup = isGroup;
        this.receivers = receivers;
    }

    @Override
    public synchronized void update(Observable o, String qualifier, String data) {
        switch (qualifier) {
            case ChatApplication.MESSAGE_RECEIVED: {
                String type = ChatHelper.getMessageType(data);
                String sender = null;
                if (type.equals("S")) {
                    sender = ChatHelper.oneToOneMessageSender(data);
                } else if (type.equals("G")) {
                    sender = ChatHelper.groupMessageSender(data);
                }

                if (data.equals(chatChannel.getId())) {
                    updateHistory();
                } else {
                    //display notification
                }
            } break;
            case ChatApplication.GROUP_RECEIVERS_CHANGED: {
                receivers = ChatHelper.groupAdvertiseMessageReceivers(data);
            } break;
            case ChatApplication.GROUP_DELETED: {
                application.deleteGroup(chatChannel.getId(), true);
                Fragment fragment = new ChatChannelsFragment();
                FragmentManager fragmentManager = getFragmentManager();
                fragmentManager.beginTransaction().replace(R.id.containerView, fragment).commit();
                Toast.makeText(getContext(), "Group has been deleted", Toast.LENGTH_SHORT).show();
            } break;
        }
    }
}
