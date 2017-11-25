package ee.ut.madp.whatsgoingon.activities;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mobsandgeeks.saripaar.ValidationError;
import com.mobsandgeeks.saripaar.Validator;
import com.mobsandgeeks.saripaar.adapter.ViewDataAdapter;
import com.mobsandgeeks.saripaar.annotation.NotEmpty;
import com.mobsandgeeks.saripaar.exception.ConversionException;

import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ee.ut.madp.whatsgoingon.ApplicationClass;
import ee.ut.madp.whatsgoingon.ModelFactory;
import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.asynctasks.InsertEventAsyncTask;
import ee.ut.madp.whatsgoingon.chat.Observable;
import ee.ut.madp.whatsgoingon.chat.Observer;
import ee.ut.madp.whatsgoingon.constants.FirebaseConstants;
import ee.ut.madp.whatsgoingon.constants.GeneralConstants;
import ee.ut.madp.whatsgoingon.constants.PermissionConstants;
import ee.ut.madp.whatsgoingon.helpers.DateHelper;
import ee.ut.madp.whatsgoingon.helpers.DialogHelper;
import ee.ut.madp.whatsgoingon.helpers.EventCalendarHelper;
import ee.ut.madp.whatsgoingon.helpers.MessageNotificationHelper;
import ee.ut.madp.whatsgoingon.helpers.MyTextWatcherHelper;
import ee.ut.madp.whatsgoingon.helpers.UserHelper;
import ee.ut.madp.whatsgoingon.models.ChatChannel;
import ee.ut.madp.whatsgoingon.models.ChatMessage;
import ee.ut.madp.whatsgoingon.models.Event;
import ee.ut.madp.whatsgoingon.models.GoogleAccountHelper;
import ee.ut.madp.whatsgoingon.reminder.ReminderManager;

import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.EXTRA_EVENT;
import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.EXTRA_EVENT_ID;
import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.PREF_ACCOUNT_NAME;
import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.PREF_REMINDERS;
import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.PREF_REMINDER_HOURS_BEFORE;
import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.REQUEST_ACCOUNT_PICKER;
import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.REQUEST_AUTHORIZATION;
import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.REQUEST_GOOGLE_PLAY_SERVICES;
import static ee.ut.madp.whatsgoingon.models.GoogleAccountHelper.getGoogleAccountCredential;

/**
 * Activity displays form for event. This form can be used for creating, editing or just viewing
 * (with possibility to join) of event. Only owner of the event can edit / delete event. Non-owner
 * can join the event.
 */
