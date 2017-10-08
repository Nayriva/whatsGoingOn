package ee.ut.madp.whatsgoingon.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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
import ee.ut.madp.whatsgoingon.FirebaseApplication;
import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.constants.SettingsConstants;
import ee.ut.madp.whatsgoingon.helpers.FontHelper;
import ee.ut.madp.whatsgoingon.helpers.ImageHelper;
import ee.ut.madp.whatsgoingon.helpers.ModelHelper;
import ee.ut.madp.whatsgoingon.helpers.MyTextWatcherHelper;

public class RegisterActivity extends AppCompatActivity implements Validator.ValidationListener{

    private static final int REQUEST_CODE_PICKER = 1;
    @NotEmpty
    @Email
    @BindView(R.id.input_layout_email)
    TextInputLayout email;

    @NotEmpty
    @BindView(R.id.input_layout_username)
    TextInputLayout name;

    @NotEmpty
    @Password(min = 6, scheme = Password.Scheme.ANY, messageResId = R.string.invalid_password_format)
    @BindView(R.id.input_layout_password)
    TextInputLayout password;

    @NotEmpty
    @ConfirmPassword
    @BindView(R.id.input_layout_repeat_password)
    TextInputLayout passwordAgain;

    @BindView(R.id.btn_register)
    Button signUpButton;

    @BindView(R.id.register_title)
    TextView registerTitle;

    @BindView(R.id.user_profile_photo)
    CircleImageView photo;

    private String profilePhoto = "";

    private Validator validator;
    private List<TextInputLayout> inputLayoutList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        ButterKnife.bind(this);

        FontHelper.setFont(this, registerTitle, SettingsConstants.CUSTOM_FONT);

        validator = new Validator(this);
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

    @OnClick(R.id.btn_register)
    void registerNewUser() {
        validator.validate();
        ((FirebaseApplication) getApplication()).createNewUser(RegisterActivity.this, ModelHelper.getProperty(email),
                ModelHelper.getProperty(password), ModelHelper.getProperty(name), profilePhoto);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Bitmap bitmap = ImagePicker.getImageFromResult(this, requestCode, resultCode, data);
        if (bitmap != null) {
            photo.setImageBitmap(bitmap);
            profilePhoto = ImageHelper.encodeBitmap(bitmap);
        }
    }


}
