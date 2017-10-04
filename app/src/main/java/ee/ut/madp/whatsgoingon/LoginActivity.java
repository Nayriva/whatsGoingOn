package ee.ut.madp.whatsgoingon;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.prefs.Preferences;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameLoginET;
    private EditText passwordLoginET;
    private EditText usernameRegisterET;
    private EditText passwordRegisterET;
    private EditText repPasswordRegisterET;
    private ChatApplication application;
    private static final String ACCOUNTS_FILE = "ACCOUNTS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        usernameLoginET = (EditText) findViewById(R.id.usernameEditText);
        passwordLoginET = (EditText) findViewById(R.id.passwordEditText);

        application = (ChatApplication) getApplication();
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

    private final String ENCRYPT_KEY = "ee.ut.madp.chat.aíéuenkl";
}
