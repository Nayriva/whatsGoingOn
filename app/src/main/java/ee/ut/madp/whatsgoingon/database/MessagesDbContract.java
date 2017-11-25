package ee.ut.madp.whatsgoingon.database;


import android.provider.BaseColumns;

/**
 * Class specifying contracts for databases.
 *
 * Created by dominikf on 18. 11. 2017.
 */

public class MessagesDbContract {

    private MessagesDbContract() {}

    public static class OutgoingMessagesTable implements BaseColumns {
        public static final String TABLE_NAME = "outgoing_messages";
        public static final String COLUMN_NAME_LOGGED_USER = "logged_user";
        public static final String COLUMN_NAME_RECEIVER = "receiver";
        public static final String COLUMN_NAME_GID = "gid";
        public static final String COLUMN_NAME_TEXT = "text";
        public static final String COLUMN_NAME_TIME = "time";
    }

    public static class IncomingMessagesTable implements BaseColumns {
        public static final String TABLE_NAME = "outgoing_messages";
        public static final String COLUMN_NAME_LOGGED_USER = "logged_user";
        public static final String COLUMN_NAME_SENDER = "sender";
        public static final String COLUMN_NAME_SENDER_DISPL_NAME = "sender_displ_name";
        public static final String COLUMN_NAME_GID = "gid";
        public static final String COLUMN_NAME_TEXT = "text";
        public static final String COLUMN_NAME_TIME = "time";
    }

    public static final String SQL_CREATE_OUTGOING =
            "CREATE TABLE " + OutgoingMessagesTable.TABLE_NAME + " (" +
                    OutgoingMessagesTable._ID + " INTEGER PRIMARY KEY," +
                    OutgoingMessagesTable.COLUMN_NAME_LOGGED_USER + " TEXT," +
                    OutgoingMessagesTable.COLUMN_NAME_RECEIVER + " TEXT," +
                    OutgoingMessagesTable.COLUMN_NAME_GID + " TEXT," +
                    OutgoingMessagesTable.COLUMN_NAME_TEXT + " TEXT," +
                    OutgoingMessagesTable.COLUMN_NAME_TIME + " LONG)";

    public static final String SQL_CREATE_INCOMING =
            "CREATE TABLE " + IncomingMessagesTable.TABLE_NAME + " (" +
                    IncomingMessagesTable._ID + " INTEGER PRIMARY KEY," +
                    IncomingMessagesTable.COLUMN_NAME_LOGGED_USER + " TEXT," +
                    IncomingMessagesTable.COLUMN_NAME_SENDER + " TEXT," +
                    IncomingMessagesTable.COLUMN_NAME_SENDER_DISPL_NAME + " TEXT," +
                    IncomingMessagesTable.COLUMN_NAME_GID + " TEXT," +
                    IncomingMessagesTable.COLUMN_NAME_TEXT + " TEXT," +
                    IncomingMessagesTable.COLUMN_NAME_TIME + " LONG)";

    public static final String SQL_DELETE_OUTGOING =
            "DROP TABLE IF EXISTS " + OutgoingMessagesTable.TABLE_NAME;

    public static final String SQL_DELETE_INCOMING =
            "DROP TABLE IF EXISTS " + IncomingMessagesTable.TABLE_NAME;
}
