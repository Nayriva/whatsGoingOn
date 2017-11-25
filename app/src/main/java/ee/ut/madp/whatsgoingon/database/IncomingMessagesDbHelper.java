package ee.ut.madp.whatsgoingon.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Helper for working with database containing incoming messages.
 * Created by dominikf on 18. 11. 2017.
 */

public class IncomingMessagesDbHelper extends SQLiteOpenHelper {

    private static final String TAG = IncomingMessagesDbHelper.class.getSimpleName();

    public static final int DATABASE_VERSION = 2;
    public static final String DATABASE_NAME = "IncomingMessages.db";

    public IncomingMessagesDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        Log.i(TAG, "constructor");
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        Log.i(TAG, "onCreate");
        sqLiteDatabase.execSQL(MessagesDbContract.SQL_CREATE_INCOMING);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        Log.i(TAG, "onUpgrade");
        sqLiteDatabase.execSQL(MessagesDbContract.SQL_DELETE_INCOMING);
        onCreate(sqLiteDatabase);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "onDowngrade");
        onUpgrade(db, oldVersion, newVersion);
    }
}
