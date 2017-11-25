package ee.ut.madp.whatsgoingon.helpers;

import android.util.Log;

/**
 * Helper for generating and parsing chat messages.
 *
 * Structure of messages:
 *  ADVERTISE_MESSAGE: A~&&~id
 *  CANCEL_ADVERTISE_MESSAGE: C~&&~id
 *  GROUP_ADVERTISE_MESSAGE: AG~&&~groupId~&&~groupDisplayName~&&~receiver1~&~&~receiver2~&~&~...~&~&~receiverX
 *  ONE_TO_ONE_MESSAGE: S~&&~senderId~&&~senderDisplayName~&&~receiverId~&&~message_text
 *  GROUP_MESSAGE: G~&&~senderId~&&~senderDisplayName~&&~groupName~&&~
 *                  receiver1~&~&~receiver2~&~&~...~&~&~receiverX~&&~message_text
 *
 * Created by dominikf on 16. 10. 2017.
 */

public class ChatHelper {

    private static final String TAG = ChatHelper.class.getSimpleName();

    private static String delimiter = "~&&~";
    private static String groupReceiversDelimiter = "~&&&~";
    public static final String ADVERTISE_MESSAGE = "A";
    public static final String CANCEL_ADVERTISE_MESSAGE = "C";
    public static final String GROUP_ADVERTISE_MESSAGE = "AG";
    public static final String ONE_TO_ONE_MESSAGE = "S";
    public static final String GROUP_MESSAGE = "G";
    public static final String GROUP_RECEIVERS_CHANGED_MESSAGE = "GR";
    public static final String GROUP_DELETED_MESSAGE = "GD";
    public static final String IMAGE_TEXT = "IMG_B64_*";
    public static final String EVENT_TEXT = "SH_EVENT_*";
    public static final String EVENT_DELIMITER = "~&~";

    //ONE TO ONE

    public static String oneToOneMessage(String sender, String senderDisplayName, String receiver, String text) {
        Log.i(TAG, "oneToOneMessage");
        return ONE_TO_ONE_MESSAGE +
                delimiter +
                sender +
                delimiter +
                senderDisplayName +
                delimiter +
                receiver +
                delimiter +
                text;
    }

    public static boolean isOneToOneMessage(String receivedMessage) {
        Log.i(TAG, "isOneToOneMessage");
        String[] parts = receivedMessage.split(delimiter);
        return "S".equals(parts[0]);
    }

    public static String oneToOneMessageSender(String receivedMessage) {
        Log.i(TAG, "oneToOneMessageSender");
        String[] parts = receivedMessage.split(delimiter);
        return parts[1];
    }

    public static String oneToOneMessageSenderDisplayName(String receivedMessage) {
        Log.i(TAG, "oneToOneMessageSenderDisplayName");
        String[] parts = receivedMessage.split(delimiter);
        return parts[2];
    }

    public static String oneToOneMessageReceiver(String receivedMessage) {
        Log.i(TAG, "oneToOneMessageReceiver");
        String[] parts = receivedMessage.split(delimiter);
        return parts[3];
    }

    public static String oneToOneMessageText(String receivedMessage) {
        Log.i(TAG, "oneToOneMessageText");
        String[] parts = receivedMessage.split(delimiter);
        return parts[4];
    }

    //GROUP

    public static String groupMessage(String sender, String authorDisplayName,
                                      String gid, String[] receivers, String text ) {
        Log.i(TAG, "groupMessage");
        StringBuilder message = new StringBuilder();
        message.append(GROUP_MESSAGE);
        message.append(delimiter);
        message.append(sender);
        message.append(delimiter);
        message.append(authorDisplayName);
        message.append(delimiter);
        message.append(gid);
        message.append(delimiter);
        message.append(receivers[0]);
        for (int i = 1; i < receivers.length; i++) {
            message.append(groupReceiversDelimiter);
            message.append(receivers[i]);
        }
        message.append(delimiter);
        message.append(text);
        return message.toString();
    }

    public static boolean isGroupMessage(String receivedMessage) {
        Log.i(TAG, "isGroupMessage");
        String[] parts = receivedMessage.split(delimiter);
        return "G".equals(parts[0]);
    }

    public static String groupMessageSender(String receivedMessage) {
        Log.i(TAG, "groupMessageSender");
        String[] parts = receivedMessage.split(delimiter);
        return parts[1];
    }

    public static String groupMessageSenderDisplayName(String receivedMessage) {
        Log.i(TAG, "groupMessageSenderDisplayName");
        String[] parts = receivedMessage.split(delimiter);
        return parts[2];
    }

    public static String groupMessageGID(String receivedMessage) {
        Log.i(TAG, "groupMessageGID");
        String[] parts = receivedMessage.split(delimiter);
        return parts[3];
    }

    public static String[] groupMessageReceivers(String receivedMessage) {
        Log.i(TAG, "groupMessageReceivers");
        String[] parts = receivedMessage.split(delimiter);
        return parts[4].split(groupReceiversDelimiter);
    }

    public static String groupMessageText(String receivedMessage) {
        Log.i(TAG, "grouPmessageText");
        String[] parts = receivedMessage.split(delimiter);
        return parts[5];
    }

    //ADVERTISE

