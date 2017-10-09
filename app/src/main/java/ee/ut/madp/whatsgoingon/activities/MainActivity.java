package ee.ut.madp.whatsgoingon.activities;

import android.content.Context;
import android.content.Intent;
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
import android.widget.TextView;

import com.facebook.login.LoginManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.Normalizer;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;
import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.chat.ChannelsActivity;
import ee.ut.madp.whatsgoingon.chat.ChatApplication;
import ee.ut.madp.whatsgoingon.chat.ChatDetailActivity;
import ee.ut.madp.whatsgoingon.fragments.ChatChannelsFragment;
import ee.ut.madp.whatsgoingon.fragments.EventsFragment;
import ee.ut.madp.whatsgoingon.fragments.HelpFragment;
import ee.ut.madp.whatsgoingon.fragments.MyProfileFragment;
import ee.ut.madp.whatsgoingon.helpers.DialogHelper;
import ee.ut.madp.whatsgoingon.helpers.ImageHelper;
import ee.ut.madp.whatsgoingon.models.User;

import static ee.ut.madp.whatsgoingon.constants.FirebaseConstants.FIREBASE_CHILD_USERS;

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
    private CircleImageView profilePhoto;
    DatabaseReference firebaseDatabase;

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

        setupNavigationHeader();

        navigationView.setCheckedItem(R.id.nav_chat);
    }

    private void setUpChannel() {
        // by default user doesnt have set displayName
        if (firebaseAuth.getCurrentUser().getEmail() != null) {
            String username = firebaseAuth.getCurrentUser().getEmail();
            username = Normalizer
                    .normalize(username, Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "")
                    .replaceAll("\\s+", "_");
            application.hostSetChannelName(username);
            application.hostInitChannel();
            application.hostStartChannel();
        }

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
                fragmentClass = EventsFragment.class;
                break;
            case R.id.nav_profile:
                fragmentClass = MyProfileFragment.class;
                break;
            case R.id.nav_settings:
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivityForResult(intent, SETTINGS_REQUEST_CODE);
                break;
            case R.id.nav_help:
                fragmentClass = HelpFragment.class;
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

        if (fragment != null && fragmentClass != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.containerView, fragment).commit();
            startActivityForResult(new Intent(this, activityClass), classRequestCode);
        }



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
    private void setupNavigationHeader() {
      // TODO get information from the shared prefences and storage
          getFirebaseDatabase().child(FIREBASE_CHILD_USERS).child(getUserId()).addValueEventListener(new ValueEventListener() {

                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Log.d(TAG, "Retrieving user with uid " + getUserId());
                    User user = dataSnapshot.getValue(User.class);
                    setupDataForDrawer(user.getName(), user.getEmail(), user.getPhoto());
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.w(TAG, "Failed to read value ", databaseError.toException());
                    DialogHelper.showAlertDialog(MainActivity.this, databaseError.toString());

                }

            });
        }

    /**
     * Return Firebase database reference
     * @return DatabaseReference
     */
    private DatabaseReference getFirebaseDatabase() {
        if (firebaseDatabase == null) {
            firebaseDatabase = FirebaseDatabase.getInstance().getReference();
        }
        return firebaseDatabase;
    }


    /**
     * Setups data for the navigation drawer. If data is not available from shared preferences and external storage, it downloads it from firebase database and storage.
     */
    public void setupDataForDrawer(String name, String email, String photo) {
        if (navigationView != null) {
            View header = navigationView.getHeaderView(0);
            profilePhoto = (CircleImageView) header.findViewById(R.id.user_photo);

            if (photo != null) {
                // TODO get photo from the external storage
                if (photo.contains("https")) {
                    //Picasso.with(this).load(photo).memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE).into(profilePhoto);
                } else if (!photo.isEmpty()) {
                    profilePhoto.setImageBitmap(ImageHelper.decodeBitmap(photo));
                }
            }


            TextView nameView = (TextView) header.findViewById(R.id.header_name);
            nameView.setText(name);
            TextView emailView = (TextView) header.findViewById(R.id.header_email);
            emailView.setText(email);
        }

    }


    private String getUserId() {
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }
}
