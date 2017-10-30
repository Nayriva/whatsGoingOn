package ee.ut.madp.whatsgoingon.chat;

import android.app.Application;
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

import ee.ut.madp.whatsgoingon.constants.FirebaseConstants;
import ee.ut.madp.whatsgoingon.helpers.ChatHelper;
import ee.ut.madp.whatsgoingon.models.ChatChannel;
import ee.ut.madp.whatsgoingon.models.ChatMessage;
import ee.ut.madp.whatsgoingon.models.Group;
import ee.ut.madp.whatsgoingon.models.User;

/**
 * Created by dominikf on 16. 10. 2017.
 */

public class ChatApplication extends Application implements Observable {

    public static final String TAG = "chat.ChatApp";
    private FirebaseAuth firebaseAuth;
    private DatabaseReference firebaseGroupsRef;
    private DatabaseReference firebaseUsersRef;

    private Map<String, ChatChannel> channelsNearDevice;
    private Map<String, List<ChatMessage>> chatHistory;
    private Map<String, String[]> groupChatsReceiversMap;

    private Handler mBusHandler;
    private Handler advertiseHandler;
    private Runnable advertiseCode;

    private final int HISTORY_MAX = 20;
    private static final int MESSAGE_CHAT = 1;
    public static final int GROUP_MESSAGE_RECEIVED = 1;
    public static final int ONE_TO_ONE_MESSAGE_RECEIVED = 2;
    public static final int GROUP_RECEIVERS_CHANGED = 3;
    public static final int GROUP_CHANNEL_DISCOVERED = 4;
    public static final int GROUP_DELETED = 5;
    public static final int USER_CHANNEL_DISCOVERED = 6;
    public static final int USER_CHANNEL_LEAVING = 7;
    //TODO handle user profile change (mainly photo)

    private List<Observer> mObservers;

    public void checkIn() {
        FirebaseApp.initializeApp(this);
        firebaseGroupsRef = FirebaseDatabase.getInstance()
                .getReference().child(FirebaseConstants.FIREBASE_CHILD_GROUPS);
        firebaseUsersRef = FirebaseDatabase.getInstance()
                .getReference().child(FirebaseConstants.FIREBASE_CHILD_USERS);

        chatHistory = new HashMap<>();
        groupChatsReceiversMap = new HashMap<>();
        firebaseAuth = FirebaseAuth.getInstance();
        mObservers = new ArrayList<>();
        channelsNearDevice = new HashMap<>();
        setUpAdvertising();
    }

    private void setUpAdvertising() {
        advertiseHandler = new Handler();
        advertiseCode = new Runnable() {
            @Override
            public void run() {
                FirebaseUser currentUser = firebaseAuth.getCurrentUser();
                if (currentUser == null) {
                    return;
                }
                String msg = ChatHelper.advertiseMessage(firebaseAuth.getCurrentUser().getUid());
                Message message = mBusHandler.obtainMessage(BusHandlerCallback.CHAT, msg);
                mBusHandler.sendMessage(message);
                advertiseHandler.postDelayed(this, 10000);
            }
        };
    }

    public synchronized void startAdvertise() {
        advertiseHandler.post(advertiseCode);
    }

