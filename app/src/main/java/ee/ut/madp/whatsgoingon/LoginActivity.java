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

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        usernameLoginET = (EditText) findViewById(R.id.usernameEditText);
        passwordLoginET = (EditText) findViewById(R.id.passwordEditText);

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

    @Override
    public void onStart() {
        super.onStart();
        application = (ChatApplication) getApplication();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            FirebaseAuth.getInstance().signOut();
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
            } case ENCRYPTION_EXCEPTION : {
                Toast.makeText(this, "Error occurred, try again!", Toast.LENGTH_SHORT).show();
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
        String filePassword = accountsFile.getString(username, null);
        if (filePassword == null) {
            return ACCOUNT_NOT_FOUND;
        }
        try {
            password = encrypt(password, ENCRYPT_KEY);
            if (! password.equals(filePassword)) {
                return WRONG_PASSWORD;
            }
        } catch (Exception e) {
            return ENCRYPTION_EXCEPTION;
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
            } case ENCRYPTION_EXCEPTION : {
                Toast.makeText(this, "Error occurred, try again!", Toast.LENGTH_SHORT).show();
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
        try {
            password = encrypt(password, ENCRYPT_KEY);
        } catch (Exception e) {
            return ENCRYPTION_EXCEPTION;
        }
        editor.putString(username, password);
        editor.apply();
        return REGISTRATION_SUCCESSFUL;
    }

    public void switchToRegister(View view) {
        setContentView(R.layout.registration);
        usernameRegisterET = (EditText) findViewById(R.id.usernameEditText);
        passwordRegisterET = (EditText) findViewById(R.id.passwordEditText);
        repPasswordRegisterET = (EditText) findViewById(R.id.repPasswordEditText);
    }

    public void switchToLogin(View view) {
        setContentView(R.layout.login);
    }

    public void googleSignIn(View view) {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    public static String encrypt(String strClearText,String strKey) throws Exception {
        String strData="";

        try {
            SecretKeySpec skeyspec = new SecretKeySpec(strKey.getBytes(),"Blowfish");
            Cipher cipher = Cipher.getInstance("Blowfish");
            cipher.init(Cipher.ENCRYPT_MODE, skeyspec);
            byte[] encrypted = cipher.doFinal(strClearText.getBytes());
            strData=new String(encrypted);

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e);
        }
        return strData;
    }

    private final short ACCOUNT_NOT_FOUND = 1;
    private final short WRONG_PASSWORD = 2;
    private final short ENCRYPTION_EXCEPTION = 3;
    private final short LOGIN_CONFIRMED = 4;
    private final short USERNAME_ALREADY_USED = 5;
    private final short PASSWORDS_ARE_DIFFERENT = 6;
    private final short REGISTRATION_SUCCESSFUL = 7;

    private final String ENCRYPT_KEY = "ee.ut.madp.chat.auenkl";
    private final int RC_SIGN_IN = 1;
    private static final String ACCOUNTS_FILE = "ACCOUNTS";
}
