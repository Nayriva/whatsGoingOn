package ee.ut.madp.whatsgoingon.chat;

import android.app.Application;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

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

import ee.ut.madp.whatsgoingon.helpers.ChatHelper;
import ee.ut.madp.whatsgoingon.models.ChatMessage;

/**
 * Created by dominikf on 16. 10. 2017.
 */

public class ChatApplication extends Application implements Observable {

    public static final String TAG = "chat.ChatApp";
    private FirebaseAuth firebaseAuth;

    private Map<String, List<ChatMessage>> chatHistory;
    private Map<String, String[]> groupChatsReceiversMap;

    private Handler mBusHandler;
    private Handler advertiseHandler;
    private Runnable advertiseCode;

    private final int HISTORY_MAX = 20;
    private static final int MESSAGE_CHAT = 1;
    public static final String MESSAGE_RECEIVED = "MESSAGE RECEIVED";
    public static final String GROUP_RECEIVERS_CHANGED = "GROUP_RECEIVERS_CHANGED";
    public static final String GROUP_DELETED = "GROUP_DELETED";

    private List<Observer> mObservers;

    public void checkIn() {
        chatHistory = new HashMap<>();
        groupChatsReceiversMap = new HashMap<>();
        FirebaseApp.initializeApp(this);
        firebaseAuth = FirebaseAuth.getInstance();
        mObservers = new ArrayList<>();
        setUpAdvertising();
    }

    private void setUpAdvertising() {
        advertiseHandler = new Handler();
        advertiseCode = new Runnable() {
            @Override
            public void run() {
                String msg = ChatHelper.advertiseMessage(firebaseAuth.getCurrentUser().getUid());
                Message message = mBusHandler.obtainMessage(BusHandlerCallback.CHAT, msg);
                mBusHandler.sendMessage(message);
                advertiseHandler.postDelayed(this, 2000);
            }
        };
    }

    public void startAdvertise() {
        advertiseHandler.post(advertiseCode);
    }

    public void stopAdvertise() {
        advertiseHandler.removeCallbacks(advertiseCode);
    }

    private Handler mHandler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == MESSAGE_CHAT) {
                String receivedMsg = (String) msg.obj;
                String messageType = ChatHelper.getMessageType(receivedMsg);
                switch (messageType) {
                    case "S": {
                        String messageReceiver = ChatHelper.oneToOneMessageReceiver(receivedMsg);
                        String sender = ChatHelper.oneToOneMessageSender(receivedMsg);
                        if (sender.equals(firebaseAuth.getCurrentUser().getUid())) {
                            return true;
                        } else if (!messageReceiver.equals(firebaseAuth.getCurrentUser().getUid())) {
                            return true;
                        }
                        storeOneToOneMessage(receivedMsg);
                        notifyObservers(MESSAGE_RECEIVED, sender);
                    }
                    break;
                    case "G": {
                        String[] messageReceivers = ChatHelper.groupMessageReceivers(receivedMsg);
                        String sender = ChatHelper.groupMessageSender(receivedMsg);
                        if (sender.equals(firebaseAuth.getCurrentUser().getUid())) {
                            return true;
                        }

                        boolean found = false;
                        for (String messageReceiver : messageReceivers) {
                            if (messageReceiver.equals(firebaseAuth.getCurrentUser().getUid())) {
                                found = true;
                            }
                        }
                        if (!found) {
                            return true;
                        }
                        storeGroupMessage(receivedMsg);
                        notifyObservers(MESSAGE_RECEIVED, sender);
                    }
                    break;
                    case "A": {
                        String uid = ChatHelper.advertiseMessageDisplayName(receivedMsg);
                        if (!chatHistory.containsKey(uid)) {
                            chatHistory.put(uid, new ArrayList<ChatMessage>());
                        }
                    }
                    break;
                    case "AG": {
                        String gid = ChatHelper.groupAdvertiseMessageId(receivedMsg);
                        String[] receivers = ChatHelper.groupAdvertiseMessageReceivers(receivedMsg);
                        boolean found = false;
                        for (String messageReceiver : receivers) {
                            if (messageReceiver.equals(firebaseAuth.getCurrentUser().getUid())) {
                                found = true;
                            }
                        }
                        if (!found) {
                            return true;
                        }
                        chatHistory.put(gid, new ArrayList<ChatMessage>());
                        groupChatsReceiversMap.put(gid, receivers);
                        notifyObservers(GROUP_RECEIVERS_CHANGED, receivedMsg);
                    }
                    break;
                    default:
                        return true;
                }
                return true;
            }
            return false;
        }
    });

    private void storeOneToOneMessage(String receivedMsg) {
        String sender = ChatHelper.oneToOneMessageSender(receivedMsg);
        if (! chatHistory.containsKey(sender)) {
            chatHistory.put(sender, new ArrayList<ChatMessage>());
        }
        String text = ChatHelper.oneToOneMessageText(receivedMsg);
        String senderName = ChatHelper.oneToOneMessageSenderDisplayName(receivedMsg);
        List<ChatMessage> hist = chatHistory.get(sender);

        ChatMessage newMessage = new ChatMessage(text, senderName, sender);

        if (hist.size() > HISTORY_MAX) {
            hist.remove(0);
            //TODO store messages to DB
        }
        hist.add(newMessage);
    }

    private void storeGroupMessage(String receivedMsg) {
        String group = ChatHelper.groupMessageDisplayName(receivedMsg);
        if (! chatHistory.containsKey(group)) {
            chatHistory.put(group, new ArrayList<ChatMessage>());
            String[] receivers = ChatHelper.groupMessageReceivers(receivedMsg);
            groupChatsReceiversMap.put(group, receivers);
        }
        String text = ChatHelper.groupMessageText(receivedMsg);
        String sender = ChatHelper.groupMessageSender(receivedMsg);
        String displayName = ChatHelper.groupMessageSenderDisplayName(receivedMsg);
        List<ChatMessage> hist = chatHistory.get(group);

        ChatMessage newMessage = new ChatMessage(text, displayName, sender);
        if (hist.size() > HISTORY_MAX) {
            hist.remove(0);
            //TODO store messages to DB
        }
        hist.add(newMessage);
    }

    public boolean isGroup(String groupName) {
        return groupChatsReceiversMap.containsKey(groupName);
    }

    public synchronized String[] getGroupReceivers(String groupName) {
        return groupChatsReceiversMap.get(groupName);
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
                ChatMessage newMessage = new ChatMessage(text, name, channel);
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

    public synchronized Set<String> getChannels() {
        Set<String> channelsCopy = new HashSet<>();
        for (String channel : chatHistory.keySet()) {
            channelsCopy.add(new String(channel.getBytes()));
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

    /* The class that is our AllJoyn service.  It implements the ChatInterface. */
    private class ChatService implements ChatInterface, BusObject {
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
                        //logStatus("BusAttachment.registerBusObject()", status);
                        return false;
                    }
                    status = mBus.connect();
                    //logStatus("BusAttachment.connect()", status);
                    if (status != Status.OK) {
                        //logStatus("BusAtt");
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
                        //logException("ChatInterface.Chat()", ex);
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

    private void notifyObservers(String arg, String data) {
        for (Observer obs : mObservers) {
            obs.update(this, arg, data);
        }
    }
}
