package ee.ut.madp.whatsgoingon.helpers;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.NumberPicker;
import android.widget.TimePicker;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Calendar;

import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.activities.EventFormActivity;

import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.PREF_REMINDER_HOURS_BEFORE;
import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.DATE_FORMAT;
import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.FULL_DATE_FORMAT;
import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.TIME_FORMAT;

/**
 * Helper for working with dialogs.
 */
public class DialogHelper {

    private static final String TAG = DialogHelper.class.getSimpleName();

    private static ProgressDialog progressDialog;

    /**
     * Shows the progress dialog
     * @param context
     * @param titleMessage
     */
    public static void showProgressDialog(Context context, String titleMessage) {
        Log.i(TAG, "showProgressDialog");
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
        Log.i(TAG, "hideProgressDialog");
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
        Log.i(TAG, "showAlertDialog");
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
        Log.i(TAG, "showInformationMessage");
        Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_LONG).show();
    }

    public static void showTimePickerDialog( final Context context, final TextInputLayout timeInputLayout,
                                             Long dateLong, Long timeLong) {

        Log.i(TAG, "showTimePickerDialog: " + dateLong + ", " + timeLong);
        final Calendar myCalendar = Calendar.getInstance();
        int hours, minutes;
        if (timeLong == null) {
            hours = myCalendar.get(Calendar.HOUR_OF_DAY);
            minutes = myCalendar.get(Calendar.MINUTE);
        } else {
            DateTime time = DateHelper.parseTimeFromString(DateHelper.parseTimeFromLong(timeLong));
            hours = time.getHourOfDay();
            minutes = time.getMinuteOfHour();
        }


        final int year,month, day;
        final DateTime date  = (dateLong == null) ?
                null :
                DateHelper.parseDateFromString(DateHelper.parseDateFromLong(dateLong));
        if (date != null) {
            year = date.getYear();
            month = date.getMonthOfYear();
            day = date.getDayOfMonth();
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
                if (date != null) {
                    if ((DateHelper.isToday(date.getMillis()) && !DateHelper.isFutureTime(calendarTime)) ||
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

    public static void showDatePickerDialog(final Context context, final TextInputLayout textInputLayout,
                                            Long dateLong) {
        Log.i(TAG, "showDatePickerDialog: " + dateLong);
        final Calendar myCalendar = Calendar.getInstance();
        final Calendar c = Calendar.getInstance();
        int year, month, day;
        if (dateLong == null) {
            year = c.get(Calendar.YEAR);
            month = c.get(Calendar.MONTH);
            day = c.get(Calendar.DAY_OF_MONTH);
        } else {
            DateTime date = DateHelper.parseDateFromString(DateHelper.parseDateFromLong(dateLong));
            year = date.getYear();
            month = date.getMonthOfYear() - 1;
            day = date.getDayOfMonth();
        }

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
        Log.i(TAG, "showCalendarSyncDialog");
        final CharSequence[] items = context.getResources().getStringArray(R.array.calendar_types);
        final ArrayList selectedItems = new ArrayList();

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.calendar_type))
                .setMultiChoiceItems(items, null, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int indexSelected, boolean isChecked) {
                        if (isChecked) {
                            // If the user checked the item, add it to the selected items
                            selectedItems.add(items[indexSelected]);
                        } else if (selectedItems.contains(indexSelected)) {
                            // Else, if the item is already in the array, remove it
                            selectedItems.remove(Integer.valueOf(indexSelected));
                        }
                    }
                }).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        if (!selectedItems.isEmpty())
                            EventCalendarHelper.insertEvent(context, EventFormActivity.getEvent(), selectedItems);

                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        //  Your code when user clicked on Cancel
                    }
                }).create();
        dialog.show();
    }

    public static void showNumberPickerDialog(final Context context, int selected) {
        Log.i(TAG, "showNumberPickerDialog: " + selected);
        final NumberPicker numberPicker = new NumberPicker(context);
        numberPicker.setMaxValue(360);
        numberPicker.setMinValue(0);
        numberPicker.setValue(selected);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.hours_before_event_title));
        builder.setMessage(context.getString(R.string.choose_hours));
        builder.setView(numberPicker);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences prefs = context.getSharedPreferences("setting.whatsgoingon", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(PREF_REMINDER_HOURS_BEFORE, numberPicker.getValue());
                editor.commit();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){

            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.show();
    }

}
