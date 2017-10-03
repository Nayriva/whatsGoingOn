package ee.ut.madp.whatsgoingon;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameET;
    private EditText passwordET;
    private ChatApplication application;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        usernameET = (EditText) findViewById(R.id.usernameEditText);
        passwordET = (EditText) findViewById(R.id.passwordEditText);

        application = (ChatApplication) getApplication();
    }

    public void logIn(View view) {
        String username = String.valueOf(usernameET.getText());
        String password = String.valueOf(passwordET.getText());

        //TODO create logic for login

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
}
