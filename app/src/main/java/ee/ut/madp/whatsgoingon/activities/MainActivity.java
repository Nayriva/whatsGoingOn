package ee.ut.madp.whatsgoingon.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import com.squareup.picasso.Picasso;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;
import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.chat.ChatApplication;
import ee.ut.madp.whatsgoingon.fragments.ChatChannelsFragment;
import ee.ut.madp.whatsgoingon.fragments.EventFragment;
import ee.ut.madp.whatsgoingon.helpers.DialogHelper;
import ee.ut.madp.whatsgoingon.helpers.ImageHelper;
import ee.ut.madp.whatsgoingon.helpers.UserHelper;
import ee.ut.madp.whatsgoingon.models.User;

import static ee.ut.madp.whatsgoingon.constants.FirebaseConstants.FIREBASE_CHILD_USERS;
import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.SETTINGS_REQUEST_CODE;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    @BindView(R.id.nav_view) NavigationView navigationView;
    @BindView(R.id.drawer_layout) DrawerLayout drawerLayout;
    @BindView(R.id.toolbar) Toolbar toolbar;

    private ChatApplication application;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference firebaseDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        setUpVariables();
        setUpNavigationView();
        setUpDrawer();
        setupNavigationHeader();
        setUpInitialFragment("Chat");
        application.startAdvertise();

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
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        selectDrawerItem(item);
        return true;
    }

    private void setUpVariables() {
        application = (ChatApplication) getApplication();
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance().getReference();
    }

    private void setUpInitialFragment(String type) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = null;
        try {
            if (type.equalsIgnoreCase("Chat")) {
                fragment = ChatChannelsFragment.class.newInstance();
            } else if (type.equalsIgnoreCase("Events")) {
                fragment = EventFragment.class.newInstance();
            }

        } catch (InstantiationException | IllegalAccessException e) {
            //this should never happen
        }
        fragmentManager.beginTransaction().replace(
                R.id.containerView, fragment).commit();
        setTitle("Chat");
    }

    private void setUpNavigationView() {
        final NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.nav_chat);
    }

    private void setUpDrawer() {
        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                navigationView.bringToFront();
                drawer.requestLayout();
            }
        };
        drawer.addDrawerListener(toggle);
        toggle.syncState();
    }

    public void selectDrawerItem(MenuItem menuItem) {
        Fragment fragment = null;
        Class fragmentClass = null;

        switch (menuItem.getItemId()) {
            case R.id.nav_chat:
                fragmentClass = ChatChannelsFragment.class;
                break;
            case R.id.nav_events:
                fragmentClass = EventFragment.class;
                break;
            case R.id.nav_profile:
                startActivity(new Intent(MainActivity.this, MyProfileActivity.class));
                break;
            case R.id.nav_settings:
                startActivityForResult(new Intent(MainActivity.this, SettingsActivity.class), SETTINGS_REQUEST_CODE);
                break;
            case R.id.nav_help:
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

        if (fragment != null ) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.containerView, fragment).commit();
        }

        menuItem.setChecked(true);
        setTitle(menuItem.getTitle());
        drawerLayout.closeDrawers();
    }

    public void signOutUser(Context context) {
        application.stopAdvertise();
        FirebaseAuth.getInstance().signOut();
        LoginManager.getInstance().logOut();
        startActivity(new Intent(context, LoginActivity.class));
    }

    private void setupNavigationHeader() {
        // TODO get information from the shared preferences and storage
        firebaseDatabase.child(FIREBASE_CHILD_USERS).child(getUserId()).addListenerForSingleValueEvent(
                new ValueEventListener() {

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

    public void setupDataForDrawer(String name, String email, String photo) {
        if (navigationView != null) {
            View header = navigationView.getHeaderView(0);
            CircleImageView profilePhoto = (CircleImageView) header.findViewById(R.id.user_photo);

            if (photo != null) {
                // TODO get photo from the external storage
                if (photo.contains("https")) {
                    if (photo.contains("facebook")) {
                        Picasso.with(this).load(UserHelper.getFacebookPhotoUrl(firebaseAuth.getCurrentUser())).into(profilePhoto);
                    } else {
                        Picasso.with(this).load(UserHelper.getGooglePhotoUrl(firebaseAuth.getCurrentUser().getPhotoUrl())).into(profilePhoto);
                    }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == SETTINGS_REQUEST_CODE) {
                setUpInitialFragment("Chat");
            }
        }
    }
}
