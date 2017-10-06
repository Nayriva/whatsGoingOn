package ee.ut.madp.whatsgoingon;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.text.Normalizer;


import ee.ut.madp.whatsgoingon.chat.ChatApplication;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameLoginET;
    private EditText passwordLoginET;
    private EditText usernameRegisterET;
    private EditText passwordRegisterET;
    private EditText repPasswordRegisterET;
    private ChatApplication application;
    private GoogleApiClient mGoogleApiClient;
    private FirebaseAuth mAuth;
    private boolean regScreenActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        initializeAuth();
        initializeEditTexts();
    }

    private void initializeEditTexts() {
        if (regScreenActive) {
            setContentView(R.layout.registration);
            usernameRegisterET = (EditText) findViewById(R.id.usernameRegEditText);
            passwordRegisterET = (EditText) findViewById(R.id.passwordRegEditText);
            repPasswordRegisterET = (EditText) findViewById(R.id.repPasswordEditText);
        } else {
            usernameLoginET = (EditText) findViewById(R.id.usernameEditText);
            passwordLoginET = (EditText) findViewById(R.id.passwordEditText);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        application = (ChatApplication) getApplication();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            loggedInContinue(currentUser.getDisplayName());
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("regScreenActive", regScreenActive);
        if (regScreenActive) {
            outState.putString("usernameRegisterETText", String.valueOf(usernameRegisterET.getText()));
            outState.putString("passwordRegisterETText", String.valueOf(passwordRegisterET.getText()));
            outState.putString("repPasswordRegisterETText", String.valueOf(repPasswordRegisterET.getText()));
        } else {
            outState.putString("usernameLoginETText", String.valueOf(usernameLoginET.getText()));
            outState.putString("passwordLoginETText", String.valueOf(passwordLoginET.getText()));
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        regScreenActive = savedInstanceState.getBoolean("regScreenActive");
        if (regScreenActive) {
            setContentView(R.layout.registration);
            initializeEditTexts();
            usernameRegisterET.setText(savedInstanceState.getString("usernameRegisterETText", ""));
            passwordRegisterET.setText(savedInstanceState.getString("passwordRegisterETText", ""));
            repPasswordRegisterET.setText(savedInstanceState.getString("repPasswordRegisterETText", ""));
        } else {
            setContentView(R.layout.login);
            initializeEditTexts();
            usernameLoginET.setText(savedInstanceState.getString("usernameLoginETText", ""));
            passwordLoginET.setText(savedInstanceState.getString("passwordLoginETText", ""));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                String username = user.getDisplayName();
                                username = Normalizer
                                        .normalize(username, Normalizer.Form.NFD)
                                        .replaceAll("\\p{M}", "")
                                        .replaceAll("\\s+", "_");
                                loggedInContinue(username);
                            }
                        }
                    }
                });
    }

    private void initializeAuth() {
        mAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getResources().getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, null)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
    }

    public void logIn(View view) {
        String username = String.valueOf(usernameLoginET.getText());
        String password = String.valueOf(passwordLoginET.getText());

        short checkResult = checkIfRegistered(username, password);
        switch (checkResult) {
            case ACCOUNT_NOT_FOUND : {
                Toast.makeText(this, "Username not found!", Toast.LENGTH_SHORT).show();
                return;
            } case WRONG_PASSWORD : {
                Toast.makeText(this, "Wrong password!", Toast.LENGTH_SHORT).show();
                return;
            } case LOGIN_CONFIRMED : // ALL WENT OK
                break;
        }
        loggedInContinue(username);
    }

    private void loggedInContinue(String username) {
        application = (ChatApplication) getApplication();
        application.hostSetChannelName(username);
        application.hostInitChannel();
        application.hostStartChannel();

        Intent finishedIntent = new Intent();
        finishedIntent.putExtra("loggedIn", true);
        finishedIntent.putExtra("username", username);
        setResult(RESULT_OK, finishedIntent);
        finish();
    }

    private short checkIfRegistered(String username, String password) {
        Context context = getApplicationContext();
        SharedPreferences accountsFile = context.getSharedPreferences(ACCOUNTS_FILE, Context.MODE_PRIVATE);
        int fileHash = accountsFile.getInt(username, -1);
        if (fileHash == -1) {
            return ACCOUNT_NOT_FOUND;
        }
        int passHash = password.hashCode();
        if (passHash != fileHash) {
            return WRONG_PASSWORD;
        }

        return LOGIN_CONFIRMED;
    }

    public void register(View view) {
        String username = String.valueOf(usernameRegisterET.getText());
        String password = String.valueOf(passwordRegisterET.getText());
        String repPassword = String.valueOf(repPasswordRegisterET.getText());
        if (username == null || username.isEmpty() || password == null || password.isEmpty()
                || repPassword == null || repPassword.isEmpty()) {
            Toast.makeText(this, "Fill all fields!", Toast.LENGTH_SHORT).show();
            return;
        }
        short checkResult = tryRegister(username, password, repPassword);
        switch (checkResult) {
            case USERNAME_ALREADY_USED : {
                Toast.makeText(this, "Username is already used!", Toast.LENGTH_SHORT).show();
                return;
            } case PASSWORDS_ARE_DIFFERENT : {
                Toast.makeText(this, "Passwords don't match!", Toast.LENGTH_SHORT).show();
                return;
            } case REGISTRATION_SUCCESSFUL : // ALL WENT OK
                break;
        }
        switchToLogin(view);
    }

    private short tryRegister(String username, String password, String repPassword) {
        Context context = getApplicationContext();
        SharedPreferences accountsFile = context.getSharedPreferences(ACCOUNTS_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = accountsFile.edit();
        String fileUsername = accountsFile.getString(username, null);
        if (fileUsername != null) {
            return USERNAME_ALREADY_USED;
        } else if (! repPassword.equals(password)) {
            return PASSWORDS_ARE_DIFFERENT;
        }

        editor.putInt(username, password.hashCode());
        editor.apply();
        return REGISTRATION_SUCCESSFUL;
    }

    public void switchToRegister(View view) {
        setContentView(R.layout.registration);
        regScreenActive = true;
        initializeEditTexts();
    }

    public void switchToLogin(View view) {
        setContentView(R.layout.login);
        regScreenActive = false;
        initializeEditTexts();
    }

    public void googleSignIn(View view) {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private final short ACCOUNT_NOT_FOUND = 1;
    private final short WRONG_PASSWORD = 2;
    private final short LOGIN_CONFIRMED = 3;
    private final short USERNAME_ALREADY_USED = 4;
    private final short PASSWORDS_ARE_DIFFERENT = 5;
    private final short REGISTRATION_SUCCESSFUL = 6;

    private final int RC_SIGN_IN = 1;
    private static final String ACCOUNTS_FILE = "ACCOUNTS";
}
