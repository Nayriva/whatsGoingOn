package ee.ut.madp.whatsgoingon.helpers;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.widget.TextView;

/**
 * Helper for working with custom fonts.
 */
public class FontHelper {

    private static final String TAG = FontHelper.class.getSimpleName();

    public static void setFont(Context context, TextView textview, String font) {
        Log.i(TAG, "setFont");
        Typeface typeface = Typeface.createFromAsset(context.getAssets(), font);
        textview.setTypeface(typeface);
    }
}
