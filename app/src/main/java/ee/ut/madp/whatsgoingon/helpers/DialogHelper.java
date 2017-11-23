package ee.ut.madp.whatsgoingon.helpers;


import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TimePicker;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Calendar;

import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.activities.EventFormActivity;

import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.DATE_FORMAT;
import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.FULL_DATE_FORMAT;
import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.TIME_FORMAT;

public class DialogHelper {

    private static ProgressDialog progressDialog;

    /**
     * Shows the progress dialog
     * @param context
     * @param titleMessage
     */
    public static void showProgressDialog(Context context, String titleMessage) {
        if (context != null) {
            progressDialog = new ProgressDialog(context,R.style.MyAlertDialogStyle);
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
            progressDialog = null;
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

    public static void showTimePickerDialog( final Context context, final TextInputLayout timeInputLayout, final TextInputLayout dateInputLayout) {

        final Calendar myCalendar = Calendar.getInstance();

        final int hours = myCalendar.get(Calendar.HOUR_OF_DAY);
        final int minutes = myCalendar.get(Calendar.MINUTE);

        final DateTime chosenDate = DateHelper.parseDateFromString(dateInputLayout.getEditText().getText().toString());
        final int year,month, day;
        if (chosenDate != null) {
            year = chosenDate.getYear();
            month = chosenDate.getMonthOfYear();
            day = chosenDate.getDayOfMonth();
        } else {
            year = myCalendar.get(Calendar.YEAR);
            month = myCalendar.get(Calendar.MONTH);
            day = myCalendar.get(Calendar.DAY_OF_MONTH);
        }


        TimePickerDialog.OnTimeSetListener timeSetListener = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int hourOfDay, int minute) {
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH, month - 1);
                myCalendar.set(Calendar.DAY_OF_MONTH, day);
                myCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                myCalendar.set(Calendar.MINUTE, minute);

                long calendarTime = myCalendar.getTime().getTime();

                DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(TIME_FORMAT);
                if (chosenDate != null) {
                    if ((DateHelper.isToday(chosenDate.getMillis()) && !DateHelper.isFutureTime(calendarTime)) ||
                            DateHelper.isPast(calendarTime)) {
                        timeInputLayout.setErrorEnabled(true);
                        timeInputLayout.setError(context.getString(R.string.error_time_past));

                    }
                }

                timeInputLayout.getEditText().setText(dateTimeFormatter.print(myCalendar.getTime().getTime()));

            }
        };

        new TimePickerDialog(context, timeSetListener, hours, minutes,
                DateFormat.is24HourFormat(context)).show();

    }

    public static void showDatePickerDialog(final Context context, final TextInputLayout textInputLayout) {
        final Calendar myCalendar = Calendar.getInstance();
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int monthOfYear, int dayOfMonth) {
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH, monthOfYear);
                myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                if (DateHelper.isPast(myCalendar.getTime().getTime())) {
                    textInputLayout.setErrorEnabled(true);
                    textInputLayout.setError(context.getString(R.string.error_date_past));
                }

                String pattern = DateHelper.isSameYear(myCalendar.getTime().getTime()) ?  DATE_FORMAT: FULL_DATE_FORMAT;
                DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(pattern);

                textInputLayout.getEditText().setText(dateTimeFormatter.print(myCalendar.getTime().getTime()));


            }
        };

        new DatePickerDialog(context, dateSetListener, year, month, day).show();
    }

    public static void showCalendarSyncDialog(final Context context) {
        final CharSequence[] items = context.getResources().getStringArray(R.array.calendar_types);
        final ArrayList seletedItems = new ArrayList();

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.calendar_type))
                .setMultiChoiceItems(items, null, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int indexSelected, boolean isChecked) {
                        if (isChecked) {
                            // If the user checked the item, add it to the selected items
                            seletedItems.add(items[indexSelected]);
                        } else if (seletedItems.contains(indexSelected)) {
                            // Else, if the item is already in the array, remove it
                            seletedItems.remove(Integer.valueOf(indexSelected));
                        }
                    }
                }).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        if (!seletedItems.isEmpty())
                            EventCalendarHelper.insertEvent(context, EventFormActivity.getEvent(), seletedItems);

                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        //  Your code when user clicked on Cancel
                    }
                }).create();
        dialog.show();
    }

}
