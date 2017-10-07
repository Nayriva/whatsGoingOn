package ee.ut.madp.whatsgoingon;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import ee.ut.madp.whatsgoingon.activities.LoginActivity;
import ee.ut.madp.whatsgoingon.activities.MainActivity;
import ee.ut.madp.whatsgoingon.helpers.DialogHelper;
import ee.ut.madp.whatsgoingon.models.User;

import static ee.ut.madp.whatsgoingon.activities.SplashActivity.TAG;
import static ee.ut.madp.whatsgoingon.constants.FirebaseConstants.FIREBASE_CHILD_USERS;


public class FirebaseApplication extends Application {

    private FirebaseAuth firebaseAuth;
    private DatabaseReference firebaseDatabase;

    /**
     * Returns FirebaseAuth
     *
     * @return FirebaseAuth
     */
    public FirebaseAuth getFirebaseAuth() {
        if (firebaseAuth == null) {
            firebaseAuth = FirebaseAuth.getInstance();
        }
        return firebaseAuth;
    }

    /**
     * Checks if the current user is logged or not.
     *
     * @return boolean, true for a logged user
     */
    public boolean checkUserLogin(Context context) {

        final FirebaseUser firebaseUser = getFirebaseAuth().getCurrentUser();

        return firebaseUser != null;
    }

    /**
     * Provides sign out of users
     * @param context
     */
    public void signOutUser(Context context) {
        getFirebaseAuth().signOut();
        context.startActivity(new Intent(context, LoginActivity.class));
    }

    /**
     * Logs an user with given email and password. Before that inputs of user are validated
     * @param context
     * @param email
     * @param password
     */
    public void loginUser(final Context context, String email, String password) {
        DialogHelper.showProgressDialog(context, getString(R.string.progress_dialog_title_login));
        getFirebaseAuth().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener((Activity) context, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (!task.isSuccessful()) {
                            Log.e(TAG, "signInWithEmail", task.getException());
                            FirebaseExceptionsChecker.checkFirebaseAuth(context, task);
                        } else {
                            if (checkUserLogin(context)) {
                                String userId = task.getResult().getUser().getUid();
                                Log.d(TAG, "loginUser: user with id " + userId + "was logged" );
                                context.startActivity(new Intent(context, MainActivity.class));
                            }
                        }
                        DialogHelper.hideProgressDialog();
                    }
                });
    }

    /**
     * Creates a new user and saves to database, stores user's profile photo
     *
     * @param context
     * @param email
     * @param password
     * @param name
     */
    public void createNewUser(final Context context, String email, String password, final String name, final String photo) {
        DialogHelper.showProgressDialog(context, getString(R.string.progress_dialog_title_signup));
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener((Activity) context, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "createUserWithEmail: onComplete:" + task.isSuccessful());
                        if (!task.isSuccessful()) {
                            FirebaseExceptionsChecker.checkFirebaseAuth(context, task);
                        } else {
                            onAuthSuccess(context, name, task.getResult().getUser(), photo);
                        }

                        DialogHelper.hideProgressDialog();
                    }
                });

    }

    /**
     * Provides the basic saving user to the Firebase database, shared preferences and saving photos to the external storage
     * @param context
     * @param name
     * @param firebaseUser
     * @param photo
     */
    private void onAuthSuccess(Context context, String name, FirebaseUser firebaseUser, String photo) {
        saveNewUser(name, firebaseUser, photo);
        if (firebaseUser != null) {
            //Stores information to shared preferences
            // TODO store user information to shared prefences
        }
        context.startActivity(new Intent(context, LoginActivity.class));
    }

    /**
     * Saves a new user to database
     *
     * @param name
     * @param firebaseUser
     * @param photo
     */
    public void saveNewUser(String name, FirebaseUser firebaseUser, String photo) {
        User user = ModelFactory.createUser(firebaseUser.getUid(), photo, firebaseUser.getEmail(), name);
        getFirebaseDatabase().child(FIREBASE_CHILD_USERS).child(firebaseUser.getUid()).setValue(user);
    }

    /**
     * Return Firebase database reference
     * @return DatabaseReference
     */
    public DatabaseReference getFirebaseDatabase() {
        if (firebaseDatabase == null) {
            firebaseDatabase = FirebaseDatabase.getInstance().getReference();
        }
        return firebaseDatabase;
    }


}
