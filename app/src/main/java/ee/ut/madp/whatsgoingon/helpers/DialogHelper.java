package ee.ut.madp.whatsgoingon.helpers;


import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.design.widget.Snackbar;
import android.view.View;

import ee.ut.madp.whatsgoingon.R;

public class DialogHelper {

    private static ProgressDialog progressDialog;

    /**
     * Shows the progress dialog
     * @param context
     * @param titleMessage
     */
    public static void showProgressDialog(Context context, String titleMessage) {
        if (context != null) {
            progressDialog = new ProgressDialog(context);
            progressDialog.setTitle(titleMessage);
            progressDialog.setMessage(context.getResources().getString(R.string.progress_dialog_wait));
            progressDialog.setCancelable(false);
            progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            progressDialog.show();
        }
    }

    /**
     * Hides the progress dialog
     */
    public static void hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    /**
     * Show alert dialog giving information that something went wrong
     * @param context
     * @param message
     */
    public static void showAlertDialog(Context context, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message)
                .setTitle(R.string.error)
                .setPositiveButton(android.R.string.ok, null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Shows an information message via Snackbar
     * @param coordinatorLayout
     * @param message
     */
    public static void showInformationMessage(View coordinatorLayout, String message) {
        Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_LONG).show();
    }
}
