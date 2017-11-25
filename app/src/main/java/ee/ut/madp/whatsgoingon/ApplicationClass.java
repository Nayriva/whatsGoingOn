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
            Map<String, List<ChatMessage>> readMessages = new HashMap<>();
            String[] incomingProjection = getIncomingProjection();
            String[] outgoingProjection = getOutgoingProjection();
            readIncomingDb(readMessages, incomingProjection);
            readOutgoingDb(readMessages, outgoingProjection);
            return readMessages;
        }

        private void readIncomingDb(Map<String, List<ChatMessage>> readMessages, String[] projection) {
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

                message = mBusControlHandler.obtainMessage(ControlBusHandlerCallback.CONTROL,
                        ChatHelper.advertiseMessage("XPhFr2Fd12Z3QtI5j1VWlaExIOF3"));
                mBusControlHandler.sendMessage(message);

                message = mBusControlHandler.obtainMessage(ControlBusHandlerCallback.CONTROL,
                        ChatHelper.advertiseMessage("a5S16xVoXHbwd2IBVBzs8WXSiKG3"));
                mBusControlHandler.sendMessage(message);
                //1-2-1 test
//                message = mBusChatHandler.obtainMessage(ChatBusHandlerCallback.CHAT,
//                        ChatHelper.oneToOneMessage("a5S16xVoXHbwd2IBVBzs8WXSiKG3", "Petra Cendelínová",
//                                loggedUser.getId(), "Test message 1_2_1" + new Random().nextInt()));
//                mBusChatHandler.sendMessage(message);
//
//                message = mBusChatHandler.obtainMessage(ChatBusHandlerCallback.CHAT,
//                        ChatHelper.oneToOneMessage("a5S16xVoXHbwd2IBVBzs8WXSiKG3", "Petra Cendelínová", loggedUser.getId(),
//                                ChatHelper.imageText("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")));
//                mBusChatHandler.sendMessage(message);
//
//                //group test
//                message = mBusChatHandler.obtainMessage(ChatBusHandlerCallback.CHAT,
//                        ChatHelper.groupMessage("a5S16xVoXHbwd2IBVBzs8WXSiKG3", "Petra Cendelínová", "d94282264c994168a919554f90af9c4c",
//                                new String[] { loggedUser.getId()}, "Test group text"  + new Random().nextInt()));
//                mBusChatHandler.sendMessage(message);

                advertiseHandler.postDelayed(this, 10000);
            }
        };
    }

    public static Uri getRingtone() {
        return ringtone;
    }

    public static void setRingtone(String ringtoneName) {
        ringtone = Uri.parse(ringtoneName);
    }

    /**
     * Starts advertisement process
     */
    public synchronized void startAdvertise() {
        advertiseHandler.post(advertiseCode);
    }

    /**
     * Stops advertisement process
     */
    public synchronized void stopAdvertise() {
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
        String gid = ChatHelper.groupDeletedGid(receivedMsg);
        notifyObservers(GROUP_DELETED, gid);
        deleteGroup(gid, true);
    }

    private void dealWithGroupReceiversChanged(String receivedMsg) {
        String gid = ChatHelper.groupReceiversChangedGid(receivedMsg);
        String[] newReceivers = ChatHelper.groupReceiversChangedReceivers(receivedMsg);
        groupChatsReceiversMap.put(gid, newReceivers);
        notifyObservers(GROUP_RECEIVERS_CHANGED, gid);
    }

    private void dealWithCancelAdvertiseMessage(String receivedMsg) {
        String sender = ChatHelper.cancelAdvertiseMessageSender(receivedMsg);
        if (loggedUser == null || sender.equals(loggedUser.getId())) {
            return;
        }
        String channelId = ChatHelper.cancelAdvertiseMessageSender(receivedMsg);
        notifyObservers(USER_CHANNEL_LEAVING, channelId);
    }

    private void dealWithGroupAdvertiseMessage(final String receivedMsg) {
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
        String uid = ChatHelper.advertiseMessageDisplayName(receivedMsg);
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null ||
                currentUser.getUid().equals(uid) || channelsNearDevice.containsKey(uid)) {
            return;
        }

        firebaseUsersRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
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
            ContentValues values = contentValues[0];
            return incomingDb.insert(tableName, null, values);
        }
    }

    private class StoreOutgoingMessageAsyncTask extends AsyncTask<ContentValues, Void, Long> {

        private String tableName = OutgoingMessagesTable.TABLE_NAME;

        @Override
        protected Long doInBackground(ContentValues... contentValues) {
            ContentValues values = contentValues[0];
            return outgoingDb.insert(tableName, null, values);
        }
    }

    private void storeIncomingMessage(String sender, String senderDisplName, String gid, String text, long time) {
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
        Message msg = mBusControlHandler.obtainMessage(ControlBusHandlerCallback.CONTROL, message);
        mBusControlHandler.sendMessage(msg);
    }

    public synchronized List<ChatMessage> getHistory(String key) {
        List<ChatMessage> orig = chatHistory.get(key);
        List<ChatMessage> copy = new ArrayList<>();
        for (ChatMessage message: orig) {
            copy.add(message);
        }
        return copy;
    }

    //***** CHANNELS METHODS *****\\

    public boolean isGroup(String gid) {
        return groupChatsReceiversMap.containsKey(gid);
    }

    public synchronized Set<ChatChannel> getChannels() {
        Set<ChatChannel> channelsCopy = new HashSet<>();
        channelsCopy.addAll(channelsNearDevice.values());
        return channelsCopy;
    }

    public ChatChannel getChannel(String channelId) {
        return channelsNearDevice.get(channelId);
    }

    //***** GROUPS METHODS *****\\

    public void createGroup(String gId, String[] receivers) {
        sendControlMessage(ChatHelper.groupAdvertiseMessage(gId, receivers));
    }

    public synchronized void deleteGroup(String groupId, boolean onlyDelete) {
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
            String gid = strings[0];
            String incomingSelection = IncomingMessagesTable.COLUMN_NAME_GID + " = ?";
            String outgoingSelection = OutgoingMessagesTable.COLUMN_NAME_GID + " = ?";
            incomingDb.delete(incomingTable, incomingSelection, new String[] { gid });
            outgoingDb.delete(outgoingTable, outgoingSelection, new String[] { gid });
            return null;
        }
    }

    public synchronized String[] getGroupReceivers(String gid) {
        return groupChatsReceiversMap.get(gid);
    }

    @Override
    public synchronized void addObserver(Observer obs) {
        if (mObservers.contains(this)) {
            mObservers.remove(this);
        }
        mObservers.add(obs);
    }

    @Override
    public synchronized void deleteObserver(Observer obs) {
        mObservers.remove(obs);
        if (mObservers.size() == 0) {
            mObservers.add(this);
        }
    }

    @Override
    public void update(Observable o, int qualifier, String data) {
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
        for (Observer obs : mObservers) {
            obs.update(this, arg, data);
        }
    }

    public User getLoggedUser() {
        return loggedUser;
    }

    public void setLoggedUser(User loggedUser) {
        ApplicationClass.loggedUser = loggedUser;
    }

    public void groupReceiversChangedAdvertise(String gid, String[] receivers) {
        String message = ChatHelper.groupReceiversChanged(gid, receivers);
        sendControlMessage(message);
    }

    public void groupDeletedAdvertise(String gid) {
        String message = ChatHelper.groupDeleted(gid);
        sendControlMessage(message);
    }

    private class ControlService implements ControlInterface, BusObject {
        private BusAttachment bus;

        public ControlService(BusAttachment bus) {
            this.bus = bus;
        }

        @BusSignalHandler(iface = "ee.ut.madp.whatisgoingon.control", signal = "Control")
        public void Control(String message) throws BusException {
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

        @BusSignalHandler(iface = "ee.ut.madp.whatisgoingon.chat", signal = "Chat")
        public void Chat(String message) {
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
            try {
                if (emitter == null) {
                    emitter = new SignalEmitter(mControlService, 0, SignalEmitter.GlobalBroadcast.Off);
                    emitter.setSessionlessFlag(true);
                    mControlInterface = emitter.getInterface(ControlInterface.class);
                }
                if (mControlInterface != null) {
                    String message = (String) msg.obj;
                    Log.i(TAG, "Sending message " + msg);
                    mControlInterface.Control(message);
                }
            } catch (BusException ex) {
                Log.w(TAG, ex);
            }
        }

        private void disconnectFromBus() {
            mBus.disconnect();
            mBusChatHandler.getLooper().quit();
        }

        private void connectToBus() {
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
            try {
                if (emitter == null) {
                    emitter = new SignalEmitter(mChatService, 0, SignalEmitter.GlobalBroadcast.Off);
                    emitter.setSessionlessFlag(true);
                    mChatInterface = emitter.getInterface(ChatInterface.class);
                }
                if (mChatInterface != null) {
                    String message = (String) msg.obj;
                    Log.i(TAG, "Sending message " + msg);
                    mChatInterface.Chat(message);
                }
            } catch (BusException ex) {
                Log.w(TAG, ex);
            }
        }

        private void disconnectFromBus() {
            mBus.disconnect();
            mBusChatHandler.getLooper().quit();
        }

        private void connectToBus() {
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
        System.loadLibrary("alljoyn_java");
    }
}
