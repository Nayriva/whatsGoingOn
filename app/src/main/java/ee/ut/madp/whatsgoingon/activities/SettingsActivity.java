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
import android.view.MenuItem;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.mvc.imagepicker.ImagePicker;

import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.chat.ChatApplication;
import ee.ut.madp.whatsgoingon.chat.Observable;
import ee.ut.madp.whatsgoingon.chat.Observer;
import ee.ut.madp.whatsgoingon.helpers.DialogHelper;

public class SettingsActivity extends AppCompatPreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener, Observer {

    public static final String PREFERENCE_EMAIL = "email";
    public static final String PREFERENCE_PASSWORD = "password";
    private static final String PREFERENCE_PHOTO = "profile_photo";
    private static final String PREFERENCE_MESSAGE_NOTIFICATION = "notifications_message";
    private static final String PREFERENCE_NOTIFICATION_VIBRATE = "notification_vibrate";
    private static final String PREFERENCE_NOTIFICATION_RINGTONE = "notification_ringtone";

    private static FirebaseAuth firebaseAuth;
    private static DatabaseReference databaseReference;
    private static CoordinatorLayout coordinatorLayout;
    private Intent intent;
    private ChatApplication application;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        application = (ChatApplication) getApplication();
        application.addObserver(this);

        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();

        firebaseAuth = FirebaseAuth.getInstance();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        application.deleteObserver(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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
        SharedPreferences prefs = getSharedPreferences("setting.whatsgoingon",
                Context.MODE_WORLD_READABLE);
        SharedPreferences.Editor editor = prefs.edit();
        if (key.equals(PREFERENCE_MESSAGE_NOTIFICATION)) {
            boolean isAllowed = sharedPreferences.getBoolean(key, true);
            editor.putBoolean(PREFERENCE_MESSAGE_NOTIFICATION, isAllowed);
            editor.apply();
        }

        if (key.equals(PREFERENCE_NOTIFICATION_VIBRATE)) {
            boolean isAllowed = sharedPreferences.getBoolean(key, true);
            editor.putBoolean(PREFERENCE_NOTIFICATION_VIBRATE, isAllowed);
            editor.commit();
        }

        if (key.equals(PREFERENCE_NOTIFICATION_RINGTONE)) {
            String ringtonePreference = sharedPreferences.getString("notification_ringtone", "DEFAULT_SOUND");
            editor.putString(PREFERENCE_NOTIFICATION_RINGTONE, ringtonePreference);
            editor.commit();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Bitmap bitmap = ImagePicker.getImageFromResult(this, requestCode, resultCode, data);
        if (bitmap != null) {
            // TODO save photo to the external storage
            String userId = firebaseAuth.getCurrentUser().getUid();
           // storeUserProfilePhotoToStorage(userId, );

//            intent = getIntent();
//            intent.putExtra(UPDATED_PHOTO, UPDATED_PHOTO);
            DialogHelper.showInformationMessage(coordinatorLayout, getString(R.string.profile_photo_updated));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPreferences(MODE_PRIVATE).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferences(MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void update(Observable o, int qualifier, String data) {
        switch (qualifier) {
            case ChatApplication.ONE_TO_ONE_MESSAGE_RECEIVED:
            case ChatApplication.GROUP_MESSAGE_RECEIVED: {
                //TODO show notification
            } break;
        }
    }

    public static class MyPreferenceFragment extends PreferenceFragment {

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);

            Preference profilePreference = findPreference(PREFERENCE_PHOTO);
            profilePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    ImagePicker.pickImage(getActivity(), getString(R.string.choose_photo));
                    return true;
                }
            });

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
                                        //DialogHelper.hideProgressDialog();
                                    } else {
                                        DialogHelper.showInformationMessage(coordinatorLayout, getString(R.string.reset_password_instructions_not_sent));
                                    }

                                }
                            });
                    return true;
                }
            });

            //TODO udpate email

        }


    }

    private void storeUserProfilePhotoToStorage(String userId, byte[] newImage) {
        //TODO implement
    }
}
