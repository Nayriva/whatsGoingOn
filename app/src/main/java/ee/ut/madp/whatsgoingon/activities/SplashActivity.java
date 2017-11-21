package ee.ut.madp.whatsgoingon.activities;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import butterknife.BindView;
import butterknife.ButterKnife;
import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.ApplicationClass;
import ee.ut.madp.whatsgoingon.constants.GeneralConstants;
import ee.ut.madp.whatsgoingon.helpers.FontHelper;
import ee.ut.madp.whatsgoingon.helpers.ImageHelper;
import ee.ut.madp.whatsgoingon.models.User;

import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.SPLASH_DISPLAY_LENGTH;

/**
 * Splash screen activity displayed when application is starting.
 */
public class SplashActivity extends AppCompatActivity {

    public static final String TAG = SplashActivity.class.getSimpleName();
    private FirebaseAuth firebaseAuth;

    @BindView(R.id.app_name) TextView appName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate()");
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
                Log.i(TAG, "Handler.postDelayed()");
                if (firebaseAuth.getCurrentUser() != null) {
                    Log.i(TAG, "Found logged user: " + firebaseAuth.getCurrentUser().getUid() + "," +
                            "starting MainActivity");
                    FirebaseUser currentUser = firebaseAuth.getCurrentUser();
                    ApplicationClass application = (ApplicationClass) getApplication();
                    ApplicationClass.loggedUser = new User(currentUser.getUid(),
                            ImageHelper.encodeBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.user)),
                            currentUser.getEmail(), currentUser.getDisplayName());

                    application.checkIn();
                    application.startAdvertise();
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                } else {
                    Log.i(TAG, "No user logged in, starting LoginActivity");
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                }
                finish();
            }
        }, SPLASH_DISPLAY_LENGTH);

        FontHelper.setFont(this, appName, GeneralConstants.CUSTOM_FONT);
    }
}
