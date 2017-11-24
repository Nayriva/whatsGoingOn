package ee.ut.madp.whatsgoingon.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.apache.commons.lang3.StringUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;
import ee.ut.madp.whatsgoingon.ApplicationClass;
import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.chat.Observable;
import ee.ut.madp.whatsgoingon.chat.Observer;
import ee.ut.madp.whatsgoingon.helpers.DialogHelper;
import ee.ut.madp.whatsgoingon.helpers.ImageHelper;
import ee.ut.madp.whatsgoingon.helpers.MessageNotificationHelper;
import ee.ut.madp.whatsgoingon.models.ChatChannel;
import ee.ut.madp.whatsgoingon.models.ChatMessage;
import ee.ut.madp.whatsgoingon.models.User;

public class UserProfileActivity extends AppCompatActivity implements Observer {

    private static final String TAG = UserProfileActivity.class.getSimpleName();
    public static final String EXTRA_STRING_USER_ID = "EXTRA_STRING_USER_ID";
    public static final String EXTRA_STRING_USER_NAME = "EXTRA_STRING_USER_NAME";

    private ApplicationClass application;

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.iw_profile_photo) CircleImageView profilePhoto;
    @BindView(R.id.tw_username) TextView username;
    @BindView(R.id.tw_email) TextView email;
    @BindView(R.id.tw_nationality) TextView nationality;
    @BindView(R.id.tw_city) TextView city;
    @BindView(R.id.tw_phone_number) TextView phoneNumber;
    @BindView(R.id.tw_school) TextView school;
    @BindView(R.id.tw_work) TextView work;
    @BindView(R.id.tw_birthday) TextView birthday;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        application = (ApplicationClass) getApplication();

        initializeGuiParts();

        Intent intent = getIntent();
        final String userId = intent.getStringExtra(EXTRA_STRING_USER_ID);
        String userName = intent.getStringExtra(EXTRA_STRING_USER_NAME);
        setTitle(userName);

        DatabaseReference userReference = FirebaseDatabase.getInstance().getReference().child("users").child(userId);
        DialogHelper.showProgressDialog(this, "Loading data");
        userReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.i(TAG, "onDataChange");
                User user = dataSnapshot.getValue(User.class);
                if (user != null) {
                    Log.i(TAG, "onDataChange - user found");
                    setContent(user);
                }
                DialogHelper.hideProgressDialog();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                //left intentionally blank
            }
        });
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

    @Override
    protected void onResume() {
        super.onResume();
        application.addObserver(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        application.deleteObserver(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return  true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initializeGuiParts() {
        Log.i(TAG, "initializeGuiParts");
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }
    }

    private void setContent(User user) {
        profilePhoto.setImageBitmap(ImageHelper.decodeBitmap(user.getPhoto()));
        if (StringUtils.isNotBlank(user.getName())) username.setText(user.getName());
        if (StringUtils.isNotBlank(user.getEmail())) email.setText(user.getEmail());
        if (StringUtils.isNotBlank(user.getNationality()))
            nationality.setText(user.getNationality());
        if (StringUtils.isNotBlank(user.getCity())) city.setText(user.getCity());
        if (StringUtils.isNotBlank(user.getPhoneNumber()))
            phoneNumber.setText(user.getPhoneNumber());
        if (StringUtils.isNotBlank(user.getWork())) work.setText(user.getWork());
        if (StringUtils.isNotBlank(user.getSchool())) school.setText(user.getSchool());
        if (StringUtils.isNotBlank(user.getBirthday())) birthday.setText(user.getBirthday());
    }
}
