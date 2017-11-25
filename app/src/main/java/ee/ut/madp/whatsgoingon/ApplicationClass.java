package ee.ut.madp.whatsgoingon;

import android.app.Application;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.SignalEmitter;
import org.alljoyn.bus.Status;
import org.alljoyn.bus.annotation.BusSignalHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import ee.ut.madp.whatsgoingon.chat.ChatInterface;
import ee.ut.madp.whatsgoingon.chat.ControlInterface;
import ee.ut.madp.whatsgoingon.chat.Observable;
import ee.ut.madp.whatsgoingon.chat.Observer;
import ee.ut.madp.whatsgoingon.constants.FirebaseConstants;
import ee.ut.madp.whatsgoingon.constants.GeneralConstants;
import ee.ut.madp.whatsgoingon.database.IncomingMessagesDbHelper;
import ee.ut.madp.whatsgoingon.database.MessagesDbContract.IncomingMessagesTable;
import ee.ut.madp.whatsgoingon.database.MessagesDbContract.OutgoingMessagesTable;
import ee.ut.madp.whatsgoingon.database.OutgoingMessagesDbHelper;
import ee.ut.madp.whatsgoingon.helpers.ChatHelper;
import ee.ut.madp.whatsgoingon.helpers.MessageNotificationHelper;
import ee.ut.madp.whatsgoingon.models.ChatChannel;
import ee.ut.madp.whatsgoingon.models.ChatMessage;
import ee.ut.madp.whatsgoingon.models.Group;
import ee.ut.madp.whatsgoingon.models.User;

/**
 * Main class of application.
 * Contains all necessary work with bus objects, takes care of sending and receiving messages.
 *
 * Created by dominikf on 16. 10. 2017.
 */

public class ApplicationClass extends Application implements Observable, Observer {

    public static final String TAG = "chat.ChatApp";
    private FirebaseAuth firebaseAuth;
    private DatabaseReference firebaseGroupsRef;
    private DatabaseReference firebaseUsersRef;
    private IncomingMessagesDbHelper incDbHelper;
    private OutgoingMessagesDbHelper outDbHelper;
    private SQLiteDatabase incomingDb;
    private SQLiteDatabase outgoingDb;

    private Map<String, ChatChannel> channelsNearDevice;
    private Map<String, List<ChatMessage>> chatHistory;
    private Map<String, String[]> groupChatsReceiversMap;

    private Handler mBusChatHandler;
    private Handler mBusControlHandler;
    private Handler advertiseHandler;
    private Runnable advertiseCode;

    private static final int MESSAGE_CHAT = 1;
    private static final int MESSAGE_CONTROL = 2;

    public static final int GROUP_MESSAGE_RECEIVED = 1;
    public static final int ONE_TO_ONE_MESSAGE_RECEIVED = 2;
    public static final int GROUP_RECEIVERS_CHANGED = 3;
    public static final int GROUP_CHANNEL_DISCOVERED = 4;
    public static final int GROUP_DELETED = 5;
    public static final int USER_CHANNEL_DISCOVERED = 6;
    public static final int USER_CHANNEL_LEAVING = 7;

    private List<Observer> mObservers;
    public static boolean notificationsOn;
    public static boolean vibrateOn;
    public static User loggedUser;
    public static Uri ringtone;

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate");
        super.onCreate();
        HandlerThread busControlThread = new HandlerThread("BusControlHandler");
        busControlThread.start();
        HandlerThread busChatThread = new HandlerThread("BusChatHandler");
        busChatThread.start();

        mBusChatHandler = new Handler(busChatThread.getLooper(), new ChatBusHandlerCallback());
        mBusChatHandler.sendEmptyMessage(ChatBusHandlerCallback.CONNECT);

        mBusControlHandler = new Handler(busControlThread.getLooper(), new ControlBusHandlerCallback());
        mBusControlHandler.sendEmptyMessage(ControlBusHandlerCallback.CONNECT);

