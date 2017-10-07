package ee.ut.madp.whatsgoingon.activities;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import ee.ut.madp.whatsgoingon.FirebaseApplication;
import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.constants.SettingsConstants;
import ee.ut.madp.whatsgoingon.helpers.FontHelper;

import static ee.ut.madp.whatsgoingon.constants.SettingsConstants.SPLASH_DISPLAY_LENGTH;

public class SplashActivity extends AppCompatActivity {

    public static final String TAG = SplashActivity.class.getSimpleName();

    @BindView(R.id.app_name)
    TextView appName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_splash);
        ButterKnife.bind(this);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                boolean isLogged = ((FirebaseApplication) getApplication()).checkUserLogin(SplashActivity.this);

                if (isLogged) {
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                } else {
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                }

            }
        }, SPLASH_DISPLAY_LENGTH);


        FontHelper.setFont(this, appName, SettingsConstants.CUSTOM_FONT);
    }
}
