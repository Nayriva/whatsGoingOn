package ee.ut.madp.whatsgoingon.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.mobsandgeeks.saripaar.ValidationError;
import com.mobsandgeeks.saripaar.Validator;
import com.mobsandgeeks.saripaar.adapter.ViewDataAdapter;
import com.mobsandgeeks.saripaar.annotation.ConfirmPassword;
import com.mobsandgeeks.saripaar.annotation.Email;
import com.mobsandgeeks.saripaar.annotation.NotEmpty;
import com.mobsandgeeks.saripaar.annotation.Password;
import com.mobsandgeeks.saripaar.exception.ConversionException;
import com.mvc.imagepicker.ImagePicker;

import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;
import ee.ut.madp.whatsgoingon.ApplicationClass;
import ee.ut.madp.whatsgoingon.FirebaseExceptionsChecker;
import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.helpers.DialogHelper;
import ee.ut.madp.whatsgoingon.helpers.ImageHelper;
import ee.ut.madp.whatsgoingon.helpers.MyTextWatcherHelper;
import ee.ut.madp.whatsgoingon.helpers.UserHelper;

/**
 * Activity with options for registering new user.
 */
public class SignUpActivity extends AppCompatActivity implements Validator.ValidationListener {

    private static final String TAG = SignUpActivity.class.getSimpleName();
    public static final String USER_HAS_REGISTERED_EXTRA = "USER_HAS_REGISTERED_EXTRA";
    private static final String SELECTED_PHOTO_EXTRA = "PHOTO_PHOTO_EXTRA";

    @NotEmpty @Email @BindView(R.id.input_layout_email) TextInputLayout email;
    @NotEmpty @BindView(R.id.input_layout_username) TextInputLayout name;
    @NotEmpty @Password(min = 6, scheme = Password.Scheme.ANY, messageResId = R.string.invalid_password_format)
    @BindView(R.id.input_layout_password) TextInputLayout password;
    @NotEmpty @ConfirmPassword @BindView(R.id.input_layout_repeat_password) TextInputLayout passwordAgain;
    @BindView(R.id.btn_register) Button signUpButton;

    @BindView(R.id.iw_profile_photo) CircleImageView photo;
    @BindView(R.id.input_username) TextInputEditText usernameInput;
    @BindView(R.id.input_email) TextInputEditText emailInput;
    @BindView(R.id.input_password) TextInputEditText passwordInput;
    @BindView(R.id.tw_login_link) TextView loginLink;

    private String profilePhoto = "";
    private List<TextInputLayout> inputLayoutList;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        ButterKnife.bind(this);

        if (savedInstanceState != null) {
            profilePhoto = savedInstanceState.getString(SELECTED_PHOTO_EXTRA);
            photo.setImageBitmap(ImageHelper.decodeBitmap(profilePhoto));
        }

        setUpValidation();

        signUpButton.setEnabled(false);

        ImagePicker.setMinQuality(600, 600);
        firebaseAuth = FirebaseAuth.getInstance();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.i(TAG, "onSaveInstanceState");
        super.onSaveInstanceState(outState);
        outState.putString(SELECTED_PHOTO_EXTRA, profilePhoto);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult: " + requestCode + ", " + resultCode + ", " + data);
        Bitmap bitmap = ImagePicker.getImageFromResult(this, requestCode, resultCode, data);
        if (bitmap != null) {
            photo.setImageBitmap(bitmap);
            profilePhoto = ImageHelper.encodeBitmap(bitmap);
        } else {
            bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.user);
            photo.setImageBitmap(bitmap);
            profilePhoto = ImageHelper.encodeBitmap(bitmap);
        }
    }

    @Override
    public void onValidationSucceeded() {
        signUpButton.setEnabled(true);
        signUpButton.setAlpha(1);
        MyTextWatcherHelper.clearAllInputs(inputLayoutList);
    }

    @Override
    public void onValidationFailed(List<ValidationError> errors) {
        Log.i(TAG, "onValidationFailed: " + errors);
        MyTextWatcherHelper.clearAllInputs(inputLayoutList);
        for (ValidationError error : errors) {
            View view = error.getView();
            String message = error.getCollatedErrorMessage(this);

            if (view instanceof TextInputLayout) {
                ((TextInputLayout) view).setErrorEnabled(true);
                ((TextInputLayout) view).setError(message);
            } else {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        }
    }

    @OnClick(R.id.iw_profile_photo)
    public void addUserProfilePhoto() {
        Log.i(TAG, "addUserProfilePhoto");
        ImagePicker.pickImage(this, getString(R.string.choose_photo));
    }

    @OnClick(R.id.btn_register)
    public void signUp() {
        Log.i(TAG, "signUp");
        String username = String.valueOf(usernameInput.getText());
        String email = String.valueOf(emailInput.getText());
        String password = String.valueOf(passwordInput.getText());
        if (profilePhoto == null) {
            profilePhoto = ImageHelper.encodeBitmap(
                    BitmapFactory.decodeResource(getResources(), R.drawable.user));
        }
        createNewUser(email, password, username, profilePhoto);
    }

    @OnClick(R.id.tw_login_link)
    public void toLogin() {
        Log.i(TAG, "toLogin");
        Intent intent = new Intent();
        intent.putExtra(USER_HAS_REGISTERED_EXTRA, false);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void createNewUser(String email, String password, final String name, final String photo) {
        Log.i(TAG, "createNewUser: " + email);
        DialogHelper.showProgressDialog(SignUpActivity.this, getString(R.string.progress_dialog_title_signup));

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.i(TAG, "createUserWithEmail: onComplete:" + task.isSuccessful());
                        if (!task.isSuccessful()) {
                            FirebaseExceptionsChecker.checkFirebaseAuth(SignUpActivity.this, task);
                            DialogHelper.hideProgressDialog();
                        } else {
                            onAuthSuccess(name, task.getResult().getUser(), photo);
                        }
                    }
                });
    }

    private void onAuthSuccess(final String displayName, FirebaseUser firebaseUser, String photo) {
        Log.i(TAG, "onAuthSuccess: " + displayName + ", " + firebaseUser);
        UserProfileChangeRequest request = new UserProfileChangeRequest.Builder().setDisplayName(displayName).build();
        firebaseUser.updateProfile(request);
        if (photo == null || photo.isEmpty()) {
            photo = ImageHelper.encodeBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.user));
        }
        UserHelper.saveNewUserToDB(displayName, firebaseUser, photo, false);
        DialogHelper.hideProgressDialog();
        Intent intent = new Intent();
        intent.putExtra(USER_HAS_REGISTERED_EXTRA, true);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void setUpValidation() {
        Log.i(TAG, "setUpValidation");
        Validator validator = new Validator(this);
        validator.setValidationListener(this);

        validator.registerAdapter(TextInputLayout.class,
                new ViewDataAdapter<TextInputLayout, String>() {
                    @Override
                    public String getData(TextInputLayout flet) throws ConversionException {
                        if (flet.getEditText() != null) {
                            return flet.getEditText().getText().toString();
                        } else {
                            return null;
                        }
                    }
                }
        );

        inputLayoutList = Arrays.asList(name, email, password, passwordAgain);

        MyTextWatcherHelper.setTextWatcherListeners(inputLayoutList, validator);
    }
}
