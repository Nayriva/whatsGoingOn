package ee.ut.madp.whatsgoingon.activities;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mvc.imagepicker.ImagePicker;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.chat.ChatApplication;
import ee.ut.madp.whatsgoingon.chat.Observable;
import ee.ut.madp.whatsgoingon.chat.Observer;
import ee.ut.madp.whatsgoingon.constants.FirebaseConstants;
import ee.ut.madp.whatsgoingon.helpers.ImageHelper;
import ee.ut.madp.whatsgoingon.models.User;

public class MyProfileActivity extends AppCompatActivity implements Observer {

    private ChatApplication application;
    private FirebaseAuth firebaseAuth;
    private FirebaseDatabase firebaseDatabase;
    private Resources res;
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

        application = (ChatApplication) getApplication();
        application.addObserver(this);
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        userReference = firebaseDatabase.getReference().child(FirebaseConstants.FIREBASE_CHILD_USERS)
                .child(firebaseAuth.getCurrentUser().getUid());
        res = getResources();
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
            case ChatApplication.ONE_TO_ONE_MESSAGE_RECEIVED:
            case ChatApplication.GROUP_MESSAGE_RECEIVED: {
                //TODO show notification
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
        userReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                user = dataSnapshot.getValue(User.class);
                assert user != null;
                //user is loggedIn so it shouldn't be null and always has name and email
                username.setText(user.getName());
                email.setText(user.getEmail());
                backupValues();
                setBackupValues();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                //intentionally left blank
            }
        });
    }

    private void setBackupValues() {
        photo = backupValues.get("photo");
        if (photo.contains("http")) {
            Picasso.with(getApplicationContext()).load(photo).into(profilePhoto);
        } else {
            profilePhoto.setImageBitmap(ImageHelper.decodeBitmap(photo));
        }
        nationality.setText(user.getNationality() == null
                ? "" : backupValues.get("nationality"));
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
                setBackupValues();
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
