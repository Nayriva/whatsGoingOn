package ee.ut.madp.whatsgoingon.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
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
import de.hdodenhof.circleimageview.CircleImageView;
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

    private static final String TAG = MyProfileActivity.class.getSimpleName();

    private ApplicationClass application;
    private User user;
    private String photo;
    private boolean photoChanged;
    private Map<String, String> backupValues;

    private DatabaseReference userReference;

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.iw_profile_photo) CircleImageView profilePhoto;
    @BindView(R.id.tw_username) TextView username;
    @BindView(R.id.tw_email) TextView email;
    @BindView(R.id.et_nationality) EditText nationality;
    @BindView(R.id.et_city) EditText city;
    @BindView(R.id.et_phone_number) EditText phoneNumber;
    @BindView(R.id.et_school) EditText school;
    @BindView(R.id.et_work) EditText work;
    @BindView(R.id.et_birthday) EditText birthday;
    @BindView(R.id.fab_edit_profile) FloatingActionButton editButton;
    @BindView(R.id.fab_finish_edit_profile) FloatingActionButton finishButton;
    @BindView(R.id.fab_discard_edit_profile) FloatingActionButton discardButton;
    @BindView(R.id.ll_edit_active_btns) LinearLayout editActiveLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
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
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        initializeGuiParts();
        fillInitialInfo();
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
        application.addObserver(this);
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
        application.deleteObserver(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult: " + requestCode + ", " + resultCode + ", " + data);
        Bitmap bitmap = ImagePicker.getImageFromResult(this, requestCode, resultCode, data);
        if (bitmap != null) {
            profilePhoto.setImageBitmap(bitmap);
            photo = ImageHelper.encodeBitmap(bitmap);
            photoChanged = true;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "onOptionsItemSelected");
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return  true;
        }
        return super.onOptionsItemSelected(item);
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
        Log.i(TAG, "initializeGuiParts");
        editActiveLayout.setVisibility(View.INVISIBLE);

        setETEditable(false);
        setFABListeners();
    }

    private void fillInitialInfo() {
        Log.i(TAG, "fillInitialInfo");
        DialogHelper.showProgressDialog(this, getString(R.string.downloading_data));
        userReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.i(TAG, "fillInitialInfo.onDataChange");
                user = dataSnapshot.getValue(User.class);
                if (user != null) {
                    username.setText(user.getName());
                    email.setText(user.getEmail());
                    backUpValues();
                    setBackedUpValues();
                }
                DialogHelper.hideProgressDialog();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                //intentionally left blank
            }
        });
    }

    private void setBackedUpValues() {
        Log.i(TAG, "setBackedUpValues");
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

    private void backUpValues() {
        Log.i(TAG, "backUpValues");
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
        Log.i(TAG, "setFABListeners");
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "editButton.onClick");
                setETEditable(true);
                editButton.setVisibility(View.INVISIBLE);
                editActiveLayout.setVisibility(View.VISIBLE);
            }
        });
        finishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "finishButton.onClick");
                setETEditable(false);
                editActiveLayout.setVisibility(View.INVISIBLE);
                editButton.setVisibility(View.VISIBLE);
                storeData();
            }
        });
        discardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "discardButton.onClick");
                setETEditable(false);
                setBackedUpValues();
                editActiveLayout.setVisibility(View.INVISIBLE);
                editButton.setVisibility(View.VISIBLE);
            }
        });
    }

    private void storeData() {
        Log.i(TAG, "storeData");
        if (photoChanged) {
            user.setPhoto(ImageHelper.encodeBitmap(((BitmapDrawable)profilePhoto.getDrawable()).getBitmap()));
            photoChanged = false;
        }
        user.setNationality(String.valueOf(nationality.getText()));
        user.setPhoneNumber(String.valueOf(phoneNumber.getText()));
        user.setWork(String.valueOf(work.getText()));
        user.setSchool(String.valueOf(school.getText()));
        user.setBirthday(String.valueOf(birthday.getText()));

        backUpValues();

        userReference.setValue(user);
    }

    public void setETEditable(boolean editable) {
        Log.i(TAG, "setETEditable: " + editable);
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
        Log.i(TAG, "pickUserProfilePhoto");
        ImagePicker.pickImage(this, getString(R.string.choose_photo));
    }
}
