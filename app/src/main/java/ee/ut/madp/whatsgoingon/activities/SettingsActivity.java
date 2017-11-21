package ee.ut.madp.whatsgoingon.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.MenuItem;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.mvc.imagepicker.ImagePicker;

import butterknife.BindView;
import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.ApplicationClass;
import ee.ut.madp.whatsgoingon.chat.Observable;
import ee.ut.madp.whatsgoingon.chat.Observer;
import ee.ut.madp.whatsgoingon.helpers.DialogHelper;
import ee.ut.madp.whatsgoingon.helpers.MessageNotificationHelper;
import ee.ut.madp.whatsgoingon.models.ChatChannel;
import ee.ut.madp.whatsgoingon.models.ChatMessage;

public class SettingsActivity extends AppCompatPreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener, Observer {

    private static final String TAG = SettingsActivity.class.getSimpleName();

    public static final String PREFERENCE_PASSWORD = "password";
    public static final String PREFERENCE_MESSAGE_NOTIFICATION = "notifications_message";
    public static final String PREFERENCE_NOTIFICATION_VIBRATE = "notification_vibrate";
    public static final String PREFERENCE_NOTIFICATION_RINGTONE = "notification_ringtone";

    private static FirebaseAuth firebaseAuth;
    private Intent intent;
    private ApplicationClass application;

    @BindView(R.id.coordinator_layout) CoordinatorLayout coordinatorLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        application = (ApplicationClass) getApplication();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        MyPreferenceFragment prefFragment = new MyPreferenceFragment();
        prefFragment.setCoordinatorLayout(coordinatorLayout);
        getFragmentManager().beginTransaction().replace(android.R.id.content, prefFragment).commit();

        firebaseAuth = FirebaseAuth.getInstance();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "onOptionsItemSelected");
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (intent == null) {
                intent = getIntent();
            }
            setResult(Activity.RESULT_OK, intent);
            finish();

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.i(TAG, "onSharedPreferenceChanged: " + sharedPreferences + ", " + key);
        SharedPreferences prefs = getSharedPreferences("setting.whatsgoingon", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        if (key.equals(PREFERENCE_MESSAGE_NOTIFICATION)) {
            boolean isAllowed = sharedPreferences.getBoolean(key, true);
            editor.putBoolean(PREFERENCE_MESSAGE_NOTIFICATION, isAllowed);
            editor.apply();
            ApplicationClass.notificationsOn = isAllowed;
        }

        if (key.equals(PREFERENCE_NOTIFICATION_VIBRATE)) {
            boolean isAllowed = sharedPreferences.getBoolean(key, true);
            editor.putBoolean(PREFERENCE_NOTIFICATION_VIBRATE, isAllowed);
            editor.commit();
            ApplicationClass.vibrateOn = isAllowed;
        }

        if (key.equals(PREFERENCE_NOTIFICATION_RINGTONE)) {
            String ringtonePreference = sharedPreferences.getString(PREFERENCE_NOTIFICATION_RINGTONE, "DEFAULT_SOUND");
            editor.putString(PREFERENCE_NOTIFICATION_RINGTONE, ringtonePreference);
            editor.commit();
            ApplicationClass.setRingtone(ringtonePreference);
        }

    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
        application.addObserver(this);
        getPreferences(MODE_PRIVATE).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
        application.deleteObserver(this);
        getPreferences(MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void update(Observable o, int qualifier, String data) {
        switch (qualifier) {
            case ApplicationClass.ONE_TO_ONE_MESSAGE_RECEIVED:
            case ApplicationClass.GROUP_MESSAGE_RECEIVED: {
                ChatChannel chatChannel = application.getChannel(data);
                ChatMessage lastMessage = application.getLastMessage(data);
                if (chatChannel != null && lastMessage != null) {
                    MessageNotificationHelper.showNotification(this, chatChannel.getName(),
                            chatChannel.getLastMessage(), chatChannel.getId());
                }
            } break;
        }
    }

    public static class MyPreferenceFragment extends PreferenceFragment {

        private CoordinatorLayout coordinatorLayout;

        public void setCoordinatorLayout(CoordinatorLayout coordinatorLayout) {
            this.coordinatorLayout = coordinatorLayout;
        }

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);

            Preference passwordPreference = findPreference(PREFERENCE_PASSWORD);
            passwordPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    firebaseAuth.sendPasswordResetEmail(firebaseAuth.getCurrentUser().getEmail())
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        DialogHelper.showInformationMessage(coordinatorLayout, getString(R.string.reset_password_instructions_sent));
                                    } else {
                                        DialogHelper.showInformationMessage(coordinatorLayout, getString(R.string.reset_password_instructions_not_sent));
                                    }
                                }
                            });
                    return true;
                }
            });
        }
    }
}
