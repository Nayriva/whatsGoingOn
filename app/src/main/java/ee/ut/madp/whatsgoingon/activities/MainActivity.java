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
import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.SETTINGS_REQUEST_CODE;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    @BindView(R.id.nav_view) NavigationView navigationView;
    @BindView(R.id.drawer_layout) DrawerLayout drawerLayout;
    @BindView(R.id.toolbar) Toolbar toolbar;

    private ApplicationClass application;
    private DatabaseReference userRef;
    private ValueEventListener valueEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        setUpFragment("Chat");
    }

    private void setUpDbListener() {
        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
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

    @Override
    protected void onPause() {
        super.onPause();
        userRef.removeEventListener(valueEventListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        userRef.addValueEventListener(valueEventListener);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == SETTINGS_REQUEST_CODE) {
                setUpFragment("Chat");
            } else if (requestCode == EVENTS_REQUEST_CODE) {
                setUpFragment("Events");
            }
        }
    }

    private void setUpFragment(String type) {
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
        setTitle(type);
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

    public void setupDataForDrawer() {
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
}
