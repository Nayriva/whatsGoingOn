package ee.ut.madp.whatsgoingon.activities;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import butterknife.BindView;
import butterknife.ButterKnife;
import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.ChatApplication;
import ee.ut.madp.whatsgoingon.constants.FirebaseConstants;
import ee.ut.madp.whatsgoingon.constants.GeneralConstants;
import ee.ut.madp.whatsgoingon.helpers.FontHelper;
import ee.ut.madp.whatsgoingon.helpers.ImageHelper;
import ee.ut.madp.whatsgoingon.models.User;

import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.SPLASH_DISPLAY_LENGTH;

public class SplashActivity extends AppCompatActivity {

    public static final String TAG = SplashActivity.class.getSimpleName();
    private FirebaseAuth firebaseAuth;

    @BindView(R.id.app_name) TextView appName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_splash);
        ButterKnife.bind(this);
        firebaseAuth = FirebaseAuth.getInstance();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (firebaseAuth.getCurrentUser() != null) {
                    FirebaseUser currentUser = firebaseAuth.getCurrentUser();
                    ChatApplication application = (ChatApplication) getApplication();
                    ChatApplication.loggedUser = new User(currentUser.getUid(),
                            ImageHelper.encodeBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.user)),
                            currentUser.getEmail(), currentUser.getDisplayName());

                    application.checkIn();
                    application.startAdvertise();
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                } else {
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                }
                finish();
            }
        }, SPLASH_DISPLAY_LENGTH);

        FontHelper.setFont(this, appName, GeneralConstants.CUSTOM_FONT);
    }
}
