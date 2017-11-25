package ee.ut.madp.whatsgoingon.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;

import java.util.Arrays;

import ee.ut.madp.whatsgoingon.constants.GeneralConstants;

import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.PREF_ACCOUNT_NAME;
import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.REQUEST_GOOGLE_PLAY_SERVICES;

/**
 * Helper for working with google account.
 *
 * Created by admin on 25.11.2017.
 */

public class GoogleAccountHelper {

    private static final String TAG = GoogleAccountHelper.class.getSimpleName();
    private static final String[] SCOPES = { CalendarScopes.CALENDAR };

    private static GoogleAccountCredential credential;

    public static void showGooglePlayServicesAvailabilityErrorDialog(final Activity activity, final int connectionStatusCode) {
        Log.i(TAG, "showGooglePlayServicesAvailabilityErrorDialog");
        activity.runOnUiThread(new Runnable() {
            public void run() {
                GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
                apiAvailability.getErrorDialog(activity, connectionStatusCode, REQUEST_GOOGLE_PLAY_SERVICES).show();
            }
        });
    }

    public static GoogleAccountCredential getGoogleAccountCredential(Context context) {
        Log.i(TAG, "getGoogleAccountCredential");
        if (credential == null) {
            credential = GoogleAccountCredential.usingOAuth2(
                context.getApplicationContext(), Arrays.asList(SCOPES))
                    .setBackOff(new ExponentialBackOff());
            SharedPreferences settings = ((Activity)context).getPreferences(Context.MODE_PRIVATE);
            credential.setSelectedAccountName(settings.getString(PREF_ACCOUNT_NAME, null));
        }
        return credential;
    }


    /**
     * Check that Google Play services APK is installed and up to date.
     */
    public static boolean checkGooglePlayServicesAvailable(Activity activity) {
        Log.i(TAG, "checkGooglePlayServicesAvailable");
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(activity);
        if (result != ConnectionResult.SUCCESS) {
            if (googleAPI.isUserResolvableError(result)) {
                // ask for googlePlayServices
                googleAPI.getErrorDialog(activity, result,
                        REQUEST_GOOGLE_PLAY_SERVICES).show();
            }

            return false;
        }

        return true;
    }

    public static void haveGooglePlayServices(Activity activity, GoogleAccountCredential credential) {
        Log.i(TAG, "haveGooglePlayServices");
        // check if there is already an account selected
        if (credential.getSelectedAccountName() == null) {
            // ask user to choose account
            chooseAccount(activity);
        }
    }

    public static void chooseAccount(Activity activity) {
        Log.i(TAG, "chooseAccount");
        activity.startActivityForResult(getGoogleAccountCredential(activity).newChooseAccountIntent(), GeneralConstants.REQUEST_ACCOUNT_PICKER);
    }
}
