package ee.ut.madp.whatsgoingon;


import android.content.Context;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

import ee.ut.madp.whatsgoingon.helpers.DialogHelper;

public class FirebaseExceptionsChecker {

    public static void checkFirebaseAuth(Context context, Task<AuthResult> task) {
        try {
            throw task.getException();
        } catch (FirebaseAuthWeakPasswordException e) {
            DialogHelper.showAlertDialog(context, context.getString(R.string.error_incorrect_password));
        } catch (FirebaseAuthInvalidCredentialsException e) {
            DialogHelper.showAlertDialog(context, context.getString(R.string.error_invalid_credential));
        } catch (FirebaseAuthUserCollisionException e) {
            DialogHelper.showAlertDialog(context, context.getString(R.string.error_user_exits));
        } catch (FirebaseAuthInvalidUserException e) {
            DialogHelper.showAlertDialog(context, context.getString(R.string.error_no_user));
        } catch (Exception e) {
            DialogHelper.showAlertDialog(context, context.getString(R.string.occur_error));
        }
    }
}
