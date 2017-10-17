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
import java.util.List;
import java.util.Map;
import java.util.Set;

import ee.ut.madp.whatsgoingon.helpers.ChatHelper;
import ee.ut.madp.whatsgoingon.models.ChatMessage;

/**
 * Created by dominikf on 16. 10. 2017.
 */

public class ChatApplication extends Application {

    public static final String TAG = "chat.ChatApp";
    private FirebaseAuth firebaseAuth;

    private Map<String, List<ChatMessage>> chatHistory;
    private Map<String, String[]> groupChats;

    private Handler mBusHandler;
    private final int HISTORY_MAX = 20;
    private static final int MESSAGE_CHAT = 1;

    private Thread timer;

    public void checkIn() {
        chatHistory = new HashMap<>();
        groupChats = new HashMap<>();
        FirebaseApp.initializeApp(this);
        firebaseAuth = FirebaseAuth.getInstance();

        timer = new Thread() {
            public void run () {
                for (;;) {
                    // do stuff in a separate thread
                    String msg = ChatHelper.advertiseMessage(firebaseAuth.getCurrentUser().getEmail());
                    Message message = mBusHandler.obtainMessage(BusHandlerCallback.CHAT, msg);
                    mBusHandler.sendMessage(message);

                    try {
                        sleep(30000);    // sleep for 3 seconds
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }

    public void startAdvertise() {
        timer.start();
    }

    public void stopAdvertise() {
        try {
            timer.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Handler mHandler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_CHAT: {
                    String receivedMsg = (String) msg.obj;
                    String messageType = ChatHelper.getMessageType(receivedMsg);
                    switch (messageType) {
                        case "S": {
                            String messageReceiver = ChatHelper.oneToOneMessageReceiver(receivedMsg);
                            if (! messageReceiver.equals(firebaseAuth.getCurrentUser().getEmail())) {
                                return true;
                            }
                            storeOneToOneMessage(receivedMsg);
                        } break;
                        case "G": {
                            String[] messageReceivers = ChatHelper.groupMessageReceivers(receivedMsg);
                            boolean found = false;
                            for (int i = 0; i < messageReceivers.length; i++) {
                                if (messageReceivers[i].equals(firebaseAuth.getCurrentUser().getEmail())) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                return true;
                            }
                            storeGroupMessage(receivedMsg);
                        } break;
                        case "A": {
                            String displayName = ChatHelper.advertiseMessageDisplayName(receivedMsg);
                            if (! chatHistory.containsKey(displayName)) {
                                chatHistory.put(displayName, new ArrayList<ChatMessage>());
                            }
                        }
                        default:
                            return true;
                    }
                } break;
                default:
                    break;
            }

            return true;
        }
    });

    private void storeOneToOneMessage(String receivedMsg) {
        String sender = ChatHelper.oneToOneMessageSender(receivedMsg);
        if (! chatHistory.containsKey(sender)) {
            chatHistory.put(sender, new ArrayList<ChatMessage>());
        }
        String text = ChatHelper.oneToOneMessageText(receivedMsg);
        List<ChatMessage> hist = chatHistory.get(sender);

        ChatMessage newMessage = new ChatMessage(text, sender);
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
            groupChats.put(group, receivers);
        }
        String text = ChatHelper.groupMessageText(receivedMsg);
        String sender = ChatHelper.groupMessageSender(receivedMsg);
        List<ChatMessage> hist = chatHistory.get(group);

        ChatMessage newMessage = new ChatMessage(text, sender);
        if (hist.size() > HISTORY_MAX) {
            hist.remove(0);
            //TODO store messages to DB
        }
        hist.add(newMessage);
    }

    public boolean isGroup(String groupName) {
        return groupChats.containsKey(groupName);
    }

    public String[] getGroupReceivers(String groupName) {
        return groupChats.get(groupName);
    }

    public void sendMessage(String message) {
        Message msg = mBusHandler.obtainMessage(BusHandlerCallback.CHAT, message);
        mBusHandler.sendMessage(msg);
    }

    public List<ChatMessage> getHistory(String key) {
        return chatHistory.get(key);
    }

    public Set<String> getChannels() {
        return chatHistory.keySet();
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
}
