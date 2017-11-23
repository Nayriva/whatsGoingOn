package ee.ut.madp.whatsgoingon.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.mobsandgeeks.saripaar.ValidationError;
import com.mobsandgeeks.saripaar.Validator;
import com.mobsandgeeks.saripaar.adapter.ViewDataAdapter;
import com.mobsandgeeks.saripaar.annotation.NotEmpty;
import com.mobsandgeeks.saripaar.exception.ConversionException;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ee.ut.madp.whatsgoingon.ApplicationClass;
import ee.ut.madp.whatsgoingon.ModelFactory;
import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.chat.Observable;
import ee.ut.madp.whatsgoingon.chat.Observer;
import ee.ut.madp.whatsgoingon.constants.FirebaseConstants;
import ee.ut.madp.whatsgoingon.constants.GeneralConstants;
import ee.ut.madp.whatsgoingon.helpers.DateHelper;
import ee.ut.madp.whatsgoingon.helpers.DialogHelper;
import ee.ut.madp.whatsgoingon.helpers.MessageNotificationHelper;
import ee.ut.madp.whatsgoingon.helpers.MyTextWatcherHelper;
import ee.ut.madp.whatsgoingon.helpers.UserHelper;
import ee.ut.madp.whatsgoingon.models.ChatChannel;
import ee.ut.madp.whatsgoingon.models.ChatMessage;
import ee.ut.madp.whatsgoingon.models.Event;

import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.PARCEL_EVENT;
import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.SYNC_CAL_REQUEST_CODE;

public class EventFormActivity extends AppCompatActivity
        implements Validator.ValidationListener, Observer {

    private static final int MY_PERMISSIONS_REQUEST_WRITE_CALENDAR = 5;
    private static final int MY_PERMISSIONS_REQUEST_READ_CALENDAR = 6;
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
    boolean canEdit = false;
    boolean isEdit = false;
    private static Event event;
    private List<String> attendants;

    private ApplicationClass application;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form_event);
        ButterKnife.bind(this);


        eventsRef = FirebaseDatabase.getInstance().getReference().child(FirebaseConstants.FIREBASE_CHILD_EVENTS);
        setValidation();
        if (getIntent().hasExtra(PARCEL_EVENT)) {
            // is Edit
            event = getIntent().getParcelableExtra(PARCEL_EVENT);
            attendants = getIntent().getStringArrayListExtra(GeneralConstants.EVENT_ATTENDANTS);
            event.setAttendantIds(attendants);
            canEdit = UserHelper.getCurrentUserId().equals(event.getOwner());
            isEdit = true;
            setupContent();
        }

        application = (ApplicationClass) getApplication();
        application.addObserver(this);

    }

    private void setupContent() {
        eventNameInput.setText(event.getName());
        eventPlaceInput.setText(event.getPlace());
        dateInput.setText(DateHelper.parseDateFromLong(event.getDate()));
        timeInput.setText(DateHelper.parseTimeFromLong(event.getDateTime()));
        descriptionInput.setText(event.getDescription());

        addEventButton.setVisibility(View.GONE);
        DateTime now = DateTime.now();
        if (now.isBefore(event.getDateTime())) {
            if (canEdit) {
                editEventButton.setVisibility(View.VISIBLE);
                deleteEventButton.setVisibility(View.VISIBLE);
            } else {
                if (event.getAttendantIds().contains(UserHelper.getCurrentUserId())) {
                    joinEventButton.setText(getString(R.string.leave_event));
                } else {
                    joinEventButton.setText(getString(R.string.join_event));
                }
                joinEventButton.setVisibility(View.VISIBLE);
                lockEdits();
            }
        }

        synchronizeEventButton.setEnabled(true);
        synchronizeEventButton.setAlpha(1);
        setTitle(event.getName());
    }

    private void lockEdits() {
        eventNameInput.setEnabled(false);
        eventPlaceInput.setEnabled(false);
        dateInput.setEnabled(false);
        timeInput.setEnabled(false);
        descriptionInput.setEnabled(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        application.deleteObserver(this);
    }

    @Override
    public void onValidationSucceeded() {
        addEventButton.setEnabled(true);
        addEventButton.setAlpha(1);
        synchronizeEventButton.setAlpha(1);
        synchronizeEventButton.setEnabled(true);
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
            case ApplicationClass.ONE_TO_ONE_MESSAGE_RECEIVED:
            case ApplicationClass.GROUP_MESSAGE_RECEIVED: {
                ChatChannel chatChannel = application.getChannel(data);
                ChatMessage lastMessage = application.getLastMessage(data);
                if (chatChannel != null && lastMessage != null) {
                    MessageNotificationHelper.showNotification(this, chatChannel.getName(),
                            chatChannel.getLastMessage(), chatChannel.getId());
                }
            }
            break;
        }
    }

    @OnClick(R.id.btn_add_event)
    public void createEvent() {
        Event createdEvent = collectEventData(false);
        storeEvent(createdEvent, getString(R.string.message_saved_event));

        Intent resultIntent = new Intent();
        resultIntent.putExtra(GeneralConstants.EXTRA_ADDED_EVENT, createdEvent);
        resultIntent.putStringArrayListExtra(GeneralConstants.EVENT_ATTENDANTS,
                (ArrayList<String>) createdEvent.getAttendantIds());
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @OnClick(R.id.btn_edit_event)
    public void editEvent() {
        Event editedEvent = collectEventData(true);
        storeEvent(editedEvent, getString(R.string.success_message_edit_event));

        Intent resultIntent = new Intent();
        resultIntent.putExtra(GeneralConstants.EXTRA_EDITED_EVENT, editedEvent);
        resultIntent.putStringArrayListExtra(GeneralConstants.EVENT_ATTENDANTS, (ArrayList<String>) editedEvent.getAttendantIds());
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @OnClick(R.id.btn_delete_event)
    public void deleteEvent() {
        if (event != null && canEdit) {
            eventsRef.child(event.getId()).removeValue();
            Toast.makeText(this, getString(R.string.success_message_deleted_event), Toast.LENGTH_SHORT).show();
            Intent intent = new Intent();
            intent.putExtra(GeneralConstants.EXTRA_DELETED_EVENT, event.getId());
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    @OnClick(R.id.input_time)
    public void showTimeDialog() {
        time.setError(null);
        time.setErrorEnabled(false);
        DialogHelper.showTimePickerDialog(this, time, date);
    }

    @OnClick(R.id.input_date)
    public void showDateDialog() {
        date.setError(null);
        date.setErrorEnabled(false);
        DialogHelper.showDatePickerDialog(this, date);
    }

    @OnClick(R.id.btn_join_event)
    public void joinEvent() {
        if (event != null) {
            boolean oldJoined = event.isJoined();
            if (!event.isJoined()) {
                event.setJoined(true);
                event.getAttendantIds().add(UserHelper.getCurrentUserId());
            } else {
                event.setJoined(false);
                event.getAttendantIds().remove(UserHelper.getCurrentUserId());
            }
            storeEvent(event, oldJoined ? getString(R.string.message_leave_event) : getString(R.string.message_join_event));

            Intent resultIntent = new Intent();
            resultIntent.putExtra(GeneralConstants.EXTRA_JOINED_EVENT, event);
            resultIntent.putStringArrayListExtra(GeneralConstants.EVENT_ATTENDANTS, (ArrayList<String>) event.getAttendantIds());
            setResult(RESULT_OK, resultIntent);
            finish();
        }
    }

    @OnClick(R.id.btn_synchronize)
    public void synchronizeEvents() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_CALENDAR},
                    MY_PERMISSIONS_REQUEST_WRITE_CALENDAR);

            return;
        }

        showAllCalendars();

//        Event event = collectEventData(isEdit);
//        Intent calIntent = new Intent(Intent.ACTION_INSERT)
//                .setData(CalendarContract.Events.CONTENT_URI)
//                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.getDateTime())
//                .putExtra(CalendarContract.Events.TITLE, event.getName())
//                .putExtra(CalendarContract.Events.DESCRIPTION, event.getDescription())
//                .putExtra(CalendarContract.Events.EVENT_LOCATION, event.getPlace())
//                .putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY);
//        startActivityForResult(calIntent, SYNC_CAL_REQUEST_CODE);

    }

    private void showAllCalendars() {
        String[] projection =
                new String[]{
                        CalendarContract.Calendars._ID,
                        CalendarContract.Calendars.NAME,
                        CalendarContract.Calendars.ACCOUNT_NAME,
                        CalendarContract.Calendars.ACCOUNT_TYPE};
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CALENDAR},
                    MY_PERMISSIONS_REQUEST_READ_CALENDAR);
            return;
        }
        DialogHelper.showCalendarSyncDialog(this);
