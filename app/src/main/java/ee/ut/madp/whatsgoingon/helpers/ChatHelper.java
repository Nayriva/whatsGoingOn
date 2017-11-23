package ee.ut.madp.whatsgoingon.helpers;

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

    //ONE TO ONE

    /**
     * Constructs one-to-one chat message with defined format
     * @param sender sender display name of message
     * @param receiver receiver display name of message
     * @param text content of message
     * @return one-to-one message for chat in correct format
     */
    public static String oneToOneMessage(String sender, String senderDisplayName, String receiver, String text) {
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

    /**
     * Validator for one-to-one messages
     * @param receivedMessage message to be validated
     * @return true if message is in one-to-one format, false otherwise
     */
    public static boolean isOneToOneMessage(String receivedMessage) {
        String[] parts = receivedMessage.split(delimiter);
        return "S".equals(parts[0]);
    }

    /**
     * Extractor of sender from one-to-one message
     * @param receivedMessage message from which the sender should be extracted
     * @return sender of message if found, null otherwise
     */
    public static String oneToOneMessageSender(String receivedMessage) {
        String[] parts = receivedMessage.split(delimiter);
        return parts[1];
    }

    /**
     * Extractor of sender's display name from one-to-one message
     * @param receivedMessage message from which the display name should be extracted
     * @return sender's display name if found, null otherwise
     */
    public static String oneToOneMessageSenderDisplayName(String receivedMessage) {
        String[] parts = receivedMessage.split(delimiter);
        return parts[2];
    }

    /**
     * Extractor of receiver from one-to-one message
     * @param receivedMessage message from which the receiver should be extracted
     * @return receiver of message if found, null otherwise
     */
    public static String oneToOneMessageReceiver(String receivedMessage) {
        String[] parts = receivedMessage.split(delimiter);
        return parts[3];
    }

    /**
     * Extractor of message text from one-to-one message
     * @param receivedMessage message from which the text should be extracted
     * @return text of message, null otherwise
     */
    public static String oneToOneMessageText(String receivedMessage) {
        String[] parts = receivedMessage.split(delimiter);
        return parts[4];
    }

    //GROUP

    /**
     * Constructs group chat message with defined format
     * @param sender sender of message
     * @param authorDisplayName display name of sender
     * @param gid id of group
     * @param receivers array of receivers
     * @param text text of message
     * @return constructed message in defined format
     */
    public static String groupMessage(String sender, String authorDisplayName,
                                      String gid, String[] receivers, String text ) {
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
        String[] parts = receivedMessage.split(delimiter);
        return "G".equals(parts[0]);
    }

    public static String groupMessageSender(String receivedMessage) {
        String[] parts = receivedMessage.split(delimiter);
        return parts[1];
    }

    public static String groupMessageSenderDisplayName(String receivedMessage) {
        String[] parts = receivedMessage.split(delimiter);
        return parts[2];
    }

    public static String groupMessageGID(String receivedMessage) {
        String[] parts = receivedMessage.split(delimiter);
        return parts[3];
    }

    public static String[] groupMessageReceivers(String receivedMessage) {
        String[] parts = receivedMessage.split(delimiter);
        return parts[4].split(groupReceiversDelimiter);
    }

    public static String groupMessageText(String receivedMessage) {
        String[] parts = receivedMessage.split(delimiter);
        return parts[5];
    }

    //ADVERTISE

    public static String advertiseMessage(String uid) {
        return ADVERTISE_MESSAGE +
                delimiter +
                uid;
    }

    public static boolean isAdvertiseMessage(String receivedMessage) {
        String[] parts = receivedMessage.split(delimiter);
        return parts[0].equals("A");
    }

    public static String advertiseMessageDisplayName(String receivedMessage) {
        String[] parts = receivedMessage.split(delimiter);
        return parts[1];
    }

    //GROUP ADVERTISE

    public static String groupAdvertiseMessage(String id, String[] receivers) {
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
        String[] parts = receivedMsg.split(delimiter);
        return parts[1];
    }

    public static String[] groupAdvertiseMessageReceivers(String receivedMsg) {
        String[] parts = receivedMsg.split(delimiter);
        return parts[2].split(groupReceiversDelimiter);
    }

    //CANCEL ADVERTISE

    public static String cancelAdvertiseMessage(String id) {
        return CANCEL_ADVERTISE_MESSAGE +
                delimiter +
                id;
    }

    public static String cancelAdvertiseMessageSender(String receivedMsg) {
        String[] parts = receivedMsg.split(delimiter);
        return parts[1];
    }

    //GROUP RECEIVERS CHANGED

    public static String groupReceiversChanged(String gid, String[] receivers) {
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
        String[] parts = receivedMsg.split(delimiter);
        return parts[1];
    }

    public static String[] groupReceiversChangedReceivers(String receivedMsg) {
        String[] parts = receivedMsg.split(delimiter);
        return parts[2].split(groupReceiversDelimiter);
    }

    //GROUP DELETED

    public static String groupDeleted(String gid) {
        return GROUP_DELETED_MESSAGE +
                delimiter +
                gid;
    }

    public static String groupDeletedGid(String receivedMsg) {
        String[] parts = receivedMsg.split(delimiter);
        return parts[1];
    }

    //COMMON

    public static String getMessageType(String receivedMessage) {
        String[] parts = receivedMessage.split(delimiter);
        return parts[0];
    }

    public static String imageText(String base64) {
        return IMAGE_TEXT + base64;
    }

    public static boolean isImageText(String text) {
        if (text.length() <= IMAGE_TEXT.length()) {
            return false;
        }
        String subs = text.substring(0, IMAGE_TEXT.length());
        return IMAGE_TEXT.equals(subs);
    }

    public static String getImageBase64(String messageText) {
        messageText = messageText.replace(IMAGE_TEXT, "");
        return messageText;
    }
}