    public static String advertiseMessage(String uid) {
        Log.i(TAG, "advertiseMessage");
        return ADVERTISE_MESSAGE +
                delimiter +
                uid;
    }

    public static boolean isAdvertiseMessage(String receivedMessage) {
        Log.i(TAG, "isAdvertiseMessage");
        String[] parts = receivedMessage.split(delimiter);
        return parts[0].equals("A");
    }

    public static String advertiseMessageDisplayName(String receivedMessage) {
        Log.i(TAG, "advertiseMessageDisplayName");
        String[] parts = receivedMessage.split(delimiter);
        return parts[1];
    }

    //GROUP ADVERTISE

    public static String groupAdvertiseMessage(String id, String[] receivers) {
        Log.i(TAG, "groupAdvertiseMessage");
        String msg = GROUP_ADVERTISE_MESSAGE +
                delimiter +
                id +
                delimiter +
                receivers [0];
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 1; i < receivers.length; i++) {
            stringBuilder.append(groupReceiversDelimiter);
            stringBuilder.append(receivers[i]);
        }
        msg = msg + stringBuilder.toString();
        return msg;
    }

    public static String groupAdvertiseMessageId(String receivedMsg) {
        Log.i(TAG, "groupAdvertiseMessageId");
        String[] parts = receivedMsg.split(delimiter);
        return parts[1];
    }

    public static String[] groupAdvertiseMessageReceivers(String receivedMsg) {
        Log.i(TAG, "groupAdvertiseMessageReceivers");
        String[] parts = receivedMsg.split(delimiter);
        return parts[2].split(groupReceiversDelimiter);
    }

    //CANCEL ADVERTISE

    public static String cancelAdvertiseMessage(String id) {
        Log.i(TAG, "cancelAdvertiseMessage");
        return CANCEL_ADVERTISE_MESSAGE +
                delimiter +
                id;
    }

    public static String cancelAdvertiseMessageSender(String receivedMsg) {
        Log.i(TAG, "cancelAdvertiseMessageSender");
        String[] parts = receivedMsg.split(delimiter);
        return parts[1];
    }

    //GROUP RECEIVERS CHANGED

    public static String groupReceiversChanged(String gid, String[] receivers) {
        Log.i(TAG, "groupReceiversChanged");
        String msg = GROUP_RECEIVERS_CHANGED_MESSAGE
                + delimiter
                + gid
                + delimiter
                + receivers[0];
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 1; i < receivers.length; i++) {
            stringBuilder.append(groupReceiversDelimiter);
            stringBuilder.append(receivers[i]);
        }
        msg = msg + stringBuilder.toString();
        return msg;
    }

    public static String groupReceiversChangedGid(String receivedMsg) {
        Log.i(TAG, "groupReceiversChangedGid");
        String[] parts = receivedMsg.split(delimiter);
        return parts[1];
    }

    public static String[] groupReceiversChangedReceivers(String receivedMsg) {
        Log.i(TAG, "groupReceiversChangedReceivers");
        String[] parts = receivedMsg.split(delimiter);
        return parts[2].split(groupReceiversDelimiter);
    }

    //GROUP DELETED

    public static String groupDeleted(String gid) {
        Log.i(TAG, "groupDeleted");
        return GROUP_DELETED_MESSAGE +
                delimiter +
                gid;
    }

    public static String groupDeletedGid(String receivedMsg) {
        Log.i(TAG, "groupDeletedGid");
        String[] parts = receivedMsg.split(delimiter);
        return parts[1];
    }

    //COMMON

    public static String getMessageType(String receivedMessage) {
        Log.i(TAG, "getMessageType");
        String[] parts = receivedMessage.split(delimiter);
        return parts[0];
    }

    public static String imageText(String base64) {
        Log.i(TAG, "imageText");
        return IMAGE_TEXT + base64;
    }

    public static boolean isImageText(String text) {
        Log.i(TAG, "isImageText");
        if (text.length() <= IMAGE_TEXT.length()) {
            return false;
        }
        String subs = text.substring(0, IMAGE_TEXT.length());
        return IMAGE_TEXT.equals(subs);
    }

    public static String getImageBase64(String messageText) {
        Log.i(TAG, "getImageBase64");
        messageText = messageText.replace(IMAGE_TEXT, "");
        return messageText;
    }

    public static String eventText(String eventText) {
        Log.i(TAG, "eventText");
        return EVENT_TEXT + eventText;
    }

    public static boolean isEventText(String text) {
        Log.i(TAG, "isEventText");
        if (text.length() <= IMAGE_TEXT.length()) {
            return false;
        }
        String subs = text.substring(0, EVENT_TEXT.length());
        return EVENT_TEXT.equals(subs);
    }

    public static String getEventText(String messageText) {
        Log.i(TAG, "getEventText");
        messageText = messageText.replace(EVENT_TEXT, "");
        return messageText;
    }

    public static String encodeEventMessage(String eventId, String eventName, String eventInfo) {
        Log.i(TAG, "encodeEventMessage");
        return eventId + EVENT_DELIMITER + eventName + EVENT_DELIMITER + eventInfo;
    }

    public static String[] decodeEventMessage(String eventMessage) {
        Log.i(TAG, "decodeEventMessage");
        //[0] - eventID
        //[1] - eventName
        //[3] - eventInfo
        return eventMessage.split(EVENT_DELIMITER);
    }

}