public class EventFormActivity extends AppCompatActivity
        implements Validator.ValidationListener, Observer {

    private static final int SHARE_EVENT_REQUEST_CODE = 1;
    private static final String TAG = EventFormActivity.class.getSimpleName();
    @NotEmpty
    @BindView(R.id.input_layout_eventname) TextInputLayout eventName;
    @NotEmpty
    @BindView(R.id.input_layout_eventplace) TextInputLayout eventPlace;
    @NotEmpty
    @BindView(R.id.input_layout_date) TextInputLayout date;
    @NotEmpty
    @BindView(R.id.input_layout_time) TextInputLayout time;

    @BindView(R.id.input_eventname) TextInputEditText eventNameInput;
    @BindView(R.id.input_eventplace) TextInputEditText eventPlaceInput;
    @BindView(R.id.input_date) TextInputEditText dateInput;
    @BindView(R.id.input_time) TextInputEditText timeInput;
    @BindView(R.id.input_description) TextInputEditText descriptionInput;
    @BindView(R.id.btn_add_event) Button addEventButton;
    @BindView(R.id.btn_edit_event) Button editEventButton;
    @BindView(R.id.btn_delete_event) Button deleteEventButton;
    @BindView(R.id.btn_synchronize) Button synchronizeEventButton;
    @BindView(R.id.btn_join_event) Button joinEventButton;

    private List<TextInputLayout> inputLayoutList;
    private DatabaseReference eventsRef;
    boolean canEdit = false;
    boolean isEdit = false;
    private static Event event;
    private List<String> attendants;

    private ApplicationClass application;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form_event);
        ButterKnife.bind(this);

        eventsRef = FirebaseDatabase.getInstance().getReference().child(FirebaseConstants.FIREBASE_CHILD_EVENTS);
        if (getIntent().hasExtra(EXTRA_EVENT) || getIntent().hasExtra(EXTRA_EVENT_ID)) {
            // is Edit
            if (getIntent().hasExtra(EXTRA_EVENT_ID))
                retrieveEventFromFirebase(getIntent().getStringExtra(EXTRA_EVENT_ID));
            else {
                event = getIntent().getParcelableExtra(EXTRA_EVENT);
                setupContent();
            }
        } else {
            event = null;
        }

        application = (ApplicationClass) getApplication();

    }

    private void setupInfoAboutEvent() {
        Log.i(TAG, "setupInfoAboutEvent");
        attendants = getIntent().getStringArrayListExtra(GeneralConstants.EXTRA_EVENT_ATTENDANTS);
        event.setAttendantIds(attendants);
        canEdit = UserHelper.getCurrentUserId().equals(event.getOwner());
        isEdit = true;
    }

    private void retrieveEventFromFirebase(String eventId) {
        Log.i (TAG, "retrieveEventFromFirebase: " + eventId);
        eventsRef.child(eventId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.i(TAG, "retrieveEventFromFirebase.onDataChange");
                event = dataSnapshot.getValue(Event.class);
                setupContent();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void setupContent() {
        Log.i(TAG, "setupContent");
        setupInfoAboutEvent();
        setValidation();
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
        } else {
            lockEdits();
        }

        synchronizeEventButton.setEnabled(true);
        synchronizeEventButton.setAlpha(1);
        setTitle(event.getName());
    }

    private void lockEdits() {
        Log.i(TAG, "lockEdits");
        eventNameInput.setEnabled(false);
        eventPlaceInput.setEnabled(false);
        dateInput.setEnabled(false);
        timeInput.setEnabled(false);
        descriptionInput.setEnabled(false);
    }

    @Override
    public void onValidationSucceeded() {
        Log.i(TAG, "onValidationSucceeded");
        addEventButton.setEnabled(true);
        addEventButton.setAlpha(1);
        synchronizeEventButton.setAlpha(1);
        synchronizeEventButton.setEnabled(true);
        MyTextWatcherHelper.clearAllInputs(inputLayoutList);
    }

    @Override
    public void onValidationFailed(List<ValidationError> errors) {
        Log.i(TAG, "onValidationFailed: " + errors);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "onCreateOptionsMenu");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.event_share_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "onOptionsItemSelected");
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.home: {
                onBackPressed();
                return true;
            }
            case R.id.share_button:
                if (event != null) {
                    shareEvent(event);
                } else {
                    shareEvent(collectEventData(false));
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
        Log.i(TAG, "createEvent");
        Event createdEvent = collectEventData(false);
        storeEvent(createdEvent, getString(R.string.message_saved_event));

        SharedPreferences prefs = getSharedPreferences("setting.whatsgoingon", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        if (prefs.getBoolean(PREF_REMINDERS, true) && prefs.getInt(PREF_REMINDER_HOURS_BEFORE, 1) > 0) {
            // notify user hours (defined from settings) before
            long subtractedTime = new LocalDateTime(createdEvent.getDateTime()).minusHours(prefs.getInt(PREF_REMINDER_HOURS_BEFORE, 1)).toDateTime().getMillis();
            new ReminderManager(this).setReminder(createdEvent, subtractedTime);
        }

        Intent resultIntent = new Intent();
        resultIntent.putExtra(GeneralConstants.EXTRA_ADDED_EVENT, createdEvent);
        resultIntent.putStringArrayListExtra(GeneralConstants.EXTRA_EVENT_ATTENDANTS,
                (ArrayList<String>) createdEvent.getAttendantIds());
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @OnClick(R.id.btn_edit_event)
    public void editEvent() {
        Log.i(TAG, "editEvent");
        Event editedEvent = collectEventData(true);
        storeEvent(editedEvent, getString(R.string.success_message_edit_event));
        EventCalendarHelper.updateEvent(EventFormActivity.this, editedEvent);

        Intent resultIntent = new Intent();
        resultIntent.putExtra(GeneralConstants.EXTRA_EDITED_EVENT, editedEvent);
        resultIntent.putStringArrayListExtra(GeneralConstants.EXTRA_EVENT_ATTENDANTS, (ArrayList<String>) editedEvent.getAttendantIds());
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @OnClick(R.id.btn_delete_event)
    public void deleteEvent() {
        Log.i(TAG, "deleteEvent");
        if (event != null && canEdit) {
            eventsRef.child(event.getId()).removeValue();
            if (event.getEventId() != 0 || event.getGoogleEventId() != null) EventCalendarHelper.deleteEvent(this, event.getEventId(), event.getGoogleEventId());

            Toast.makeText(this, getString(R.string.success_message_deleted_event), Toast.LENGTH_SHORT).show();
            Intent intent = new Intent();
            intent.putExtra(GeneralConstants.EXTRA_DELETED_EVENT, event.getId());
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    @OnClick(R.id.input_time)
    public void showTimeDialog() {
        Log.i(TAG, "showTimeDialog");
        time.setError(null);
        time.setErrorEnabled(false);
        if (event == null) {
            DialogHelper.showTimePickerDialog(this, time, null, null);
        } else {
            DialogHelper.showTimePickerDialog(this, time, event.getDate(), event.getDateTime());
        }
    }

    @OnClick(R.id.input_date)
    public void showDateDialog() {
        Log.i(TAG, "showDateDialog");
        date.setError(null);
        date.setErrorEnabled(false);
        if (event == null) {
            DialogHelper.showDatePickerDialog(this, date, null);
        } else {
            DialogHelper.showDatePickerDialog(this, date, event.getDate());
        }
    }

    @OnClick(R.id.btn_join_event)
    public void joinEvent() {
        Log.i(TAG, "joinEvent");
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
            resultIntent.putStringArrayListExtra(GeneralConstants.EXTRA_EVENT_ATTENDANTS, (ArrayList<String>) event.getAttendantIds());
            setResult(RESULT_OK, resultIntent);
            finish();
        }
    }

    @OnClick(R.id.btn_synchronize)
    public void synchronizeEvents() {
        Log.i(TAG, "synchronizeEvents");

        DialogHelper.showCalendarSyncDialog(this);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult: " + requestCode + ", " + resultCode + ", " + data);
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode == Activity.RESULT_OK) {
                    // has been installed
                    GoogleAccountHelper.haveGooglePlayServices(this, GoogleAccountHelper.getGoogleAccountCredential(this));
                } else {
                    GoogleAccountHelper.checkGooglePlayServicesAvailable(this);
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == Activity.RESULT_OK) {
                    new InsertEventAsyncTask(EventFormActivity.this, EventCalendarHelper.initializeCalendarService(EventFormActivity.this),
                            event.getId(), EventCalendarHelper.createGoogleEvent(event)).execute();
                } else {
                    GoogleAccountHelper.chooseAccount(this);
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == Activity.RESULT_OK && data != null && data.getExtras() != null) {
                    String accountName = data.getExtras().getString(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        getGoogleAccountCredential(this).setSelectedAccountName(accountName);
                        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.commit();
                        new InsertEventAsyncTask(EventFormActivity.this, EventCalendarHelper.initializeCalendarService(EventFormActivity.this),
                                event.getId(), EventCalendarHelper.createGoogleEvent(event)).execute();
                    }
                }
                break;
        }
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
        application.addObserver(this);
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
        application.deleteObserver(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        Log.i(TAG, "onRequestPermissionsResult");
        switch (requestCode) {
            case PermissionConstants.PERMISSIONS_GROUP_TWO: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    synchronizeEvents();
                    Toast.makeText(EventFormActivity.this, "Permission was granted", Toast.LENGTH_LONG).show();

                } else {
                    Toast.makeText(EventFormActivity.this, "Permission was granted", Toast.LENGTH_LONG).show();
                    synchronizeEventButton.setVisibility(View.GONE);
                }
                return;
            }

        }
    }

    private Event collectEventData(boolean isEdit) {
        Log.i(TAG, "collectEventData: " + isEdit);
        String eventName = String.valueOf(eventNameInput.getText());
        String description = String.valueOf(descriptionInput.getText());
        String place = String.valueOf(eventPlaceInput.getText());
        DateTime date = DateHelper.parseDateFromString(String.valueOf(dateInput.getText()));
        DateTime time = DateHelper.parseTimeFromString(String.valueOf(timeInput.getText()));
        DateTime dateTime = date.withTime(time.getHourOfDay(), time.getMinuteOfHour(),
                time.getSecondOfMinute(), time.getMillisOfSecond());
        String ownerId = UserHelper.getCurrentUserId();
        Event createdEvent;
        if (isEdit) {
            createdEvent = ModelFactory.createNewEvent(event.getId(), eventName, place, description,
                    DateHelper.removeTimeFromDate(date.toDate()).getTime(), ownerId, dateTime.getMillis());
            if (event.getEventId() != 0) createdEvent.setEventId(event.getEventId());
            if (event.getGoogleEventId() != null) createdEvent.setGoogleEventId(event.getGoogleEventId());
        }

        else createdEvent = ModelFactory.createNewEvent(null, eventName, place, description,
                DateHelper.removeTimeFromDate(date.toDate()).getTime(), ownerId, dateTime.getMillis());

        return createdEvent;
    }

    private void setValidation() {
        Log.i(TAG, "setValidation");
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
        Log.i(TAG, "storeEvent");
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

    private void shareEvent(Event event) {
        Log.i(TAG, "shareEvent");
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.putExtra(EXTRA_EVENT_ID, event.getId());
        sharingIntent.setType("text/plain");
        String subject = event.getName();
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
        String text = DateHelper.parseDateFromLong(event.getDateTime()) + " at " + DateHelper.parseTimeFromLong(event.getDateTime()) + ", " + event.getPlace();
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, text);
        startActivityForResult(Intent.createChooser(sharingIntent, "Sharing option"), SHARE_EVENT_REQUEST_CODE);
    }

    public static Event getEvent() {
        Log.i(TAG, "getEvent");
        return event;
    }
}