    public synchronized void stopAdvertise() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            return;
        }
        sendMessage(ChatHelper.cancelAdvertiseMessage(firebaseAuth.getCurrentUser().getUid()));
        advertiseHandler.removeCallbacks(advertiseCode);
        channelsNearDevice = new HashMap<>();
        chatHistory = new HashMap<>();
        groupChatsReceiversMap = new HashMap<>();
    }

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

    private Handler mHandler = new Handler(new Handler.Callback() {

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
                    case ChatHelper.ADVERTISE_MESSAGE: {
                        dealWithAdvertiseMessage(receivedMsg);
                    } break;
                    case ChatHelper.GROUP_ADVERTISE_MESSAGE: {
                        dealWithGroupAdvertiseMessage(receivedMsg);
                    } break;
                    case ChatHelper.CANCEL_ADVERTISE_MESSAGE: {
                        dealWithCancelAdvertiseMessage(receivedMsg);
                    } break;
                    default:
                        break;
                }
                return true;
            }
            return false;
        }
    });



    private void dealWithCancelAdvertiseMessage(String receivedMsg) {
        String sender = ChatHelper.cancelAdvertiseMessageSender(receivedMsg);
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null || sender.equals(currentUser.getUid())) {
            return;
        }
        String channelId = ChatHelper.cancelAdvertiseMessageSender(receivedMsg);
        notifyObservers(USER_CHANNEL_LEAVING, channelId);
    }

    private void dealWithGroupAdvertiseMessage(final String receivedMsg) {
        final String gid = ChatHelper.groupAdvertiseMessageId(receivedMsg);
        if (channelsNearDevice.containsKey(gid)) {
            return;
        }

        String[] receivers = ChatHelper.groupAdvertiseMessageReceivers(receivedMsg);
        boolean found = false;
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        for (String receiver: receivers) {
            if (receiver.equals(currentUser.getUid())) {
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
                newGroupChannel.setReceivers(group.getReceivers().toArray(new String[0]));
                channelsNearDevice.put(newGroupChannel.getId(), newGroupChannel);
                groupChatsReceiversMap.put(newGroupChannel.getId(),
                        group.getReceivers().toArray(new String[0]));
                //TODO download history from firebase
                chatHistory.put(newGroupChannel.getId(), new ArrayList<ChatMessage>());
                notifyObservers(GROUP_CHANNEL_DISCOVERED, gid);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                //left blank intentionally
            }
        });
    }

    private void dealWithAdvertiseMessage(String receivedMsg) {
        final String uid = ChatHelper.advertiseMessageDisplayName(receivedMsg);
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
                //TODO download history from firebase
                chatHistory.put(user.getId(), new ArrayList<ChatMessage>());
                notifyObservers(USER_CHANNEL_DISCOVERED, uid);
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
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null || sender.equals(currentUser.getUid())) {
            return;
        }

        boolean found = false;
        for (String messageReceiver : messageReceivers) {
            if (messageReceiver.equals(firebaseAuth.getCurrentUser().getUid())) {
                found = true;
                break;
            }
        }
        if (!found) {
            return;
        }
        storeGroupMessage(receivedMsg);
        String gid = ChatHelper.groupMessageGID(receivedMsg);
        notifyObservers(GROUP_MESSAGE_RECEIVED, gid);
    }

    private void storeGroupMessage(String receivedMsg) {
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
                firebaseAuth.getCurrentUser().getUid().equals(sender));
        if (hist.size() > HISTORY_MAX) {
            hist.remove(0);
            //TODO store messages to DB
        }
        hist.add(newMessage);
    }

    private void dealWithOneToOneMessage(String receivedMsg) {
        String messageReceiver = ChatHelper.oneToOneMessageReceiver(receivedMsg);
        String sender = ChatHelper.oneToOneMessageSender(receivedMsg);
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null || sender.equals(currentUser.getUid())) {
            return;
        } else if (!messageReceiver.equals(firebaseAuth.getCurrentUser().getUid())) {
            return;
        }
        storeOneToOneMessage(receivedMsg);
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
                firebaseAuth.getCurrentUser().getUid().equals(sender));
        if (hist.size() > HISTORY_MAX) {
            hist.remove(0);
            //TODO store messages to DB
        }
        hist.add(newMessage);
    }

    public boolean isGroup(String gid) {
        return groupChatsReceiversMap.containsKey(gid);
    }

    public synchronized String[] getGroupReceivers(String gid) {
        return groupChatsReceiversMap.get(gid);
    }

    public void sendMessage(String message) {
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
                if (hist.size() > HISTORY_MAX) {
                    hist.remove(0);
                    //TODO store messages to DB
                }
                hist.add(newMessage);
            } break;
            case "G": {
                storeGroupMessage(message);
            } break;
        }
        Message msg = mBusHandler.obtainMessage(BusHandlerCallback.CHAT, message);
        mBusHandler.sendMessage(msg);
    }

    public synchronized List<ChatMessage> getHistory(String key) {
        List<ChatMessage> orig = chatHistory.get(key);
        List<ChatMessage> copy = new ArrayList<>();
        for (ChatMessage message: orig) {
            copy.add(message);
        }
        return copy;
    }

    public synchronized Set<ChatChannel> getChannels() {
        Set<ChatChannel> channelsCopy = new HashSet<>();
        for (ChatChannel channel : channelsNearDevice.values()) {
            channelsCopy.add(channel);
        }
        return channelsCopy;
    }

    public synchronized void deleteGroup(String groupId, boolean onlyDelete) {
        Iterator<Map.Entry<String,List<ChatMessage>>> iter = chatHistory.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, List<ChatMessage>> entry = iter.next();
            if(groupId.equals(entry.getKey())){
                iter.remove();
            }
        }
        if (onlyDelete) {
            notifyObservers(GROUP_DELETED, groupId);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread busThread = new HandlerThread("BusHandler");
        busThread.start();

        mBusHandler = new Handler(busThread.getLooper(), new BusHandlerCallback());
        mBusHandler.sendEmptyMessage(BusHandlerCallback.CONNECT);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        mBusHandler.sendEmptyMessage(BusHandlerCallback.DISCONNECT);
    }

    public void createGroup(String gId, String[] receivers) {
        sendMessage(ChatHelper.groupAdvertiseMessage(
                gId, receivers
        ));
    }

    public ChatChannel getChannel(String channelId) {
        return channelsNearDevice.get(channelId);
    }

    /* The class that is our AllJoyn service.  It implements the ChatInterface. */
    private class ChatService implements ChatInterface, BusObject {
        private BusAttachment   bus;

        public ChatService(BusAttachment bus) {
            this.bus = bus;
        }

        @BusSignalHandler(iface = "ee.ut.madp.whatisgoingon.chat", signal = "Chat")
        public void Chat(String message) {
            Log.i(TAG, "Signal  : " + message);
            sendMessage(MESSAGE_CHAT, message);
        }

        private void sendMessage(int what, Object obj) {
            mHandler.sendMessage(mHandler.obtainMessage(what, obj));
        }
    }

    /* This Callback class will handle all AllJoyn calls. See onCreate(). */
    private class BusHandlerCallback implements Handler.Callback {

        /* The AllJoyn BusAttachment */
        private BusAttachment mBus;

        /* The AllJoyn SignalEmitter used to emit sessionless signals */
        private SignalEmitter emitter;

        private ChatInterface mChatInterface = null;
        private ChatService mChatService = null;

        /* These are the messages sent to the BusHandlerCallback from the UI. */
        private static final int CONNECT = 1;
        private static final int DISCONNECT = 2;
        private static final int CHAT = 3;

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case CONNECT: {
                    org.alljoyn.bus.alljoyn.DaemonInit.PrepareDaemon(getApplicationContext());
                    mBus = new BusAttachment(getPackageName(), BusAttachment.RemoteMessage.Receive);
                    mChatService = new ChatService(mBus);
                    Status status = mBus.registerBusObject(mChatService, "/ChatService");
                    if (Status.OK != status) {
                        Log.i(TAG + ".RegBus()", status.toString());
                        return false;
                    }
                    status = mBus.connect();
                    Log.i(TAG, status.toString());
                    if (status != Status.OK) {
                        Log.i(TAG + ".BusCon()", "BusAtt");
                        return false;
                    }
                    status = mBus.registerSignalHandlers(mChatService);
                    if (status != Status.OK) {
                        Log.i(TAG, "Problem while registering signal handler");
                        return false;
                    }
                    status = mBus.addMatch("sessionless='t'");
                    if (status == Status.OK) {
                        Log.i(TAG, "AddMatch was called successfully");
                    }
                    break;
                } case DISCONNECT: {
                    mBus.disconnect();
                    mBusHandler.getLooper().quit();
                    break;
                } case CHAT: {
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
                    break;
                }
                default:
                    break;
            }
            return true;
        }
    }

    /* Load the native alljoyn_java library. */
    static {
        System.loadLibrary("alljoyn_java");
    }

    @Override
    public synchronized void addObserver(Observer obs) {
        mObservers.add(obs);
    }

    @Override
    public synchronized void deleteObserver(Observer obs) {
        mObservers.remove(obs);
    }

    private void notifyObservers(int arg, String data) {
        for (Observer obs : mObservers) {
            obs.update(this, arg, data);
        }
    }
}
