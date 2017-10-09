package ee.ut.madp.whatsgoingon.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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
import ee.ut.madp.whatsgoingon.FirebaseExceptionsChecker;
import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.helpers.DialogHelper;
import ee.ut.madp.whatsgoingon.helpers.ImageHelper;
import ee.ut.madp.whatsgoingon.helpers.MyTextWatcherHelper;

import static ee.ut.madp.whatsgoingon.activities.SplashActivity.TAG;
import static ee.ut.madp.whatsgoingon.helpers.UserHelper.saveNewUser;

public class SignupActivity extends AppCompatActivity implements Validator.ValidationListener{

    @NotEmpty @Email @BindView(R.id.input_layout_email) TextInputLayout email;
    @NotEmpty @BindView(R.id.input_layout_username) TextInputLayout name;
    @NotEmpty @Password(min = 6, scheme = Password.Scheme.ANY, messageResId = R.string.invalid_password_format)
    @BindView(R.id.input_layout_password) TextInputLayout password;

    @NotEmpty @ConfirmPassword @BindView(R.id.input_layout_repeat_password) TextInputLayout passwordAgain;
    @BindView(R.id.btn_register) Button signUpButton;
    @BindView(R.id.user_profile_photo) CircleImageView photo;
    @BindView(R.id.input_username) TextInputEditText usernameInput;
    @BindView(R.id.input_email) TextInputEditText emailInput;
    @BindView(R.id.input_password) TextInputEditText passwordInput;

    private String profilePhoto = "";

    private List<TextInputLayout> inputLayoutList;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference firebaseDatabase;
    private Resources res;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        ButterKnife.bind(this);

        Validator validator = new Validator(this);
        validator.setValidationListener(this);

        validator.registerAdapter(TextInputLayout.class,
                new ViewDataAdapter<TextInputLayout, String>() {
                    @Override
                    public String getData(TextInputLayout flet) throws ConversionException {
                        return flet.getEditText().getText().toString();
                    }
                }
        );

        inputLayoutList = Arrays.asList(name, email, password, passwordAgain);

        MyTextWatcherHelper.setTextWatcherListeners(inputLayoutList, validator);

        signUpButton.setEnabled(false);

        ImagePicker.setMinQuality(600, 600);
        firebaseAuth = FirebaseAuth.getInstance();
        res = getResources();
        context = getApplicationContext();
    }

    @Override
    public void onValidationSucceeded() {
        signUpButton.setEnabled(true);
        signUpButton.setAlpha(1);
        MyTextWatcherHelper.clearAllInputs(inputLayoutList);
    }

    @Override
    public void onValidationFailed(List<ValidationError> errors) {
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

    @OnClick(R.id.user_profile_photo)
    public void addUserProfilePhoto() {
        ImagePicker.pickImage(this, getString(R.string.choose_photo));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Bitmap bitmap = ImagePicker.getImageFromResult(this, requestCode, resultCode, data);
        if (bitmap != null) {
            photo.setImageBitmap(bitmap);
            profilePhoto = ImageHelper.encodeBitmap(bitmap);
        }
    }

    @OnClick(R.id.btn_register)
    public void register() {
        Log.i(TAG, "register()");
        String username = String.valueOf(usernameInput.getText());
        String email = String.valueOf(emailInput.getText());
        String password = String.valueOf(passwordInput.getText());
        createNewUser(email, password, username, profilePhoto);
    }

    private void createNewUser(String email, String password, final String name, final String photo) {
        DialogHelper.showProgressDialog(SignupActivity.this, getString(R.string.progress_dialog_title_signup));

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "createUserWithEmail: onComplete:" + task.isSuccessful());
                        if (!task.isSuccessful()) {
                            FirebaseExceptionsChecker.checkFirebaseAuth(context, task);
                            DialogHelper.hideProgressDialog();
                        } else {
                            onAuthSuccess(name, task.getResult().getUser(), photo);
                        }

                    }
                });
    }

    private void onAuthSuccess( String name, FirebaseUser firebaseUser, String photo) {
        saveNewUser(name, firebaseUser, photo);
        if (firebaseUser != null) {
            // TODO store user information to shared prefences
        }
        setResult(Activity.RESULT_OK);
        DialogHelper.hideProgressDialog();
        finish();
    }


    public DatabaseReference getFirebaseDatabase() {
        if (firebaseDatabase == null) {
            firebaseDatabase = FirebaseDatabase.getInstance().getReference();
        }
        return firebaseDatabase;
    }
}
