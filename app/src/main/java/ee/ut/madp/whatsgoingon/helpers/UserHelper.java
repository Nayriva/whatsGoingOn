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
import ee.ut.madp.whatsgoingon.models.User;

import static ee.ut.madp.whatsgoingon.constants.FirebaseConstants.FIREBASE_CHILD_USERS;

public class UserHelper {

    public static final String TAG = UserHelper.class.getSimpleName();

    private static DatabaseReference firebaseDatabase;


    private static DatabaseReference getFirebaseDatabase() {
        if (firebaseDatabase == null) {
            firebaseDatabase = FirebaseDatabase.getInstance().getReference();
        }
        return firebaseDatabase;
    }


    /**
     * Saves a new user to the firebase database
     * @param name
     * @param firebaseUser
     * @param photo
     */
    public static void saveNewUser(String name, FirebaseUser firebaseUser, String photo) {
        User user = ModelFactory.createUser(firebaseUser.getUid(), photo, firebaseUser.getEmail(), name);
        getFirebaseDatabase().child(FIREBASE_CHILD_USERS).child(firebaseUser.getUid()).setValue(user);
    }

    public static String getFacebookPhotoUrl(FirebaseUser firebaseUser) {
        String facebookUserId = "";
        for (UserInfo profile : firebaseUser.getProviderData()) {
            if (profile.getProviderId().equals("facebook.com")) {
                facebookUserId = profile.getUid();
            }
        }

        final String photoUrl = "https://graph.facebook.com/" + facebookUserId + "/picture?type=large";

        return photoUrl;
    }

    /**
     * Gets facebook profile photo and saves all info about the user to the Firebase database
     *
     * @param firebaseUser
     */
    public static void saveFacebookInfoAboutUser(final Context context, final FirebaseUser firebaseUser) {
        final String photoUrl = getFacebookPhotoUrl(firebaseUser);
        Picasso.with(context)
                .load(photoUrl)
                .into(new Target() {
                          @Override
                          public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                              try {
                                  saveNewUser(firebaseUser.getDisplayName(), firebaseUser, photoUrl);
                                  LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                                  View view = inflater.inflate(R.layout.nav_header_main, null);
                                  CircleImageView profilePhoto = (CircleImageView) view.findViewById(R.id.user_photo);
                                  profilePhoto.setImageBitmap(bitmap);
                              } catch (Exception e) {
                                  Log.e(TAG, "onBitmapLoaded " + e.getMessage());
                              }
                          }

                          @Override
                          public void onBitmapFailed(Drawable errorDrawable) {
                          }

                          @Override
                          public void onPrepareLoad(Drawable placeHolderDrawable) {
                          }
                      }
                );
    }
}
