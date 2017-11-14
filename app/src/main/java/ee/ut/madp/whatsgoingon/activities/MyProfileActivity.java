package ee.ut.madp.whatsgoingon.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mvc.imagepicker.ImagePicker;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.ApplicationClass;
import ee.ut.madp.whatsgoingon.chat.Observable;
import ee.ut.madp.whatsgoingon.chat.Observer;
import ee.ut.madp.whatsgoingon.constants.FirebaseConstants;
import ee.ut.madp.whatsgoingon.helpers.DialogHelper;
import ee.ut.madp.whatsgoingon.helpers.ImageHelper;
import ee.ut.madp.whatsgoingon.helpers.MessageNotificationHelper;
import ee.ut.madp.whatsgoingon.models.ChatChannel;
import ee.ut.madp.whatsgoingon.models.ChatMessage;
import ee.ut.madp.whatsgoingon.models.User;

public class MyProfileActivity extends AppCompatActivity implements Observer {

    private ApplicationClass application;
    private User user;
    private String photo;
    private boolean photoChanged;
    private Map<String, String> backupValues;

    private DatabaseReference userReference;

    @BindView(R.id.toolbar) Toolbar toolbar;
    private ImageView profilePhoto;
    private TextView username;
    private TextView email;
    private EditText nationality;
    private EditText city;
    private EditText phoneNumber;
    private EditText school;
    private EditText work;
    private EditText birthday;
    private FloatingActionButton editButton;
    private FloatingActionButton finishButton;
    private FloatingActionButton discardButton;
    private LinearLayout editActiveLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_profile);

        application = (ApplicationClass) getApplication();
        application.addObserver(this);
        userReference = FirebaseDatabase.getInstance().getReference()
                .child(FirebaseConstants.FIREBASE_CHILD_USERS)
                .child(application.getLoggedUser().getId());
        photoChanged = false;

        ButterKnife.bind(this);

        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        initializeGuiParts();
        fillInitialInfo();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        application.deleteObserver(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Bitmap bitmap = ImagePicker.getImageFromResult(this, requestCode, resultCode, data);
        if (bitmap != null) {
            profilePhoto.setImageBitmap(bitmap);
            photo = ImageHelper.encodeBitmap(bitmap);
            photoChanged = true;
        }
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

    private void initializeGuiParts() {
        editActiveLayout = (LinearLayout) findViewById(R.id.lyt_edit_active_btns);
        editActiveLayout.setVisibility(View.INVISIBLE);

        profilePhoto = (ImageView) findViewById(R.id.iw_profile_photo);
        username = (TextView) findViewById(R.id.tw_username);
        email = (TextView) findViewById(R.id.et_email);
        nationality = (EditText) findViewById(R.id.et_nationality);
        city = (EditText) findViewById(R.id.et_city);
        phoneNumber = (EditText) findViewById(R.id.et_phone_number);
        work = (EditText) findViewById(R.id.et_work);
        school = (EditText) findViewById(R.id.et_school);
        birthday = (EditText) findViewById(R.id.et_birthday);

        setETEditable(false);

        editButton = (FloatingActionButton) findViewById(R.id.fab_edit_profile);
        finishButton = (FloatingActionButton) findViewById(R.id.fab_finish_edit_profile);
        discardButton = (FloatingActionButton) findViewById(R.id.fab_discard_edit_profile);
        setFABListeners();
    }

    private void fillInitialInfo() {
        DialogHelper.showProgressDialog(this, getString(R.string.downloading_data));
        userReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                user = dataSnapshot.getValue(User.class);
                assert user != null;
                //user is loggedIn so it shouldn't be null and always has name and email
                username.setText(user.getName());
                email.setText(user.getEmail());
                backupValues();
                setBackedUpValues();
                DialogHelper.hideProgressDialog();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                //intentionally left blank
            }
        });
    }

    private void setBackedUpValues() {
        photo = backupValues.get("photo");
        if (photo != null) {
            profilePhoto.setImageBitmap(ImageHelper.decodeBitmap(photo));
        }
        nationality.setText(user.getNationality() == null
                ? "" : backupValues.get("nationality"));
        city.setText(user.getNationality() == null
                ? "" : backupValues.get("city"));
        phoneNumber.setText(user.getPhoneNumber() == null
                ? "" : backupValues.get("phoneNumber"));
        work.setText(user.getWork() == null
                ? "" : backupValues.get("work"));
        school.setText(user.getSchool() == null
                ? "" : backupValues.get("school"));
        birthday.setText(user.getBirthday() == null
                ? "" : backupValues.get("birthday"));
    }

    private void backupValues() {
        backupValues = new HashMap<>();
        backupValues.put("nationality", user.getNationality());
        backupValues.put("phoneNumber", user.getPhoneNumber());
        backupValues.put("work", user.getWork());
        backupValues.put("school", user.getSchool());
        backupValues.put("birthday", user.getBirthday());
        backupValues.put("photo", user.getPhoto());
        backupValues.put("city", user.getCity());
    }

    private void setFABListeners() {
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setETEditable(true);
                editButton.setVisibility(View.INVISIBLE);
                editActiveLayout.setVisibility(View.VISIBLE);
            }
        });
        finishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setETEditable(false);
                editActiveLayout.setVisibility(View.INVISIBLE);
                editButton.setVisibility(View.VISIBLE);
                storeData();
            }
        });
        discardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setETEditable(false);
                setBackedUpValues();
                editActiveLayout.setVisibility(View.INVISIBLE);
                editButton.setVisibility(View.VISIBLE);
            }
        });
    }

    private void storeData() {
        if (photoChanged) {
            user.setPhoto(ImageHelper.encodeBitmap(((BitmapDrawable)profilePhoto.getDrawable()).getBitmap()));
            photoChanged = false;
        }
        user.setNationality(String.valueOf(nationality.getText()));
        user.setPhoneNumber(String.valueOf(phoneNumber.getText()));
        user.setWork(String.valueOf(work.getText()));
        user.setSchool(String.valueOf(school.getText()));
        user.setBirthday(String.valueOf(birthday.getText()));

        backupValues();

        userReference.setValue(user);
    }

    public void setETEditable(boolean editable) {
        nationality.setEnabled(editable);
        phoneNumber.setEnabled(editable);
        work.setEnabled(editable);
        school.setEnabled(editable);
        birthday.setEnabled(editable);

        if (editable) {
            profilePhoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    pickUserProfilePhoto();
                }
            });
        } else {
            profilePhoto.setOnClickListener(null);
        }
    }

    private void pickUserProfilePhoto() {
        ImagePicker.pickImage(this, getString(R.string.choose_photo));
    }
}