//        Cursor calCursor =
//                getContentResolver().
//                        query(CalendarContract.Calendars.CONTENT_URI,
//                                projection,
//                                CalendarContract.Calendars.VISIBLE + " = 1 AND " + CalendarContract.Calendars._ID + " = 1 ",
//                                null,
//                                CalendarContract.Calendars._ID + " ASC");
//        if (calCursor.moveToFirst()) {
//            do {
//                long id = calCursor.getLong(0);
//                String displayName = calCursor.getString(1);
//                // ...
//            } while (calCursor.moveToNext());
//        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == SYNC_CAL_REQUEST_CODE) {
                Log.i("Events", "Povedlo se");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_CALENDAR: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    synchronizeEvents();

                    Toast.makeText(EventFormActivity.this, "Granted", Toast.LENGTH_LONG).show();
                    // permission was granted, yay! do the
                    // calendar task you need to do.

                } else {

                    Toast.makeText(EventFormActivity.this, "Neni granted", Toast.LENGTH_LONG).show();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'switch' lines to check for other
            // permissions this app might request
        }
    }

    private Event collectEventData(boolean isEdit) {
        String eventName = String.valueOf(eventNameInput.getText());
        String description = String.valueOf(descriptionInput.getText());
        String place = String.valueOf(eventPlaceInput.getText());
        DateTime date = DateHelper.parseDateFromString(String.valueOf(dateInput.getText()));
        DateTime time = DateHelper.parseTimeFromString(String.valueOf(timeInput.getText()));
        DateTime dateTime = date.withTime(time.getHourOfDay(), time.getMinuteOfHour(),
                time.getSecondOfMinute(), time.getMillisOfSecond());
        String ownerId = UserHelper.getCurrentUserId();
        Event createdEvent;
        if (isEdit)
            createdEvent = ModelFactory.createNewEvent(event.getId(), eventName, place, description,
                    DateHelper.removeTimeFromDate(date.toDate()).getTime(), ownerId, dateTime.getMillis());
        else createdEvent = ModelFactory.createNewEvent(null, eventName, place, description,
                DateHelper.removeTimeFromDate(date.toDate()).getTime(), ownerId, dateTime.getMillis());
        return createdEvent;
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
        // possible to synchronize if the data has been filled in
        synchronizeEventButton.setEnabled(false);
        synchronizeEventButton.setAlpha(0.7f);
    }

    private void storeEvent(Event event, String message) {
        String id;
        if (event.getId() == null) {
            id = eventsRef.push().getKey();
            event.setId(id);
        } else {
            id = event.getId();
        }

        eventsRef.child(id).setValue(event);
        Toast.makeText(EventFormActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return  true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static Event getEvent() {
        return event;
    }
}
