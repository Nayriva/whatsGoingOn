package ee.ut.madp.whatsgoingon.helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import de.hdodenhof.circleimageview.CircleImageView;
import ee.ut.madp.whatsgoingon.ModelFactory;
import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.ApplicationClass;
import ee.ut.madp.whatsgoingon.models.User;

import static ee.ut.madp.whatsgoingon.constants.FirebaseConstants.FIREBASE_CHILD_USERS;

/**
 * Helper contains method for easier manipulation with user information.
 */
public class UserHelper {

    private static final String TAG = UserHelper.class.getSimpleName();

    private static DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference().child(FIREBASE_CHILD_USERS);

    /**
     * Get id of currently logged user
     * @return Id of logged user, null if no user is logged in.
     */
    public static String getCurrentUserId() {
        Log.i(TAG, "getCurrentUserId");
        return ApplicationClass.loggedUser.getId();
    }

    /**
     * Store new user information into firebase DB.
     * @param name name of user
     * @param firebaseUser firebase object of user
     * @param photo photo of user
     * @param logIn if true, user is stored in ApplicationClass as logged user
     */
    public static void saveNewUserToDB(String name, FirebaseUser firebaseUser, String photo, boolean logIn) {
        Log.i(TAG, "saveNewUserToDb: " + name + ", " + firebaseUser + ", logIn: " + logIn);
        User user = ModelFactory.createUser(firebaseUser.getUid(), photo, firebaseUser.getEmail(), name);
        if (logIn) {
            Log.i(TAG, "saveNewUserToDb: storing user as loggedIn user to ApplicationClass");
            ApplicationClass.loggedUser = user;
        }
        usersRef.child(firebaseUser.getUid()).setValue(user);
        usersRef.child(FIREBASE_CHILD_USERS).child(firebaseUser.getUid()).keepSynced(true);
    }

    /**
     * Save info about user logged in via Facebook.
     * @param context context
     * @param firebaseUser firebase user object
     */
    public static void saveFacebookInfoAboutUser(final Context context, final FirebaseUser firebaseUser) {
        Log.i(TAG, "saveFacebookInfoAboutUser: " + firebaseUser);
        final String photoUrl = getFacebookPhotoUrl(firebaseUser);
        Picasso.with(context)
                .load(photoUrl)
                .into(new Target() {
                          @Override
                          public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                              Log.i(TAG, "saveFacebookInfoAboutUser.onBitmapLoaded");
                              try {
                                  saveNewUserToDB(firebaseUser.getDisplayName(), firebaseUser,
                                          ImageHelper.encodeBitmap(bitmap), true);
                                  LayoutInflater inflater = (LayoutInflater) context
                                          .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                                  View view = inflater.inflate(R.layout.nav_header_main, null);
                                  CircleImageView profilePhoto = (CircleImageView) view
                                          .findViewById(R.id.user_photo);
                                  profilePhoto.setImageBitmap(bitmap);
                              } catch (Exception e) {
                                  Log.e(TAG, "saveFacebookInfoAboutUser.onBitmapLoaded - error: " + e.getMessage());
                              }
                          }

                          @Override
                          public void onBitmapFailed(Drawable errorDrawable) {
                              //left blank intentionally
                          }

                          @Override
                          public void onPrepareLoad(Drawable placeHolderDrawable) {
                              //left blank intentionally
                          }
                      }
                );
    }

    /**
     * Save info about user logged in via Google.
     * @param context context
     * @param firebaseUser firebase user object
     * @param photoUrl URL to user's photo
     */
    public static void saveGoogleInfoAboutUser(final Context context, final FirebaseUser firebaseUser, final String photoUrl) {
        Log.i(TAG, "saveGoogleInfoAboutUser: " + firebaseUser);
        Picasso.with(context)
                .load(photoUrl)
                .into(new Target() {
                          @Override
                          public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                              Log.i(TAG, "saveGoogleInfoAboutUser.onBitmapLoaded");
                              try {
                                  saveNewUserToDB(firebaseUser.getDisplayName(), firebaseUser,
                                          ImageHelper.encodeBitmap(bitmap), true);
                                  LayoutInflater inflater = (LayoutInflater) context
                                          .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                                  View view = inflater.inflate(R.layout.nav_header_main, null);
                                  CircleImageView profilePhoto = (CircleImageView) view
                                          .findViewById(R.id.user_photo);
                                  profilePhoto.setImageBitmap(bitmap);
                              } catch (Exception e) {
                                  Log.e(TAG, "saveGoogleInfoAboutUser.onBitmapLoaded - error: " + e.getMessage());
                              }
                          }

                          @Override
                          public void onBitmapFailed(Drawable errorDrawable) {
                              //left blank intentionally
                          }

                          @Override
                          public void onPrepareLoad(Drawable placeHolderDrawable) {
                              //left blank intentionally
                          }
                      }
                );
    }

    private static String getFacebookPhotoUrl(FirebaseUser firebaseUser) {
        String facebookUserId = "";
        for (UserInfo profile: firebaseUser.getProviderData()) {
            if (profile.getProviderId().equals("facebook.com")) {
                facebookUserId = profile.getUid();
            }
        }

        return "https://graph.facebook.com/" + facebookUserId + "/picture?type=large";
    }
}