        SharedPreferences prefs = getSharedPreferences("setting.whatsgoingon", Context.MODE_PRIVATE);
        notificationsOn = prefs.getBoolean(GeneralConstants.PREF_MESSAGE_NOTIFICATION, true);
        vibrateOn = prefs.getBoolean(GeneralConstants.PREF_NOTIFICATION_VIBRATE, true);
        ringtone = Uri.parse(prefs.getString(GeneralConstants.PREF_NOTIFICATION_RINGTONE, "DEFAULT_SOUND"))   ;
    }

    @Override
    public void onTerminate() {
        Log.i(TAG, "onTerminate");
        super.onTerminate();
        stopAdvertise();

        incomingDb.close();
        outgoingDb.close();
        incDbHelper.close();
        outDbHelper.close();

        mBusChatHandler.sendEmptyMessage(ChatBusHandlerCallback.DISCONNECT);
        mBusControlHandler.sendEmptyMessage(ControlBusHandlerCallback.DISCONNECT);
    }

    /**
     * Performs basic initialization of application.
     */
    public void checkIn() {
        Log.i(TAG, "checkIn");
        FirebaseApp.initializeApp(this);
        firebaseGroupsRef = FirebaseDatabase.getInstance()
                .getReference().child(FirebaseConstants.FIREBASE_CHILD_GROUPS);
        firebaseUsersRef = FirebaseDatabase.getInstance()
                .getReference().child(FirebaseConstants.FIREBASE_CHILD_USERS);

        MessageNotificationHelper.setManager((NotificationManager) getSystemService(NOTIFICATION_SERVICE));

        chatHistory = new ConcurrentHashMap<>();
        groupChatsReceiversMap = new ConcurrentHashMap<>();
        firebaseAuth = FirebaseAuth.getInstance();
        mObservers = new ArrayList<>();
        channelsNearDevice = new HashMap<>();

        incDbHelper = new IncomingMessagesDbHelper(getApplicationContext());
        outDbHelper = new OutgoingMessagesDbHelper(getApplicationContext());
        incomingDb = incDbHelper.getWritableDatabase();
        outgoingDb = outDbHelper.getWritableDatabase();
        try {
            Map<String, List<ChatMessage>> readMessages = new ReadMessagesFromDb().execute().get();
            for (String key: readMessages.keySet()) {
                List<ChatMessage> messages = readMessages.get(key);
                if (chatHistory.containsKey(key)) {
                    chatHistory.get(key).addAll(messages);
                } else {
                    chatHistory.put(key, messages);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        setUpAdvertising();
    }

    private class ReadMessagesFromDb extends AsyncTask<Void, Void, Map<String, List<ChatMessage>>> {
        private String incomingTable = IncomingMessagesTable.TABLE_NAME;
        private String outgoingTable = OutgoingMessagesTable.TABLE_NAME;
        private String loggedUserId = ApplicationClass.loggedUser.getId();

        @Override
        protected Map<String, List<ChatMessage>> doInBackground(Void... voids) {
            Log.i(TAG, "ReadMessagesFromDb.doInBackground");
            Map<String, List<ChatMessage>> readMessages = new HashMap<>();
            String[] incomingProjection = getIncomingProjection();
            String[] outgoingProjection = getOutgoingProjection();
            readIncomingDb(readMessages, incomingProjection);
            readOutgoingDb(readMessages, outgoingProjection);
            return readMessages;
        }

        private void readIncomingDb(Map<String, List<ChatMessage>> readMessages, String[] projection) {
            Log.i(TAG, "ReadMessagesFromDb.readIncomingDb");
            String selection = IncomingMessagesTable.COLUMN_NAME_LOGGED_USER + " = ?";
            String[] selectionArgs = { loggedUserId };
            Cursor incomingCursor = incomingDb.query(incomingTable, projection, selection, selectionArgs, null, null, null);

            int senderIndex = incomingCursor.getColumnIndex(IncomingMessagesTable.COLUMN_NAME_SENDER);
            int senderDisplIndex = incomingCursor.getColumnIndex(IncomingMessagesTable.COLUMN_NAME_SENDER_DISPL_NAME);
            int gidIndex = incomingCursor.getColumnIndex(IncomingMessagesTable.COLUMN_NAME_GID);
            int textIndex = incomingCursor.getColumnIndex(IncomingMessagesTable.COLUMN_NAME_TEXT);
            int timeIndex = incomingCursor.getColumnIndex(IncomingMessagesTable.COLUMN_NAME_TIME);

            while (incomingCursor.moveToNext()) {
                String sender = incomingCursor.getString(senderIndex);
                String senderDispl = incomingCursor.getString(senderDisplIndex);
                String gid = incomingCursor.getString(gidIndex);
                String text = incomingCursor.getString(textIndex);
                long time = incomingCursor.getLong(timeIndex);
                ChatMessage msg = new ChatMessage(text, senderDispl, sender, false, time);
                if (gid != null) {
                    if (readMessages.containsKey(gid)) {
                        readMessages.get(gid).add(msg);
                    } else {
                        readMessages.put(gid, new ArrayList<ChatMessage>());
                        readMessages.get(gid).add(msg);
                    }
                } else {
                    if (readMessages.containsKey(sender)) {
                        readMessages.get(sender).add(msg);
                    } else {
                        readMessages.put(sender, new ArrayList<ChatMessage>());
                        readMessages.get(sender).add(msg);
                    }
                }
            }
            incomingCursor.close();
        }

        private void readOutgoingDb(Map<String, List<ChatMessage>> readMessages, String[] projection) {
            Log.i(TAG, "ReadMessagesFromDb.readOutgoingDb");
            String selection = OutgoingMessagesTable.COLUMN_NAME_LOGGED_USER + " = ?";
            String[] selectionArgs = { loggedUserId };
            Cursor outgoingCursor = outgoingDb.query(outgoingTable, projection, selection, selectionArgs, null, null, null);

            int receiverIndex = outgoingCursor.getColumnIndex(OutgoingMessagesTable.COLUMN_NAME_RECEIVER);
            int gidIndex = outgoingCursor.getColumnIndex(OutgoingMessagesTable.COLUMN_NAME_GID);
            int textIndex = outgoingCursor.getColumnIndex(OutgoingMessagesTable.COLUMN_NAME_TEXT);
            int timeIndex = outgoingCursor.getColumnIndex(OutgoingMessagesTable.COLUMN_NAME_TIME);

            while (outgoingCursor.moveToNext()) {
                String receiver = outgoingCursor.getString(receiverIndex);
                String gid = outgoingCursor.getString(gidIndex);
                String text = outgoingCursor.getString(textIndex);
                long time = outgoingCursor.getLong(timeIndex);
                ChatMessage msg = new ChatMessage(text, loggedUser.getName(), loggedUser.getId(), true, time);
                if (gid != null) {
                    if (readMessages.containsKey(gid)) {
                        readMessages.get(gid).add(msg);
                    } else {
                        readMessages.put(gid, new ArrayList<ChatMessage>());
                        readMessages.get(gid).add(msg);
                    }
                } else {
                    if (readMessages.containsKey(receiver)) {
                        readMessages.get(receiver).add(msg);
                    } else {
                        readMessages.put(receiver, new ArrayList<ChatMessage>());
                        readMessages.get(receiver).add(msg);
                    }
                }
            }
            outgoingCursor.close();
        }

        public String[] getIncomingProjection() {
            String[] incomingProjection = new String[] {
                    IncomingMessagesTable.COLUMN_NAME_SENDER,
                    IncomingMessagesTable.COLUMN_NAME_SENDER_DISPL_NAME,
                    IncomingMessagesTable.COLUMN_NAME_GID,
                    IncomingMessagesTable.COLUMN_NAME_TEXT,
                    IncomingMessagesTable.COLUMN_NAME_TIME
            };
            return incomingProjection;
        }

        public String[] getOutgoingProjection() {
            String[] outgoingProjection = new String[]{
                    OutgoingMessagesTable.COLUMN_NAME_RECEIVER,
                    OutgoingMessagesTable.COLUMN_NAME_GID,
                    OutgoingMessagesTable.COLUMN_NAME_TEXT,
                    OutgoingMessagesTable.COLUMN_NAME_TIME
            };
            return outgoingProjection;
        }
    }

    /**
     * Set up all necessary components for advertising the device among others.
     */
    private void setUpAdvertising() {
        Log.i(TAG, "setUpAdvertising");
        advertiseHandler = new Handler();
        advertiseCode = new Runnable() {
            @Override
            public void run() {
                FirebaseUser currentUser = firebaseAuth.getCurrentUser();
                if (currentUser == null) {
                    return;
                }

                Message message = mBusControlHandler.obtainMessage(ControlBusHandlerCallback.CONTROL,
                        ChatHelper.advertiseMessage(currentUser.getUid()));
                mBusControlHandler.sendMessage(message);

                advertiseHandler.postDelayed(this, 10000);
            }
        };
    }

    public static Uri getRingtone() {
        Log.i(TAG, "getRingtone");
        return ringtone;
    }

    public static void setRingtone(String ringtoneName) {
        Log.i(TAG, "setRingtone");
        ringtone = Uri.parse(ringtoneName);
    }

    /**
     * Starts advertisement process
     */
    public synchronized void startAdvertise() {
        Log.i(TAG, "startAdvertise");
        advertiseHandler.post(advertiseCode);
    }

    /**
     * Stops advertisement process
     */
    public synchronized void stopAdvertise() {
        Log.i(TAG, "stopAdvertise");
        sendChatMessage(ChatHelper.cancelAdvertiseMessage(loggedUser.getId()));
        advertiseHandler.removeCallbacks(advertiseCode);
        channelsNearDevice = new ConcurrentHashMap<>();
        chatHistory = new ConcurrentHashMap<>();
        groupChatsReceiversMap = new ConcurrentHashMap<>();
    }

    //***** HANDLERS *****\\

    /**
     * Handler for chat messages
     */
    private Handler mChatHandler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            Log.i(TAG, "mChatHandler.handleMessage: " + msg);
            if (firebaseAuth.getCurrentUser() != null && msg.what == MESSAGE_CHAT) {
                String receivedMsg = (String) msg.obj;
                String messageType = ChatHelper.getMessageType(receivedMsg);
                switch (messageType) {
                    case ChatHelper.ONE_TO_ONE_MESSAGE: {
                        dealWithOneToOneMessage(receivedMsg);
                    } break;
                    case ChatHelper.GROUP_MESSAGE: {
                        dealWithGroupMessage(receivedMsg);
                    } break;
                    default:
                        break;
                }
                return true;
            }
            return false;
        }
    });

    /**
     * Handler for control messages
     */
    private Handler mControlHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            Log.i(TAG, "mControlHandler.handleMessage: " + message);
            if (firebaseAuth.getCurrentUser() != null && message.what == MESSAGE_CONTROL) {
                String receivedMsg = (String) message.obj;
                String messageType = ChatHelper.getMessageType(receivedMsg);
                switch (messageType) {
                    case ChatHelper.ADVERTISE_MESSAGE: {
                        dealWithAdvertiseMessage(receivedMsg);
                    }
                    break;
                    case ChatHelper.GROUP_ADVERTISE_MESSAGE: {
                        dealWithGroupAdvertiseMessage(receivedMsg);
                    }
                    break;
                    case ChatHelper.CANCEL_ADVERTISE_MESSAGE: {
                        dealWithCancelAdvertiseMessage(receivedMsg);
                    }
                    break;
                    case ChatHelper.GROUP_DELETED_MESSAGE: {
                        dealWithGroupDeletedMessage(receivedMsg);
                    }
                    break;
                    case ChatHelper.GROUP_RECEIVERS_CHANGED_MESSAGE: {
                        dealWithGroupReceiversChanged(receivedMsg);
                    } break;
                    default:
                        break;
                }
            }
            return true;
        }
    });

    //***** MESSAGES METHODS *****\\

    private void dealWithGroupDeletedMessage(String receivedMsg) {
        Log.i(TAG, "dealWithGroupDeletedMessage");
        String gid = ChatHelper.groupDeletedGid(receivedMsg);
        notifyObservers(GROUP_DELETED, gid);
        deleteGroup(gid, true);
    }

    private void dealWithGroupReceiversChanged(String receivedMsg) {
        Log.i(TAG, "dealWithGroupReceiversChanged");
        String gid = ChatHelper.groupReceiversChangedGid(receivedMsg);
        String[] newReceivers = ChatHelper.groupReceiversChangedReceivers(receivedMsg);
        groupChatsReceiversMap.put(gid, newReceivers);
        notifyObservers(GROUP_RECEIVERS_CHANGED, gid);
    }

    private void dealWithCancelAdvertiseMessage(String receivedMsg) {
        Log.i(TAG, "dealWithCancelAdvertiseMessage");
        String sender = ChatHelper.cancelAdvertiseMessageSender(receivedMsg);
        if (loggedUser == null || sender.equals(loggedUser.getId())) {
            return;
        }
        String channelId = ChatHelper.cancelAdvertiseMessageSender(receivedMsg);
        notifyObservers(USER_CHANNEL_LEAVING, channelId);
    }

    private void dealWithGroupAdvertiseMessage(final String receivedMsg) {
        Log.i(TAG, "dealWithGroupAdvertiseMessage");
        String gid = ChatHelper.groupAdvertiseMessageId(receivedMsg);
        if (channelsNearDevice.containsKey(gid)) {
            return;
        }

        String[] receivers = ChatHelper.groupAdvertiseMessageReceivers(receivedMsg);
        boolean found = false;
        if (loggedUser == null) {
            return;
        }

        for (String receiver: receivers) {
            if (receiver.equals(loggedUser.getId())) {
                found = true;
                break;
            }
        }

        if (!found) {
            return;
        }

        firebaseGroupsRef.child(gid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.i(TAG, "dealWithGroupAdvertiseMessage.onDataChange");
                Group group = dataSnapshot.getValue(Group.class);
                if (group == null) {
                    return;
                }
                ChatChannel newGroupChannel = new ChatChannel(group.getId(), group.getDisplayName(),
                        group.getPhoto(), true);
                channelsNearDevice.put(newGroupChannel.getId(), newGroupChannel);
                groupChatsReceiversMap.put(newGroupChannel.getId(),
                        group.getReceivers().toArray(new String[0]));
                if (! chatHistory.containsKey(newGroupChannel.getId())) {
                    chatHistory.put(newGroupChannel.getId(), new ArrayList<ChatMessage>());
                }
                notifyObservers(GROUP_CHANNEL_DISCOVERED, group.getId());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                //left blank intentionally
            }
        });
    }

    private void dealWithAdvertiseMessage(String receivedMsg) {
        Log.i(TAG, "dealWithAdvertiseMessage");
        String uid = ChatHelper.advertiseMessageDisplayName(receivedMsg);
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null ||
                currentUser.getUid().equals(uid) || channelsNearDevice.containsKey(uid)) {
            return;
        }

        firebaseUsersRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.i(TAG, "dealWithAdvertiseMessage.onDataChange");
                User user = dataSnapshot.getValue(User.class);
                if (user == null) {
                    return;
                }
                ChatChannel newUserChannel = new ChatChannel(user.getId(), user.getName(), user.getPhoto(), false);
                channelsNearDevice.put(user.getId(), newUserChannel);
                if (! chatHistory.containsKey(user.getId())) {
                    chatHistory.put(user.getId(), new ArrayList<ChatMessage>());
                }
                notifyObservers(USER_CHANNEL_DISCOVERED, user.getId());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                //left blank intentionally
            }
        });
    }

    private void dealWithGroupMessage(String receivedMsg) {
        Log.i(TAG, "dealWithGroupMessage");
        String[] messageReceivers = ChatHelper.groupMessageReceivers(receivedMsg);
        String sender = ChatHelper.groupMessageSender(receivedMsg);
        if (loggedUser == null || sender.equals(loggedUser.getId())) {
            return;
        }

        boolean found = false;
        for (String messageReceiver : messageReceivers) {
            if (messageReceiver.equals(loggedUser.getId())) {
                found = true;
                break;
            }
        }
        if (!found) {
            return;
        }
        storeGroupMessage(receivedMsg, true);
        String gid = ChatHelper.groupMessageGID(receivedMsg);
        ChatChannel chatChannel = getChannel(gid);
        if (chatChannel != null) {
            chatChannel.setNewMessage(true);
        }
        notifyObservers(GROUP_MESSAGE_RECEIVED, gid);
    }

    private void storeGroupMessage(String receivedMsg, boolean incoming) {
        Log.i(TAG, "storeGroupMessage: " + incoming);
        String group = ChatHelper.groupMessageGID(receivedMsg);
        if (! chatHistory.containsKey(group)) {
            chatHistory.put(group, new ArrayList<ChatMessage>());
            String[] receivers = ChatHelper.groupMessageReceivers(receivedMsg);
            groupChatsReceiversMap.put(group, receivers);
        }
        String text = ChatHelper.groupMessageText(receivedMsg);
        String sender = ChatHelper.groupMessageSender(receivedMsg);
        String displayName = ChatHelper.groupMessageSenderDisplayName(receivedMsg);
        List<ChatMessage> hist = chatHistory.get(group);

        ChatMessage newMessage = new ChatMessage(text, displayName, sender,
                loggedUser.getId().equals(sender));
        if (incoming) {
            storeIncomingMessage(sender, displayName, group, text, newMessage.getMessageTime());
        } else {
            storeOutgoingMessage(null, group, text, newMessage.getMessageTime());
        }
        hist.add(newMessage);
    }

    private void dealWithOneToOneMessage(String receivedMsg) {
        Log.i(TAG, "dealWithOneToOneMessage");
        String messageReceiver = ChatHelper.oneToOneMessageReceiver(receivedMsg);
        String sender = ChatHelper.oneToOneMessageSender(receivedMsg);
        if (loggedUser == null || sender.equals(loggedUser.getId())) {
            return;
        } else if (!messageReceiver.equals(loggedUser.getId())) {
            return;
        }
        storeOneToOneMessage(receivedMsg);
        ChatChannel chatChannel = getChannel(sender);
        if (chatChannel != null) {
            chatChannel.setNewMessage(true);
        }
        notifyObservers(ONE_TO_ONE_MESSAGE_RECEIVED, sender);
    }

    private void storeOneToOneMessage(String receivedMsg) {
        Log.i(TAG, "storeOneToOneMessage");
        String sender = ChatHelper.oneToOneMessageSender(receivedMsg);
        if (! chatHistory.containsKey(sender)) {
            chatHistory.put(sender, new ArrayList<ChatMessage>());
        }
        String text = ChatHelper.oneToOneMessageText(receivedMsg);
        String senderName = ChatHelper.oneToOneMessageSenderDisplayName(receivedMsg);
        List<ChatMessage> hist = chatHistory.get(sender);

        ChatMessage newMessage = new ChatMessage(text, senderName, sender,
                loggedUser.getId().equals(sender));
        storeIncomingMessage(sender, senderName, null, text, newMessage.getMessageTime());
        hist.add(newMessage);
    }

    private class StoreIncomingMessageAsyncTask extends AsyncTask<ContentValues, Void, Long> {

        private String tableName = IncomingMessagesTable.TABLE_NAME;

        @Override
        protected Long doInBackground(ContentValues... contentValues) {
            Log.i(TAG, "StoreIncomingMessageAsyncTask.doInBackground");
            ContentValues values = contentValues[0];
            return incomingDb.insert(tableName, null, values);
        }
    }

    private class StoreOutgoingMessageAsyncTask extends AsyncTask<ContentValues, Void, Long> {

        private String tableName = OutgoingMessagesTable.TABLE_NAME;

        @Override
        protected Long doInBackground(ContentValues... contentValues) {
            Log.i(TAG, "StoreOutgoingMessageAsyncTask.doInBackground");
            ContentValues values = contentValues[0];
            return outgoingDb.insert(tableName, null, values);
        }
    }

    private void storeIncomingMessage(String sender, String senderDisplName, String gid, String text, long time) {
        Log.i(TAG, "storeIncomingMessage");
        ContentValues values = new ContentValues();
        values.put(IncomingMessagesTable.COLUMN_NAME_LOGGED_USER, loggedUser.getId());
        values.put(IncomingMessagesTable.COLUMN_NAME_SENDER, sender);
        values.put(IncomingMessagesTable.COLUMN_NAME_SENDER_DISPL_NAME, senderDisplName);
        values.put(IncomingMessagesTable.COLUMN_NAME_GID, gid);
        values.put(IncomingMessagesTable.COLUMN_NAME_TEXT, text);
        values.put(IncomingMessagesTable.COLUMN_NAME_TIME, time);
        new StoreIncomingMessageAsyncTask().execute(values);
    }

    private void storeOutgoingMessage(String receiver, String gid, String text, long time) {
        Log.i(TAG, "storeOutgoingMessage");
        ContentValues values = new ContentValues();
        values.put(OutgoingMessagesTable.COLUMN_NAME_LOGGED_USER, loggedUser.getId());
        values.put(OutgoingMessagesTable.COLUMN_NAME_RECEIVER, receiver);
        values.put(OutgoingMessagesTable.COLUMN_NAME_GID, gid);
        values.put(OutgoingMessagesTable.COLUMN_NAME_TEXT, text);
        values.put(OutgoingMessagesTable.COLUMN_NAME_TIME, time);
        new StoreOutgoingMessageAsyncTask().execute(values);
    }

    /**
     * Get last message of specified channel
     * @param channelId specifies channel
     * @return last message, null if no messages were found
     */
    public ChatMessage getLastMessage(String channelId) {
        Log.i(TAG, "getLastMessage");
        List<ChatMessage> chatMessages = chatHistory.get(channelId);
        if (chatMessages != null) {
            int size = chatMessages.size();
            if (size > 0) {
                return chatMessages.get(size - 1);
            }
        }
        return null;
    }

    /**
     * Method for sending chat messages
     * @param message message to be sent
     */
    public void sendChatMessage(String message) {
        Log.i(TAG, "sendChatMessage");
        String type = ChatHelper.getMessageType(message);
        switch (type) {
            case "S": {
                String channel = ChatHelper.oneToOneMessageReceiver(message);
                String name = ChatHelper.oneToOneMessageSenderDisplayName(message);
                String text = ChatHelper.oneToOneMessageText(message);
                if (! chatHistory.containsKey(channel)) {
                    chatHistory.put(channel, new ArrayList<ChatMessage>());
                }
                List<ChatMessage> hist = chatHistory.get(channel);
                ChatMessage newMessage = new ChatMessage(text, name, channel, true);
                storeOutgoingMessage(channel, null, text, newMessage.getMessageTime());
                hist.add(newMessage);
            } break;
            case "G": {
                storeGroupMessage(message, false);
            } break;
        }
        Message msg = mBusChatHandler.obtainMessage(ChatBusHandlerCallback.CHAT, message);
        mBusChatHandler.sendMessage(msg);
    }

    /**
     * Method for sending control messages
     * @param message message to be sent
     */
    public void sendControlMessage(String message) {
        Log.i(TAG, "sendControlMessage");
        Message msg = mBusControlHandler.obtainMessage(ControlBusHandlerCallback.CONTROL, message);
        mBusControlHandler.sendMessage(msg);
    }

    public synchronized List<ChatMessage> getHistory(String key) {
        Log.i(TAG, "getHistory");
        List<ChatMessage> orig = chatHistory.get(key);
        List<ChatMessage> copy = new ArrayList<>();
        for (ChatMessage message: orig) {
            copy.add(message);
        }
        return copy;
    }

    //***** CHANNELS METHODS *****\\

    public boolean isGroup(String gid) {
        Log.i(TAG, "isGroup");
        return groupChatsReceiversMap.containsKey(gid);
    }

    public synchronized Set<ChatChannel> getChannels() {
        Log.i(TAG, "getChannels");
        Set<ChatChannel> channelsCopy = new HashSet<>();
        channelsCopy.addAll(channelsNearDevice.values());
        return channelsCopy;
    }

    public ChatChannel getChannel(String channelId) {
        Log.i(TAG, "getChannel");
        return channelsNearDevice.get(channelId);
    }

    //***** GROUPS METHODS *****\\

    public void createGroup(String gId, String[] receivers) {
        Log.i(TAG, "createGroup");
        sendControlMessage(ChatHelper.groupAdvertiseMessage(gId, receivers));
    }

    public synchronized void deleteGroup(String groupId, boolean onlyDelete) {
        Log.i(TAG, "deleteGroup");
        Iterator<Map.Entry<String,List<ChatMessage>>> iter1 = chatHistory.entrySet().iterator();
        while (iter1.hasNext()) {
            Map.Entry<String, List<ChatMessage>> entry = iter1.next();
            if(groupId.equals(entry.getKey())){
                iter1.remove();
                break;
            }
        }
        Iterator<Map.Entry<String, ChatChannel>> iter2 = channelsNearDevice.entrySet().iterator();
        while (iter2.hasNext()) {
            Map.Entry<String, ChatChannel> entry = iter2.next();
            if (groupId.equals(entry.getKey())) {
                iter2.remove();
                break;
            }
        }
        Iterator<Map.Entry<String, String[]>> iter3 = groupChatsReceiversMap.entrySet().iterator();
        while (iter3.hasNext()) {
            Map.Entry<String, String[]> entry = iter3.next();
            if (groupId.equals(entry.getKey())) {
                iter3.remove();
                break;
            }
        }

        if (onlyDelete) {
            notifyObservers(GROUP_DELETED, groupId);
            new DeleteGroupFromDb().execute(groupId);
        }
    }

    private class DeleteGroupFromDb extends AsyncTask<String, Void, Void> {
        private String incomingTable = IncomingMessagesTable.TABLE_NAME;
        private String outgoingTable = OutgoingMessagesTable.TABLE_NAME;

        @Override
        protected Void doInBackground(String... strings) {
            Log.i(TAG, "DeleteGroupFromDb.doInBackground");
            String gid = strings[0];
            String incomingSelection = IncomingMessagesTable.COLUMN_NAME_GID + " = ?";
            String outgoingSelection = OutgoingMessagesTable.COLUMN_NAME_GID + " = ?";
            incomingDb.delete(incomingTable, incomingSelection, new String[] { gid });
            outgoingDb.delete(outgoingTable, outgoingSelection, new String[] { gid });
            return null;
        }
    }

    public synchronized String[] getGroupReceivers(String gid) {
        Log.i(TAG, "getGroupReceivers");
        return groupChatsReceiversMap.get(gid);
    }

    @Override
    public synchronized void addObserver(Observer obs) {
        Log.i(TAG, "addObserver");
        if (mObservers.contains(this)) {
            mObservers.remove(this);
        }
        mObservers.add(obs);
    }

    @Override
    public synchronized void deleteObserver(Observer obs) {
        Log.i(TAG, "deleteObserver");
        mObservers.remove(obs);
        if (mObservers.size() == 0) {
            mObservers.add(this);
        }
    }

    @Override
    public void update(Observable o, int qualifier, String data) {
        Log.i(TAG, "update");
        switch (qualifier) {
            case ApplicationClass.ONE_TO_ONE_MESSAGE_RECEIVED:
            case ApplicationClass.GROUP_MESSAGE_RECEIVED: {
                ChatChannel chatChannel = getChannel(data);
                ChatMessage lastMessage = getLastMessage(data);
                if (chatChannel != null && lastMessage != null) {
                    MessageNotificationHelper.showNotification(this, chatChannel.getName(),
                            chatChannel.getLastMessage(), chatChannel.getId());
                }
            } break;
        }
    }

    private void notifyObservers(int arg, String data) {
        Log.i(TAG, "notifyObservers");
        for (Observer obs : mObservers) {
            obs.update(this, arg, data);
        }
    }

    public User getLoggedUser() {
        Log.i(TAG, "getLoggedUser");
        return loggedUser;
    }

    public void setLoggedUser(User loggedUser) {
        Log.i(TAG, "setLoggedUser");
        ApplicationClass.loggedUser = loggedUser;
    }

    public void groupReceiversChangedAdvertise(String gid, String[] receivers) {
        Log.i(TAG, "groupReceiversChangedAdvertise");
        String message = ChatHelper.groupReceiversChanged(gid, receivers);
        sendControlMessage(message);
    }

    public void groupDeletedAdvertise(String gid) {
        Log.i(TAG, "groupDeletedAdvertise");
        String message = ChatHelper.groupDeleted(gid);
        sendControlMessage(message);
    }

    private class ControlService implements ControlInterface, BusObject {
        private BusAttachment bus;

        public ControlService(BusAttachment bus) {
            this.bus = bus;
        }

        @BusSignalHandler(iface = "ee.ut.madp.whatisgoingon.control", signal = "control")
        public void control(String message) throws BusException {
            Log.i(TAG, "Signal : " + message);
            sendControlMessage(MESSAGE_CONTROL, message);
        }

        private void sendControlMessage(int what, Object obj) {
            mControlHandler.sendMessage(mControlHandler.obtainMessage(what, obj));
        }
    }

    private class ChatService implements ChatInterface, BusObject {

        private final String TAG = ChatService.class.getSimpleName();
        private BusAttachment bus;

        public ChatService(BusAttachment bus) {
            this.bus = bus;
        }

        @BusSignalHandler(iface = "ee.ut.madp.whatisgoingon.chat", signal = "chat")
        public void chat(String message) {
            Log.i(TAG, "Signal  : " + message);
            sendMessage(MESSAGE_CHAT, message);
        }

        private void sendMessage(int what, Object obj) {
            mChatHandler.sendMessage(mChatHandler.obtainMessage(what, obj));
        }
    }

    private class ControlBusHandlerCallback implements Handler.Callback {

        private final String TAG = ControlBusHandlerCallback.class.getSimpleName();

        private BusAttachment mBus;
        private SignalEmitter emitter;
        private ControlInterface mControlInterface = null;
        private ControlService mControlService = null;

        private static final int CONNECT = 1;
        private static final int DISCONNECT = 2;
        private static final int CONTROL = 3;

        @Override
        public boolean handleMessage(Message msg) {
            Log.i(TAG, "ControlBusHandlerCallback.handleMessage: " + msg);
            switch (msg.what) {
                case CONNECT: {
                    connectToBus();
                    break;
                } case DISCONNECT: {
                    disconnectFromBus();
                    break;
                } case CONTROL: {
                    dealWithControl(msg);
                    break;
                }
                default:
                    break;
            }
            return true;
        }

        private void dealWithControl(Message msg) {
            Log.i(TAG, "dealWithControl: " + msg);
            try {
                if (emitter == null) {
                    emitter = new SignalEmitter(mControlService, 0, SignalEmitter.GlobalBroadcast.Off);
                    emitter.setSessionlessFlag(true);
                    mControlInterface = emitter.getInterface(ControlInterface.class);
                }
                if (mControlInterface != null) {
                    String message = (String) msg.obj;
                    Log.i(TAG, "Sending message " + msg);
                    mControlInterface.control(message);
                }
            } catch (BusException ex) {
                Log.w(TAG, ex);
            }
        }

        private void disconnectFromBus() {
            Log.i(TAG, "disconnectFromBus");
            mBus.disconnect();
            mBusChatHandler.getLooper().quit();
        }

        private void connectToBus() {
            Log.i(TAG, "connectToBus");
            org.alljoyn.bus.alljoyn.DaemonInit.PrepareDaemon(getApplicationContext());
            mBus = new BusAttachment(getPackageName(), BusAttachment.RemoteMessage.Receive);
            mControlService = new ControlService(mBus);
            Status status = mBus.registerBusObject(mControlService, "/ControlService");
            if (Status.OK != status) {
                Log.i(TAG + ".RegBus()", status.toString());
                return;
            }
            status = mBus.connect();
            Log.i(TAG, status.toString());
            if (status != Status.OK) {
                Log.i(TAG + ".BusCon()", "BusAtt");
                return;
            }
            status = mBus.registerSignalHandlers(mControlService);
            if (status != Status.OK) {
                Log.i(TAG, "Problem while registering signal handler");
                return;
            }
            status = mBus.addMatch("sessionless='t'");
            if (status == Status.OK) {
                Log.i(TAG, "AddMatch was called successfully");
            }
        }
    }

    private class ChatBusHandlerCallback implements Handler.Callback {

        private final String TAG = ChatBusHandlerCallback.class.getSimpleName();

        private BusAttachment mBus;
        private SignalEmitter emitter;
        private ChatInterface mChatInterface = null;
        private ChatService mChatService = null;

        private static final int CONNECT = 1;
        private static final int DISCONNECT = 2;
        private static final int CHAT = 3;

        @Override
        public boolean handleMessage(Message msg) {
            Log.i(TAG, "handleMessage: " + msg);
            switch (msg.what) {
                case CONNECT: {
                    connectToBus();
                    break;
                } case DISCONNECT: {
                    disconnectFromBus();
                    break;
                } case CHAT: {
                    dealWithChatMessage(msg);
                    break;
                }
                default:
                    break;
            }
            return true;
        }

        private void dealWithChatMessage(Message msg) {
            Log.i(TAG, "dealWithChatMessage");
            try {
                if (emitter == null) {
                    emitter = new SignalEmitter(mChatService, 0, SignalEmitter.GlobalBroadcast.Off);
                    emitter.setSessionlessFlag(true);
                    mChatInterface = emitter.getInterface(ChatInterface.class);
                }
                if (mChatInterface != null) {
                    String message = (String) msg.obj;
                    Log.i(TAG, "Sending message " + msg);
                    mChatInterface.chat(message);
                }
            } catch (BusException ex) {
                Log.w(TAG, ex);
            }
        }

        private void disconnectFromBus() {
            Log.i(TAG, "disconnectFromBus");
            mBus.disconnect();
            mBusChatHandler.getLooper().quit();
        }

        private void connectToBus() {
            Log.i(TAG, "connectToBus");
            org.alljoyn.bus.alljoyn.DaemonInit.PrepareDaemon(getApplicationContext());
            mBus = new BusAttachment(getPackageName(), BusAttachment.RemoteMessage.Receive);
            mChatService = new ChatService(mBus);
            Status status = mBus.registerBusObject(mChatService, "/ChatService");
            if (Status.OK != status) {
                Log.i(TAG + ".RegBus()", status.toString());
                return;
            }
            status = mBus.connect();
            Log.i(TAG, status.toString());
            if (status != Status.OK) {
                Log.i(TAG + ".BusCon()", "BusAtt");
                return;
            }
            status = mBus.registerSignalHandlers(mChatService);
            if (status != Status.OK) {
                Log.i(TAG, "Problem while registering signal handler");
                return;
            }
            status = mBus.addMatch("sessionless='t'");
            if (status == Status.OK) {
                Log.i(TAG, "AddMatch was called successfully");
            }
        }
    }

    /* Load the native alljoyn_java library. */
    static {
        Log.i(TAG, "loadAllJoyn_java");
        System.loadLibrary("alljoyn_java");
    }
}
