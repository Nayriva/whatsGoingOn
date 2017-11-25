package ee.ut.madp.whatsgoingon.asynctasks;

import android.os.AsyncTask;
import android.util.Log;

import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.services.calendar.Calendar;

import java.io.IOException;

import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.activities.EventFormActivity;
import ee.ut.madp.whatsgoingon.helpers.DialogHelper;
import ee.ut.madp.whatsgoingon.models.GoogleAccountHelper;

import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.REQUEST_AUTHORIZATION;

/**
 * Created by admin on 25.11.2017.
 */

abstract class CalendarAsyncTask extends AsyncTask<Void, Void, Void> {

    final EventFormActivity activity;
    final Calendar client;

    CalendarAsyncTask(EventFormActivity activity, Calendar client) {
        this.activity = activity;
        this.client = client;
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            doInBackground();
        } catch (final GooglePlayServicesAvailabilityIOException availabilityException) {
            GoogleAccountHelper.showGooglePlayServicesAvailabilityErrorDialog(activity, availabilityException.getConnectionStatusCode());
        } catch (UserRecoverableAuthIOException userRecoverableException) {
            activity.startActivityForResult(
                    userRecoverableException.getIntent(), REQUEST_AUTHORIZATION);
        } catch (IOException e) {
            Log.e("CalendarAsyncTask", "Error occurred during handling Google calendar " + e.getMessage());
        }
        return null;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        DialogHelper.showProgressDialog(activity, activity.getString(R.string.event_processing));
    }

    @Override
    protected final void onPostExecute(Void ignored) {
        super.onPostExecute(ignored);
        DialogHelper.hideProgressDialog();
    }

    protected abstract void doInBackground() throws IOException;
}