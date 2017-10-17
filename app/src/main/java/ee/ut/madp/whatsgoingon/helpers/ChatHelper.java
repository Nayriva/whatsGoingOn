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
 */

public class ChatHelper {
    private static String delimiter = "~&&~";
    private static String groupReceiversDelimiter = "~&&~&&~";

    //ONE TO ONE

    public static String oneToOneMessage(String sender, String receiver, String text) {
        return "S" +
                delimiter +
                sender +
                delimiter +
                receiver +
                delimiter +
                text;
    }

    public static boolean isOneToOneMessage(String receivedMessage) {
        String[] parts = receivedMessage.split(delimiter);
        return "S".equals(parts[0]);
    }

    public static String oneToOneMessageSender(String receivedMessage) {
        String[] parts = receivedMessage.split(delimiter);
        return parts[1];
    }

    public static String oneToOneMessageReceiver(String receivedMessage) {
        String[] parts = receivedMessage.split(delimiter);
        return parts[2];
    }

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
