package ee.ut.madp.whatsgoingon.activities;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.mobsandgeeks.saripaar.ValidationError;
import com.mobsandgeeks.saripaar.Validator;
import com.mobsandgeeks.saripaar.adapter.ViewDataAdapter;
import com.mobsandgeeks.saripaar.annotation.NotEmpty;
import com.mobsandgeeks.saripaar.exception.ConversionException;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ee.ut.madp.whatsgoingon.ModelFactory;
import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.chat.ChatApplication;
import ee.ut.madp.whatsgoingon.chat.Observable;
import ee.ut.madp.whatsgoingon.chat.Observer;
import ee.ut.madp.whatsgoingon.constants.FirebaseConstants;
import ee.ut.madp.whatsgoingon.helpers.DateHelper;
import ee.ut.madp.whatsgoingon.helpers.DialogHelper;
import ee.ut.madp.whatsgoingon.helpers.MyTextWatcherHelper;
import ee.ut.madp.whatsgoingon.models.Event;

import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.PARCEL_EVENT;

public class EventFormActivity extends AppCompatActivity
        implements Validator.ValidationListener, Observer {

    @NotEmpty
    @BindView(R.id.input_layout_eventname)
    TextInputLayout eventName;
    @NotEmpty
    @BindView(R.id.input_layout_eventplace)
    TextInputLayout eventPlace;
    @NotEmpty
    @BindView(R.id.input_layout_date)
    TextInputLayout date;
    @NotEmpty
    @BindView(R.id.input_layout_time)
    TextInputLayout time;

    @BindView(R.id.input_eventname)
    TextInputEditText eventNameInput;
    @BindView(R.id.input_eventplace)
    TextInputEditText eventPlaceInput;
    @BindView(R.id.input_date)
    TextInputEditText dateInput;
    @BindView(R.id.input_time)
    TextInputEditText timeInput;
    @BindView(R.id.input_description)
    TextInputEditText descriptionInput;
    @BindView(R.id.btn_add_event)
    Button addEventButton;
    @BindView(R.id.btn_edit_event)
    Button editEventButton;
    @BindView(R.id.btn_delete_event)
    Button deleteEventButton;
    @BindView(R.id.btn_synchronize)
    Button synchronizeEventButton;
    @BindView(R.id.btn_join_event)
    Button joinEventButton;

    private List<TextInputLayout> inputLayoutList;
    private DatabaseReference eventsRef;
    private Intent data;
    boolean isEdit = false;
    private Event event;

    private ChatApplication application;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form_event);
        ButterKnife.bind(this);

        if (getIntent().hasExtra(PARCEL_EVENT)) {
            isEdit = true;
            setupContentForEdit((Event) getIntent().getParcelableExtra(PARCEL_EVENT));
        }

        application = (ChatApplication) getApplication();
        application.addObserver(this);
        setValidation();
        eventsRef = FirebaseDatabase.getInstance().getReference().child(FirebaseConstants.FIREBASE_CHILD_EVENTS);
    }

    private void setupContentForEdit(Event event) {
        this.event = event;
        eventNameInput.setText(event.getName());
        eventPlaceInput.setText(event.getPlace());
        dateInput.setText(DateHelper.parseDateFromLong(event.getDate()));
        timeInput.setText(DateHelper.parseTimeFromLong(event.getDateTime()));
        descriptionInput.setText(event.getDescription());

        if (FirebaseAuth.getInstance().getCurrentUser().getUid().equals(event.getOwner())) {
            // only owner can edit or delete event
            editEventButton.setVisibility(View.VISIBLE);
            deleteEventButton.setVisibility(View.VISIBLE);
        } else {
            // non owner can join the event, by default the owner is joining the event
            joinEventButton.setVisibility(View.VISIBLE);
        }

        synchronizeEventButton.setVisibility(View.VISIBLE);
        addEventButton.setVisibility(View.GONE);
        setTitle(event.getName());

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        application.deleteObserver(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            data = new Intent();
            setResult(Activity.RESULT_OK, data);
            finish();
        }
        return true;
    }

    @Override
    public void onValidationSucceeded() {
        addEventButton.setEnabled(true);
        addEventButton.setAlpha(1);
        MyTextWatcherHelper.clearAllInputs(inputLayoutList);
    }

    @Override
    public void onValidationFailed(List<ValidationError> errors) {
        if (date.isErrorEnabled() || time.isErrorEnabled()) {
            inputLayoutList.remove(time);
            inputLayoutList.remove(date);
            MyTextWatcherHelper.clearAllInputs(inputLayoutList);
        }

        for (ValidationError error : errors) {
            View view = error.getView();
            String message = error.getCollatedErrorMessage(this);

            if (view instanceof TextInputLayout) {
                ((TextInputLayout) view).setErrorEnabled(true);
                ((TextInputLayout) view).setError(message);
            } else {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        }
        addEventButton.setAlpha(0.7f);
    }

    @Override
    public void update(Observable o, int qualifier, String data) {
        switch (qualifier) {
            case ChatApplication.ONE_TO_ONE_MESSAGE_RECEIVED:
            case ChatApplication.GROUP_MESSAGE_RECEIVED: {
                //TODO show notification
            }
            break;
        }
    }


    @OnClick({R.id.btn_add_event, R.id.btn_edit_event})
    public void createOrEditEvent() {
        String eventName = String.valueOf(eventNameInput.getText());
        String description = String.valueOf(descriptionInput.getText());
        String place = String.valueOf(eventPlaceInput.getText());
        DateTime date = DateHelper.parseDateFromString(String.valueOf(dateInput.getText()));
        DateTime time = DateHelper.parseTimeFromString(String.valueOf(timeInput.getText()));
        DateTime dateTime = date.withTime(time.getHourOfDay(), time.getMinuteOfHour(), time.getSecondOfMinute(), time.getMillisOfSecond());
        String ownerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String id = isEdit ? event.getId() : null;
        Event createdEvent = ModelFactory.createNewEvent(id, eventName, place, description, DateHelper.removeTimeFromDate(date.toDate()).getTime(), ownerId, dateTime.getMillis());
        storeNewEvent(createdEvent);
        if (!isEdit) {
            clearAllInputs();
            if (!inputLayoutList.contains(this.date) && !inputLayoutList.contains(this.time)) {
                inputLayoutList.add(this.date);
                inputLayoutList.add(this.time);
            }
            MyTextWatcherHelper.clearAllInputs(inputLayoutList);

            addEventButton.setEnabled(false);
            addEventButton.setAlpha(0.7f);
        } else {
            setupContentForEdit(createdEvent);
        }

    }


    private void clearAllInputs() {
        eventNameInput.getText().clear();
        eventPlaceInput.getText().clear();
        descriptionInput.getText().clear();
        dateInput.getText().clear();
        timeInput.getText().clear();
    }

    @OnClick(R.id.btn_delete_event)
    public void deleteEvent() {
        if (event != null && isEdit) {
            eventsRef.child(event.getId()).removeValue();
            Toast.makeText(this, getString(R.string.success_message_deleted_event), Toast.LENGTH_SHORT).show();
            finish();
        }

    }

    @OnClick(R.id.input_time)
    public void showTimeDialog() {
        //closeKeyboard();
        time.setError(null);
        time.setErrorEnabled(false);
        DialogHelper.showTimePickerDialog(this, time, date);
    }

    @OnClick(R.id.input_date)
    public void showDateDialog() {
//        closeKeyboard();
        date.setError(null);
        date.setErrorEnabled(false);
        DialogHelper.showDatePickerDialog(this, date);

    }

    @OnClick(R.id.btn_join_event)
    public void joinEvent() {
        if (isEdit && event != null) {
            eventsRef.child(event.getId()).child(FirebaseConstants.FIREBASE_CHILD_EVENTS_ATTENDANTS).push().setValue(FirebaseAuth.getInstance().getCurrentUser().getUid());
        }
    }

    public static final String[] EVENT_PROJECTION = new String[]{
            CalendarContract.Calendars._ID,                           // 0
            CalendarContract.Calendars.ACCOUNT_NAME,                  // 1
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,         // 2
            CalendarContract.Calendars.OWNER_ACCOUNT                  // 3
    };

    // The indices for the projection array above.
    private static final int PROJECTION_ID_INDEX = 0;
    private static final int PROJECTION_ACCOUNT_NAME_INDEX = 1;
    private static final int PROJECTION_DISPLAY_NAME_INDEX = 2;
    private static final int PROJECTION_OWNER_ACCOUNT_INDEX = 3;

    /**
     * Sychronizes events with the existing google calendar
     */
    @OnClick(R.id.btn_synchronize)
    public void synchronizeEvents() {

        Cursor cur = null;
        ContentResolver cr = getContentResolver();
        Uri uri = CalendarContract.Calendars.CONTENT_URI;
// Submit the query and get a Cursor object back.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        cur = cr.query(uri, EVENT_PROJECTION, null, null, null);

        while (cur.moveToNext()) {
            long calID = 0;
            String displayName = null;
            String accountName = null;
            String ownerName = null;

            // Get the field values
            calID = cur.getLong(PROJECTION_ID_INDEX);
            displayName = cur.getString(PROJECTION_DISPLAY_NAME_INDEX);
            accountName = cur.getString(PROJECTION_ACCOUNT_NAME_INDEX);
            ownerName = cur.getString(PROJECTION_OWNER_ACCOUNT_INDEX);

        }


        long calID = 0;
        long startMillis = 0;
        long endMillis = 0;
        Calendar beginTime = Calendar.getInstance();
        beginTime.set(2017, 10, 30, 17, 30);
        startMillis = beginTime.getTimeInMillis();
        Calendar endTime = Calendar.getInstance();
        endTime.set(2017, 11, 2, 8, 45);
        endMillis = endTime.getTimeInMillis();

        ContentValues values = new ContentValues();
        values.put(CalendarContract.Events.DTSTART, startMillis);
        values.put(CalendarContract.Events.DTEND, endMillis);
        values.put(CalendarContract.Events.TITLE, "Jazzercise");
        values.put(CalendarContract.Events.DESCRIPTION, "Group workout");
        values.put(CalendarContract.Events.CALENDAR_ID, calID);
        values.put(CalendarContract.Events.EVENT_TIMEZONE, "America/Los_Angeles");

        Uri uri2 = cr.insert(CalendarContract.Events.CONTENT_URI, values);

// get the event ID that is the last element in the Uri
        long eventID = Long.parseLong(uri.getLastPathSegment());


    }


    private void setValidation() {
        Validator validator = new Validator(this);
        validator.setValidationListener(this);

        validator.registerAdapter(TextInputLayout.class,
                new ViewDataAdapter<TextInputLayout, String>() {
                    @Override
                    public String getData(TextInputLayout flet) throws ConversionException {
                        if (flet.getEditText() != null) {
                            return flet.getEditText().getText().toString();
                        } else {
                            return null;
                        }
                    }
                }
        );

        inputLayoutList = new ArrayList<TextInputLayout>() {{
            add(eventName);
            add(eventPlace);
            add(date);
            add(time);
        }};

        MyTextWatcherHelper.setTextWatcherListeners(inputLayoutList, validator);

        addEventButton.setEnabled(false);
        addEventButton.setAlpha(0.7f);
    }

    private void storeNewEvent(Event event) {
        String id;
        if (!isEdit) {
            id = eventsRef.push().getKey();
            event.setId(id);
        } else {
            id = event.getId();
        }

        eventsRef.child(id).setValue(event);
        String message = isEdit ? getString(R.string.success_message_edit_event) : getString(R.string.message_saved_event);
        Toast.makeText(EventFormActivity.this, message, Toast.LENGTH_SHORT).show();
        closeKeyboard();
    }

    private void closeKeyboard() {
        //TODO finalize this method
        View view = getCurrentFocus();
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
