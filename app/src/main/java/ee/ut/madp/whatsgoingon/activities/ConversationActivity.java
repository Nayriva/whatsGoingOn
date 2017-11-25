package ee.ut.madp.whatsgoingon.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ee.ut.madp.whatsgoingon.ApplicationClass;
import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.adapters.GroupParticipantsAdapter;
import ee.ut.madp.whatsgoingon.adapters.MessageAdapter;
import ee.ut.madp.whatsgoingon.chat.Observable;
import ee.ut.madp.whatsgoingon.chat.Observer;
import ee.ut.madp.whatsgoingon.constants.FirebaseConstants;
import ee.ut.madp.whatsgoingon.helpers.ChatHelper;
import ee.ut.madp.whatsgoingon.helpers.ImageHelper;
import ee.ut.madp.whatsgoingon.helpers.MessageNotificationHelper;
import ee.ut.madp.whatsgoingon.models.ChatChannel;
import ee.ut.madp.whatsgoingon.models.ChatMessage;
import ee.ut.madp.whatsgoingon.models.Group;
import ee.ut.madp.whatsgoingon.models.GroupParticipant;
import ee.ut.madp.whatsgoingon.models.User;

import static ee.ut.madp.whatsgoingon.R.string.add_members;
import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.EXTRA_CHANNEL_ID;
import static ee.ut.madp.whatsgoingon.constants.PermissionConstants.PERMISSIONS_GROUP_ONE;

/**
 * Activity for displaying chat conversation for single chat channel;
 */
public class ConversationActivity extends AppCompatActivity implements Observer {
    public static final String TAG = ConversationActivity.class.getSimpleName();
    private static final int PICK_PHOTO_REQUEST_CODE = 1;
    private static final int TAKE_PHOTO_REQUEST_CODE = 2;

    @BindView(R.id.recyclerView) RecyclerView recyclerView;
    @BindView(R.id.et_message) EditText editTextMessage;

