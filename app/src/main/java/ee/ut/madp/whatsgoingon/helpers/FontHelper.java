package ee.ut.madp.whatsgoingon.helpers;

import android.content.Context;
import android.graphics.Typeface;
import android.widget.TextView;


public class FontHelper {
    public static void setFont(Context context, TextView textview, String font) {
        Typeface typeface = Typeface.createFromAsset(context.getAssets(), font);
        textview.setTypeface(typeface);
    }
}
