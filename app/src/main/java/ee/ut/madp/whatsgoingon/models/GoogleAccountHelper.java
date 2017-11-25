package ee.ut.madp.whatsgoingon.models;

import android.app.Activity;
import android.content.Context;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;

import java.util.Arrays;

import ee.ut.madp.whatsgoingon.constants.GeneralConstants;

/**
 * Created by admin on 25.11.2017.
 */

public class GoogleAccountHelper {
    private static final String[] SCOPES = { CalendarScopes.CALENDAR };

    private static GoogleAccountCredential credential;

    public static GoogleAccountCredential getGoogleAccountCredential(Context context) {
        if (credential == null) credential = GoogleAccountCredential.usingOAuth2(
                context.getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
        return credential;
    }


    /**
     * Check that Google Play services APK is installed and up to date.
     */
    public static boolean checkGooglePlayServicesAvailable(Activity activity) {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(activity);
        if (result != ConnectionResult.SUCCESS) {
            if (googleAPI.isUserResolvableError(result)) {
                // ask for googlePlayServices
                googleAPI.getErrorDialog(activity, result,
                        GeneralConstants.REQUEST_GOOGLE_PLAY_SERVICES).show();
            }

            return false;
        }

        return true;
    }

    public static void haveGooglePlayServices(Activity activity, GoogleAccountCredential credential) {
        // check if there is already an account selected
        if (credential.getSelectedAccountName() == null) {
            // ask user to choose account
            chooseAccount(activity);
        }
    }

    public static void chooseAccount(Activity activity) {
        activity.startActivityForResult(getGoogleAccountCredential(activity).newChooseAccountIntent(), GeneralConstants.REQUEST_ACCOUNT_PICKER);
    }
}