    private LinearLayoutManager linearLayoutManager;
    private MessageAdapter messageAdapter;
    private List<ChatMessage> chatMessageList = new ArrayList<>();
    private DatabaseReference groupsRef;
    private DatabaseReference usersRef;
    private ApplicationClass application;
    private ChatChannel chatChannel;
    private Map<String, String> photosMap;
    private Uri imageUri;
    String[] PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);
        ButterKnife.bind(this);

        application = (ApplicationClass) getApplication();
        groupsRef = FirebaseDatabase.getInstance().getReference().child(FirebaseConstants.FIREBASE_CHILD_GROUPS);
        usersRef = FirebaseDatabase.getInstance().getReference().child(FirebaseConstants.FIREBASE_CHILD_USERS);
        photosMap = new HashMap<>();

        if (getIntent().hasExtra(EXTRA_CHANNEL_ID)) {
            chatChannel = application.getChannel(getIntent().getStringExtra(EXTRA_CHANNEL_ID));
            setTitle(chatChannel.getName());
        }

        setupRecyclerView();
        if (chatChannel.isGroup()) {
            downloadPhotos();
        } else {
            photosMap.put(application.getLoggedUser().getId(), application.getLoggedUser().getPhoto());
            photosMap.put(chatChannel.getId(), chatChannel.getPhoto());
        }
        updateHistory();
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
        application.addObserver(this);
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
        application.deleteObserver(this);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult: " + requestCode + ", " + resultCode + ", " + data);
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PHOTO_REQUEST_CODE && resultCode == RESULT_OK) {
            Context context = this;
            try {
                InputStream inputStream = context.getContentResolver().openInputStream(data.getData());
                new SendPhotoAsyncTask(application.getLoggedUser().getId(), application.getLoggedUser().getName(),
                        chatChannel.isGroup(), application.getGroupReceivers(chatChannel.getId()),
                        chatChannel.getId()).execute(inputStream);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else if (requestCode == TAKE_PHOTO_REQUEST_CODE && resultCode == RESULT_OK) {
            Uri selectedImage = imageUri;
            new SendTakenPhotoAsyncTask(application.getLoggedUser().getId(), application.getLoggedUser().getName(),
                    chatChannel.isGroup(), application.getGroupReceivers(chatChannel.getId()),
                    chatChannel.getId()).execute(selectedImage);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "onCreateOptionsMenu");
        if (chatChannel.isGroup()) {
            getMenuInflater().inflate(R.menu.group_chat_menu, menu);
        } else {
            getMenuInflater().inflate(R.menu.one_to_one_chat_menu, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "onOptionsItemSelected: " + item);
        switch (item.getItemId()) {
            case R.id.group_menu_add_member: {
                showAddMemberDialog();
                return true;
            }
            case R.id.group_menu_leave_group: {
                showLeaveChannelDialog();
                return true;
            }
            case R.id.one_to_one_menu_show_profile: {
                openProfile();
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public synchronized void update(Observable o, int qualifier, String data) {
        switch (qualifier) {
            case ApplicationClass.ONE_TO_ONE_MESSAGE_RECEIVED:
            case ApplicationClass.GROUP_MESSAGE_RECEIVED: {
                if (data.equals(chatChannel.getId())) {
                    updateHistory();
                    chatChannel.setNewMessage(false);
                } else {
                    ChatChannel chatChannel = application.getChannel(data);
                    ChatMessage lastMessage = application.getLastMessage(data);
                    if (chatChannel != null && lastMessage != null) {
                        MessageNotificationHelper.showNotification(this, chatChannel.getName(),
                                chatChannel.getLastMessage(), chatChannel.getId());
                    }
                }
            }
            break;
            case ApplicationClass.GROUP_DELETED: {
                if (data.equals(chatChannel.getId())) {
                    Toast.makeText(ConversationActivity.this, R.string.group_deleted,
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            case ApplicationClass.GROUP_RECEIVERS_CHANGED: {
                if (data.equals(chatChannel.getId())) {
                    downloadPhotos();
                }
            }
            break;
            default:
                break;
        }
    }

    @OnClick(R.id.bt_send)
    public void sendMessage() {
        Log.i(TAG, "sendMessage");
        String messageText = String.valueOf(editTextMessage.getText());
        if (messageText == null || messageText.isEmpty()) {
            return;
        }
        sendMessage(messageText);
    }

    @OnClick(R.id.btn_send_pick_photo)
    public void sendPickedPicture() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSIONS_GROUP_ONE);
            return;
        }
        Log.i(TAG, "sendPickedPicture");
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_PHOTO_REQUEST_CODE);
    }

    @OnClick(R.id.btn_send_taken_photo)
    public void sendTakenPicture() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSIONS_GROUP_ONE);
            return;
        }

        Log.i(TAG, "sendTakenPicture");
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photo = new File(Environment.getExternalStorageDirectory(),  "Pic.jpg");
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photo));
        imageUri = Uri.fromFile(photo);
        startActivityForResult(intent, TAKE_PHOTO_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_GROUP_ONE: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(ConversationActivity.this, "Permission was granted", Toast.LENGTH_LONG).show();

                }
            }
        }
    }

    private void setupRecyclerView() {
        Log.i(TAG, "setupRecyclerView");
        messageAdapter = new MessageAdapter(this, chatMessageList, photosMap);
        linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(messageAdapter);
    }

    private void downloadPhotos() {
        Log.i(TAG, "downloadPhotos");
        String[] receivers = application.getGroupReceivers(chatChannel.getId());
        if (receivers == null || receivers.length < 1) {
            return;
        }
        for (String receiver : application.getGroupReceivers(chatChannel.getId())) {
            usersRef.child(receiver).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Log.i(TAG, "downloadPhotos.onDataChane");
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null && user.getId() != null && user.getPhoto() != null) {
                        photosMap.put(user.getId(), user.getPhoto());
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    //left blank intentionally
                }
            });
        }
    }

    private void updateHistory() {
        Log.i(TAG, "updateHistory");
        chatMessageList.clear();
        chatMessageList.addAll(application.getHistory(chatChannel.getId()));
        linearLayoutManager.scrollToPosition(chatMessageList.size() - 1);
        messageAdapter.notifyDataSetChanged();
    }

    private void showAddMemberDialog() {
        Log.i(TAG, "showAddMemberDialog");
        final Dialog dialog = new Dialog(ConversationActivity.this);
        dialog.setContentView(R.layout.dialog_add_group_member);
        dialog.setTitle(add_members);

        Button buttonOk = (Button) dialog.findViewById(R.id.btn_add_group_members_ok);
        Button buttonCancel = (Button) dialog.findViewById(R.id.btn_add_group_members_cancel);
        ListView peopleListView = (ListView) dialog.findViewById(R.id.lv_add_group_members_list);

        Set<ChatChannel> channelsNearDevice = application.getChannels();
        String[] receivers = application.getGroupReceivers(chatChannel.getId());

        Iterator<ChatChannel> iter = channelsNearDevice.iterator();
        while (iter.hasNext()) {
            ChatChannel channel = iter.next();
            if (application.isGroup(channel.getId())) {
                iter.remove();
            } else {
                for (String receiver : receivers) {
                    if (channel.getId().equals(receiver)) {
                        iter.remove();
                        break;
                    }
                }
            }
        }

        List<GroupParticipant> possibleParticipants = new ArrayList<>();
        for (ChatChannel channel: channelsNearDevice) {
            possibleParticipants.add(new GroupParticipant(channel.getId(), channel.getName(), channel.getPhoto(), false));
        }
        final GroupParticipantsAdapter adapter = new GroupParticipantsAdapter(ConversationActivity.this,
                R.layout.dialog_group_list_item, possibleParticipants);
        peopleListView.setAdapter(adapter);

        buttonOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "addMemberDialog: buttonOk.onClick");
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
        Log.i(TAG, "addGroupMembers: " + selected);
        String groupId = chatChannel.getId();
        List<String> newParticipants = new ArrayList<>();
        Collections.addAll(newParticipants, application.getGroupReceivers(chatChannel.getId()));
        for (GroupParticipant participant : selected) {
            newParticipants.add(participant.getId());
        }
        application.deleteGroup(groupId, false);
        application.createGroup(groupId, newParticipants.toArray(new String[0]));
        Group updatedGroup = new Group(groupId, chatChannel.getName(), chatChannel.getPhoto(), newParticipants);
        groupsRef.child(groupId).setValue(updatedGroup);
    }

    private void showLeaveChannelDialog() {
        Log.i(TAG, "showLeaveChannelDialog");
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
        Log.i(TAG, "leaveGroup");
        String groupId = chatChannel.getId();
        List<String> newReceivers = new ArrayList<>();
        for (String receiver : application.getGroupReceivers(chatChannel.getId())) {
            if (!receiver.equals(application.getLoggedUser().getId())) {
                newReceivers.add(receiver);
            }
        }
        application.deleteGroup(groupId, true);
        if (newReceivers.size() < 3) {
            groupsRef.child(groupId).removeValue();
            application.groupDeletedAdvertise(groupId);
        } else {
            Group newGroup = new Group(groupId, chatChannel.getName(), chatChannel.getPhoto(), newReceivers);
            groupsRef.child(groupId).setValue(newGroup);
            application.groupReceiversChangedAdvertise(groupId, newReceivers.toArray(new String[0]));
        }
    }

    private void sendMessage(String text) {
        Log.i(TAG, "sendMessage");
        String sender = application.getLoggedUser().getId();
        String displayName = application.getLoggedUser().getName();
        String message;
        if (chatChannel.isGroup()) {
            message = ChatHelper.groupMessage(sender, displayName,
                    chatChannel.getId(), application.getGroupReceivers(chatChannel.getId()), text);
        } else {
            message = ChatHelper.oneToOneMessage(sender, displayName,
                    chatChannel.getId(), text);
        }
        application.sendChatMessage(message);
        editTextMessage.setText("");
        updateHistory();
    }

    private void openProfile() {
        Log.i(TAG, "openProfile: " + chatChannel.getId());
        String id = chatChannel.getId();
        String name = chatChannel.getName();
        Intent profileIntent = new Intent(this, UserProfileActivity.class);
        profileIntent.putExtra(UserProfileActivity.EXTRA_STRING_USER_ID, id);
        profileIntent.putExtra(UserProfileActivity.EXTRA_STRING_USER_NAME, name);
        startActivity(profileIntent);
    }

    private class SendPhotoAsyncTask extends AsyncTask<InputStream, Void, Void> {

        private String sender;
        private String displayName;
        private boolean isGroup;
        private String[] receivers;
        private String channelId;

        public SendPhotoAsyncTask(String sender, String displayName,
                                  boolean isGroup, String[] receivers, String channelId) {
            this.sender = sender;
            this.displayName = displayName;
            this.isGroup = isGroup;
            this.receivers = receivers;
            this.channelId = channelId;
        }

        @Override
        protected Void doInBackground(InputStream... inputStreams) {
            if (inputStreams != null) {
                InputStream is = inputStreams[0];
                Bitmap bmp = BitmapFactory.decodeStream(is);
                String base64 = ImageHelper.encodeBitmap(bmp);
                String text = ChatHelper.imageText(base64);
                sendMessage(text);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            updateHistory();
        }

        private void sendMessage(String text) {
            Log.i(TAG, "SendPhotoAsyncTask.sendMessage");
            String message;
            if (isGroup) {
                message = ChatHelper.groupMessage(sender, displayName, channelId, receivers, text);
            } else {
                message = ChatHelper.oneToOneMessage(sender, displayName, channelId, text);
            }
            application.sendChatMessage(message);
        }
    }

    private class SendTakenPhotoAsyncTask extends AsyncTask<Uri, Void, Void> {

        private String sender;
        private String displayName;
        private boolean isGroup;
        private String[] receivers;
        private String channelId;

        public SendTakenPhotoAsyncTask(String sender, String displayName,
                                       boolean isGroup, String[] receivers, String channelId) {
            this.sender = sender;
            this.displayName = displayName;
            this.isGroup = isGroup;
            this.receivers = receivers;
            this.channelId = channelId;
        }

        @Override
        protected Void doInBackground(Uri... uris) {
            if (uris != null) {
                Uri uri = uris[0];
                try {
                    Bitmap reducedSizeBitmap = ImageHelper.getScaledBitmap(ConversationActivity.this, uri.getPath());
                    if(reducedSizeBitmap != null) {
                        sendMessage(ChatHelper.imageText(ImageHelper.encodeBitmap(reducedSizeBitmap)));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        private void sendMessage(String text) {
            Log.i(TAG, "SendTakenPhotoAsyncTask.sendMessage");
            String message;
            if (isGroup) {
                message = ChatHelper.groupMessage(sender, displayName, channelId, receivers, text);
            } else {
                message = ChatHelper.oneToOneMessage(sender, displayName, channelId, text);
            }
            application.sendChatMessage(message);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            updateHistory();
        }
    }
}
