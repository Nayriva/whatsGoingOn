package ee.ut.madp.whatsgoingon.activities;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Window;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ee.ut.madp.whatsgoingon.FirebaseExceptionsChecker;
import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.ApplicationClass;
import ee.ut.madp.whatsgoingon.constants.GeneralConstants;
import ee.ut.madp.whatsgoingon.helpers.DialogHelper;
import ee.ut.madp.whatsgoingon.helpers.FontHelper;
import ee.ut.madp.whatsgoingon.helpers.ImageHelper;
import ee.ut.madp.whatsgoingon.helpers.UserHelper;
import ee.ut.madp.whatsgoingon.models.User;

import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.GOOGLE_SIGN_IN_REQUEST_CODE;
import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.SIGN_UP_REQUEST_CODE;

public class LoginActivity extends AppCompatActivity
        implements GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = LoginActivity.class.getSimpleName();
    private ApplicationClass application;

    @BindView(R.id.coordinator_layout) CoordinatorLayout coordinatorLayout;
    @BindView(R.id.input_email) TextInputEditText emailInput;
    @BindView(R.id.input_password) TextInputEditText passwordInput;
    @BindView(R.id.login_title) TextView loginTitle;
    @BindView(R.id.btn_facebook) LoginButton facebookButton;

    private GoogleApiClient mGoogleApiClient;
    private FirebaseAuth firebaseAuth;
    private CallbackManager callbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
        FontHelper.setFont(this, loginTitle, GeneralConstants.CUSTOM_FONT);

        callbackManager = CallbackManager.Factory.create();

        application = (ApplicationClass) getApplication();
        initializeAuth();
    }

    @Override
    public void onStart() {
        Log.i(TAG, "onStart");
        super.onStart();
        if (checkUserLogin()) {
            firebaseAuth.signOut();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);

        callbackManager.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GOOGLE_SIGN_IN_REQUEST_CODE) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
            }
        }

        if (requestCode == SIGN_UP_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                DialogHelper.showInformationMessage(coordinatorLayout, getString(R.string.success_signup));
            }
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        //left blank intentionally
    }

    @OnClick(R.id.btn_login)
    public void signIn() {
        Log.i(TAG, "signIn()");
        String email = String.valueOf(emailInput.getText());
        String password = String.valueOf(passwordInput.getText());
        firebaseAuthWithEmail(email, password);
    }

    @OnClick(R.id.btn_google)
    public void signInGoogle() {
        Log.i(TAG, "signInGoogle()");
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, GOOGLE_SIGN_IN_REQUEST_CODE);
    }

    @OnClick(R.id.register_link)
    public void register() {
        Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
        startActivityForResult(intent, SIGN_UP_REQUEST_CODE);
    }

    private void initializeAuth() {
        Log.i(TAG, "initializeAuth()");
        firebaseAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.web_client_id))
                .requestEmail()
                .requestId()
                .requestProfile()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        facebookButton.setReadPermissions("email", "public_profile");
        facebookButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(TAG, "facebook:onSuccess:" + loginResult);
                firebaseAuthWithFacebook(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "facebook:onCancel");
            }

            @Override
            public void onError(FacebookException error) {
                Log.d(TAG, "facebook:onError", error);
                DialogHelper.showAlertDialog(LoginActivity.this, getString(R.string.occur_error));
            }
        });
    }

    private void firebaseAuthWithEmail(String email, String password) {

        DialogHelper.showProgressDialog(LoginActivity.this, getString(R.string.progress_dialog_title_signup));

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener( new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (!task.isSuccessful()) {
                            Log.e(TAG, "signInWithEmail", task.getException());
                            FirebaseExceptionsChecker.checkFirebaseAuth(LoginActivity.this, task);
                        } else if (checkUserLogin()) {
                            String userId = task.getResult().getUser().getUid();
                            Log.d(TAG, "loginUser: user with id " + userId + "was logged");
                            startMainActivity();
                        }
                       DialogHelper.hideProgressDialog();
                    }
                });
    }

    private void firebaseAuthWithGoogle(final GoogleSignInAccount acc) {
        Log.i(TAG, "firebaseAuthWithGoogle: " + acc );

        DialogHelper.showProgressDialog(LoginActivity.this, getString(R.string.progress_dialog_title_login));
        AuthCredential credential = GoogleAuthProvider.getCredential(acc.getIdToken(), null);

        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());
                        if (!task.isSuccessful()) {
                            Log.d(TAG, "signInWithCredential", task.getException());
                            DialogHelper.showAlertDialog(LoginActivity.this, getString(R.string.unsuccessful_login));
                        } else {
                            UserHelper.saveGoogleInfoAboutUser(LoginActivity.this, task.getResult().getUser(),
                                    acc.getPhotoUrl().toString());
                            if (checkUserLogin()) {
                                startMainActivity();
                            }
                        }

                        DialogHelper.hideProgressDialog();
                    }
                });
    }

    private void firebaseAuthWithFacebook(AccessToken token) {
        Log.d(TAG, "firebaseAuthWithFacebook:" + token);

        DialogHelper.showProgressDialog(LoginActivity.this, getString(R.string.progress_dialog_title_login));
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());

        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());
                        if (!task.isSuccessful()) {
                            Log.d(TAG, "signInWithCredential", task.getException());
                            DialogHelper.showAlertDialog(LoginActivity.this, getString(R.string.unsuccessful_login));
                        } else {
                            UserHelper.saveFacebookInfoAboutUser(LoginActivity.this, task.getResult().getUser());
                            if (checkUserLogin()) {
                                startMainActivity();
                            }
                        }

                        DialogHelper.hideProgressDialog();
                    }
                });
    }

    private boolean checkUserLogin() {
        return firebaseAuth.getCurrentUser() != null;
    }

    private void startMainActivity() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            ApplicationClass.loggedUser = new User(currentUser.getUid(),
                    ImageHelper.encodeBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.user)),
                    currentUser.getEmail(), currentUser.getDisplayName());
        }
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        application.checkIn();
        application.startAdvertise();
        finish();
    }
}
