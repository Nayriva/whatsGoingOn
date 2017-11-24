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
import android.view.Menu;
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

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;
import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.ApplicationClass;
import ee.ut.madp.whatsgoingon.fragments.ChatChannelsFragment;
import ee.ut.madp.whatsgoingon.fragments.EventFragment;
import ee.ut.madp.whatsgoingon.helpers.ImageHelper;
import ee.ut.madp.whatsgoingon.models.User;

import static ee.ut.madp.whatsgoingon.constants.FirebaseConstants.FIREBASE_CHILD_USERS;
import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.EVENTS_REQUEST_CODE;
import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.MY_PROFILE_REQUEST_CODE;
import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.SETTINGS_REQUEST_CODE;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String LAST_ACTIVE_FRAGMENT = "LAST_ACTIVE_FRAGMENT";

    @BindView(R.id.nav_view) NavigationView navigationView;
    @BindView(R.id.drawer_layout) DrawerLayout drawerLayout;
    @BindView(R.id.toolbar) Toolbar toolbar;

    private String activeFragment = "Chat";
    private ApplicationClass application;
    private DatabaseReference userRef;
    private ValueEventListener valueEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        application = (ApplicationClass) getApplication();
        userRef = FirebaseDatabase.getInstance().getReference().child(FIREBASE_CHILD_USERS)
                .child(ApplicationClass.loggedUser.getId());

        setUpDbListener();
        setSupportActionBar(toolbar);
        setUpNavigationView();
        setUpDrawer();
        setupDataForDrawer();
        if (savedInstanceState == null) {
            setUpFragment("Chat");
        } else {
            activeFragment = savedInstanceState.getString(LAST_ACTIVE_FRAGMENT);
            setTitle(activeFragment);
        }
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
        userRef.removeEventListener(valueEventListener);
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
        userRef.addValueEventListener(valueEventListener);

        if (activeFragment != null) {
            if (activeFragment.equals("Chat")) {
                navigationView.setCheckedItem(R.id.nav_chat);
            } else if (activeFragment.equals("Events")) {
                navigationView.setCheckedItem(R.id.nav_events);
            }
            setTitle(activeFragment);
        }
    }

    @Override
    public void onBackPressed() {
        Log.i(TAG, "onBackPressed");
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(LAST_ACTIVE_FRAGMENT, activeFragment);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Log.i(TAG, "onNavigationItemSelected");
        selectDrawerItem(item);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult: " + requestCode + ", " + resultCode + ", " + data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void setUpFragment(String type) {
        Log.i(TAG, "setUpFragment: " + type);
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = null;
        try {
            if (type.equalsIgnoreCase("Chat")) {
                fragment = ChatChannelsFragment.class.newInstance();
                navigationView.setCheckedItem(R.id.nav_chat);
            } else if (type.equalsIgnoreCase("Events")) {
                fragment = EventFragment.class.newInstance();
                navigationView.setCheckedItem(R.id.nav_events);
            }

        } catch (InstantiationException | IllegalAccessException e) {
            //this should never happen
        }
        fragmentManager.beginTransaction().replace(
                R.id.containerView, fragment).commit();
        setTitle(type);
    }

    private void setUpNavigationView() {
        Log.i(TAG, "setUpNavigationView");
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.nav_chat);
    }

    private void setUpDrawer() {
        Log.i(TAG, "setUpDrawer");
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
        Log.i(TAG, "selectDrawerItem: " + menuItem);
        Fragment fragment = null;
        Class fragmentClass = null;

        switch (menuItem.getItemId()) {
            case R.id.nav_chat:
                fragmentClass = ChatChannelsFragment.class;
                activeFragment = "Chat";
                break;
            case R.id.nav_events:
                fragmentClass = EventFragment.class;
                activeFragment = "Events";
                break;
            case R.id.nav_profile:
                startActivity(new Intent(MainActivity.this, MyProfileActivity.class));
                break;
            case R.id.nav_settings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                break;
            case R.id.nav_help:
                //TODO implement help screen
                break;
            case R.id.nav_logout:
                signOutUser(this);
                break;
            default:
                fragmentClass = ChatChannelsFragment.class;
                break;
        }

        try {
            if (fragmentClass != null) {
                fragment = (Fragment) fragmentClass.newInstance();
            }
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
        Log.i(TAG, "signOutUser");
        application.stopAdvertise();
        FirebaseAuth.getInstance().signOut();
        LoginManager.getInstance().logOut();
        startActivity(new Intent(context, LoginActivity.class));
        finish();
    }

    public void setupDataForDrawer() {
        Log.i(TAG, "setupDataForDrawer");
        if (navigationView != null) {
            User user = ApplicationClass.loggedUser;
            View header = navigationView.getHeaderView(0);
            CircleImageView profilePhoto = (CircleImageView) header.findViewById(R.id.user_photo);
            profilePhoto.setImageBitmap(ImageHelper.decodeBitmap(user.getPhoto()));

            TextView nameView = (TextView) header.findViewById(R.id.header_name);
            nameView.setText(user.getName());
            TextView emailView = (TextView) header.findViewById(R.id.header_email);
            emailView.setText(user.getEmail());
        }
    }

    private void setUpDbListener() {
        Log.i(TAG, "setUpDbListener");
        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.i(TAG, "setUpDbListener.onDataChange");
                User user = dataSnapshot.getValue(User.class);
                if (user != null) {
                    ApplicationClass.loggedUser.setPhoto(user.getPhoto());
                    setupDataForDrawer();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        userRef.addListenerForSingleValueEvent(valueEventListener);
    }
}
