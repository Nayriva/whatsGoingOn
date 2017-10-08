package ee.ut.madp.whatsgoingon.activities;

import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;
import ee.ut.madp.whatsgoingon.R;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    @BindView(R.id.nav_view)
    NavigationView navigationView;
    @BindView(R.id.drawer_layout)
    DrawerLayout drawerLayout;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    CircleImageView profilePhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
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

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_chat) {

        } else if (id == R.id.nav_profile) {

        } else if (id == R.id.nav_events) {

        } else if (id == R.id.nav_settings) {

        } else if (id == R.id.nav_logout) {

        } else if (id == R.id.nav_help) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @OnClick(R.id.fab)
    void add() {

    }


    private void chooseFragment(int itemId) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        Fragment fragment = null;
        Class fragmentClass = null;
//        switch (itemId) {
//            case R.id.nav_profile:
//                fragmentClass = HomeFragment.class;
//                break;
//            case R.id.nav_inbox:
//                Intent intent = new Intent(this, ConversationActivity.class);
//                startActivityForResult(intent, CHAT_REQUEST_CODE);
//                break;
//            case R.id.nav_map:
//                fragmentClass = MapsFragment.class;
//                break;
//            case R.id.nav_dog_breed:
//                fragmentClass = DogBreedsFragment.class;
//                break;
//            case R.id.nav_reminder:
//                fragmentClass = ReminderFragment.class;
//                break;
//            case R.id.nav_favourite_dog_breeds:
//                fragmentClass = FavouriteDogBreedsFragment.class;
//                break;
//            case R.id.nav_settings:
//                Intent intent2 = new Intent(MainActivity.this, SettingsActivity.class);
//                startActivityForResult(intent2, SETTINGS_REQUEST_CODE);
//                break;
//            case R.id.nav_logout:
//                ((FirebaseApplication) getApplication()).signOutUser(this);
//                break;
//            default:
//                fragmentClass = HomeFragment.class;
//
//        }
//
//        try {
//            fragment = (Fragment) fragmentClass.newInstance();
//        } catch (Exception e) {
//            Log.e(TAG, "Failed to obtain the fragment " + e.getMessage());
//
//        }

        if (fragment != null && fragmentClass != null) {
            fragmentManager.beginTransaction().replace(R.id.content_main, fragment).addToBackStack(null).commit();
        }

    }


    /**
     * It sets data for navigation drawer such as name, email and dogPhoto that can be taken from external storage or download from firebase storage
     *
     * @param name
     * @param email
     * @param photo
     */
    private void setupDataForDrawer(String name, String email, String photo) {
        if (navigationView != null) {
            View header = navigationView.getHeaderView(0);
            profilePhoto = (CircleImageView) header.findViewById(R.id.user_photo);

//            if (photo != null) {
//                String imagePath = userId + STORAGE_IMAGES_PATH + STORAGE_MY_PROFILE_PHOTO;
//                if (ImageUtils.checkExternalStorage() && ImageUtils.checkIfImageExits(MainActivity.this, imagePath)) {
//                    File filePath = getExternalCacheDir();
//                    File myDir = new File(filePath.getAbsolutePath() + "/" + imagePath);
//                    Picasso.with(this).load(myDir).memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE).into(profilePhoto);
//                } else {
//                    if (photo.contains("https")) {
//                        Picasso.with(this).load(photo).memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE).into(profilePhoto);
//                    } else if (!photo.isEmpty()) {
//                        profilePhoto.setImageBitmap(ImageUtils.decodeBitmap(photo));
//                    }
//                }
//            }

            TextView nameView = (TextView) header.findViewById(R.id.header_name);
            nameView.setText(name);
            TextView emailView = (TextView) header.findViewById(R.id.header_email);
            emailView.setText(email);
        }
    }

}
