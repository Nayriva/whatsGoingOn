package ee.ut.madp.whatsgoingon.helpers;

/**
 * Helper for generating and parsing chat messages.
 *
 * Structure of messages:
 *  ADVERTISE_MESSAGE: A~&&~displayName
 *  ONE_TO_ONE_MESSAGE: S~&&~sender~&&~receiver~&&~message_text
 *  GROUP_MESSAGE: G~&&~sender~&&~groupName~&&~receiver1~&~&~receiver2~&~&~...~&~&~receiverX~&&~message_text
 *
 * Created by dominikf on 16. 10. 2017.
 *
 * //TODO add javaDoc
 */

public class ChatHelper {
    private static String delimiter = "~&&~";
    private static String groupReceiversDelimiter = "~&&~&&~";

    //ONE TO ONE

    /**
     * Constructs one-to-one chat message with defined format
     * @param sender sender display name of message
     * @param receiver receiver display name of message
     * @param text content of message
     * @return one-to-one message for chat in correct format
     */
    public static String oneToOneMessage(String sender, String receiver, String text) {
        return "S" +
                delimiter +
                sender +
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
     * Extractor of receiver from one-to-one message
     * @param receivedMessage message from which the receiver should be extracted
     * @return receiver of message if found, null otherwise
     */
    public static String oneToOneMessageReceiver(String receivedMessage) {
        String[] parts = receivedMessage.split(delimiter);
        return parts[2];
    }

    /**
     * Extractor of message text from one-to-one message
     * @param receivedMessage message from which the text should be extracted
     * @return text of message, null otherwise
     */
    public static String oneToOneMessageText(String receivedMessage) {
        String[] parts = receivedMessage.split(delimiter);
        return parts[3];
    }

    //GROUP

    public static String groupMessage(String sender, String displayName, String[] receivers, String text ) {
        StringBuilder message = new StringBuilder();
        message.append("G");
        message.append(delimiter);
        message.append(sender);
        message.append(delimiter);
        message.append(displayName);
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

    public static String groupMessageDisplayName(String receivedMessage) {
        String[] parts = receivedMessage.split(delimiter);
        return parts[2];
    }

    public static String[] groupMessageReceivers(String receivedMessage) {
        String[] parts = receivedMessage.split(delimiter);
        return parts[3].split(groupReceiversDelimiter);
    }

    public static String groupMessageText(String receivedMessage) {
        String[] parts = receivedMessage.split(delimiter);
        return parts[4];
    }

    //ADVERTISE
    public static String advertiseMessage(String displayName) {
        return "A" +
                delimiter +
                displayName;
    }

    public static boolean isAdvertiseMessage(String receivedMessage) {
        String[] parts = receivedMessage.split(delimiter);
        return parts[0].equals("A");
    }

    public static String advertiseMessageDisplayName(String receivedMessage) {
        String[] parts = receivedMessage.split(delimiter);
        return parts[1];
    }

    //COMMON

    public static String getMessageType(String receivedMessage) {
        String[] parts = receivedMessage.split(delimiter);
        return parts[0];
    }
}
