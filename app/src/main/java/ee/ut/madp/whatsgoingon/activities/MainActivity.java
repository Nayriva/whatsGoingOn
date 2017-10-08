package ee.ut.madp.whatsgoingon.activities;

import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import com.facebook.login.LoginManager;
import com.google.firebase.auth.FirebaseAuth;

import java.text.Normalizer;

import butterknife.BindView;
import butterknife.ButterKnife;
import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.chat.ChannelsActivity;
import ee.ut.madp.whatsgoingon.chat.ChatApplication;
import ee.ut.madp.whatsgoingon.chat.ChatDetailActivity;
import ee.ut.madp.whatsgoingon.fragments.ChatChannelsFragment;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    @BindView(R.id.nav_view)
    NavigationView navigationView;
    @BindView(R.id.drawer_layout)
    DrawerLayout drawerLayout;
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    private ChatApplication application;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        application = (ChatApplication) getApplication();
        firebaseAuth = FirebaseAuth.getInstance();
        setUpChannel();

        final NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                navigationView.bringToFront();
                drawer.requestLayout();
            }
        };
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        //setupDrawerContent(navigationView);

        navigationView.setCheckedItem(R.id.nav_chat);
    }

    private void setUpChannel() {
        String username = firebaseAuth.getCurrentUser().getDisplayName();
        username = Normalizer
                .normalize(username, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("\\s+", "_");
        application.hostSetChannelName(username);
        application.hostInitChannel();
        application.hostStartChannel();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

//    private void setupDrawerContent(NavigationView navigationView) {
//        navigationView.setNavigationItemSelectedListener(
//                new NavigationView.OnNavigationItemSelectedListener() {
//                    @Override
//                    public boolean onNavigationItemSelected(MenuItem menuItem) {
//                        selectDrawerItem(menuItem);
//                        return true;
//
//                    }
//                });
//    }
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        selectDrawerItem(item);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHAT_CHANNELS_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                String channelToJoin = data.getStringExtra("channelToJoin");
                Intent intent = new Intent(this, ChatDetailActivity.class);
                intent.putExtra("channelToJoin", channelToJoin);
                startActivityForResult(intent, CHAT_DETAIL_REQUEST_CODE);
            }
        }
    }

    public void selectDrawerItem(MenuItem menuItem) {
        Fragment fragment = null;
        Class fragmentClass = null;
        Class activityClass = null;
        int classRequestCode = -1;
        switch(menuItem.getItemId()){
            case R.id.nav_chat:
                fragmentClass = ChatChannelsFragment.class;
                activityClass = ChannelsActivity.class;
                classRequestCode = CHAT_CHANNELS_REQUEST_CODE;
                break;
            case R.id.nav_events:
                //fragmentClass = EventsFragment.class;
                break;
            case R.id.nav_profile:
                //fragmentClass = MyProfileFragment.class;
                break;
            case R.id.nav_settings:
                //fragmentClass = SettingsFragment.class;
                break;
            case R.id.nav_help:
                //fragmentClass = HelpFragment.class;
                break;
            case R.id.nav_logout:
                signOutUser(this);
                break;
            default:
                fragmentClass = ChatChannelsFragment.class;
                break;
        }

        try {
            fragment = (Fragment) fragmentClass.newInstance();
        } catch (Exception e) {
            Log.e(TAG, "Failed to obtain the fragment " + e.getMessage());

        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.containerView, fragment).commit();
        startActivityForResult(new Intent(this, activityClass), classRequestCode);

        menuItem.setChecked(true);
        setTitle(menuItem.getTitle());
        drawerLayout.closeDrawers();
    }

    private final int CHAT_CHANNELS_REQUEST_CODE = 1;
    private final int CHAT_DETAIL_REQUEST_CODE = 2;
    private final int EVENTS_REQUEST_CODE = 3;
    private final int EVENT_DETAIL_REQUES_CODE = 4;
    private final int EVENT_JOIN_REQUEST_CODE = 5;
    private final int EVENT_ADD_REQUEST_CODE = 6;
    private final int EVENT_EDIT_REQUEST_CODE = 7;
    private final int SETTINGS_REQUEST_CODE = 8;
    private final int HELP_REQUEST_CODE = 9;
    private final int LOG_OUT_REQUEST_CODE = 10;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        application.hostStopChannel();
        application.quit();
    }
    /**
     * Provides sign out of users
     *
     * @param context
     */
    public void signOutUser(Context context) {
        FirebaseAuth.getInstance().signOut();
        LoginManager.getInstance().logOut();
        context.startActivity(new Intent(context, LoginActivity.class));

    }

    /**
     * Setups the navigation header, if data is not available from shared preferences and external storage, it downloads it from firebase database and storage.
     */
    public void setupNavigationHeader() {

    }
}
