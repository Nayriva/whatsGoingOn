package ee.ut.madp.whatsgoingon.activities;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
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
import java.util.Calendar;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ee.ut.madp.whatsgoingon.ModelFactory;
import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.ChatApplication;
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

public class EventFormActivity extends AppCompatActivity
        implements Validator.ValidationListener, Observer {

    @NotEmpty @BindView(R.id.input_layout_eventname) TextInputLayout eventName;
    @NotEmpty @BindView(R.id.input_layout_eventplace) TextInputLayout eventPlace;
    @NotEmpty @BindView(R.id.input_layout_date) TextInputLayout date;
    @NotEmpty @BindView(R.id.input_layout_time) TextInputLayout time;

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
    private Event event;
    private List<String> attendants;

    private ChatApplication application;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form_event);
        ButterKnife.bind(this);

        eventsRef = FirebaseDatabase.getInstance().getReference().child(FirebaseConstants.FIREBASE_CHILD_EVENTS);
        if (getIntent().hasExtra(PARCEL_EVENT)) {
            event = getIntent().getParcelableExtra(PARCEL_EVENT);
            attendants = getIntent().getStringArrayListExtra(GeneralConstants.EVENT_ATTENDANTS);
            event.setAttendantIds(attendants);
            canEdit = UserHelper.getCurrentUserId().equals(event.getOwner());
            setupContent();
        }

        application = (ChatApplication) getApplication();
        application.addObserver(this);
        setValidation();
    }

    private void setupContent() {
        eventNameInput.setText(event.getName());
        eventPlaceInput.setText(event.getPlace());
        dateInput.setText(DateHelper.parseDateFromLong(event.getDate()));
        timeInput.setText(DateHelper.parseTimeFromLong(event.getDateTime()));
        descriptionInput.setText(event.getDescription());

        addEventButton.setVisibility(View.GONE);
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

        synchronizeEventButton.setVisibility(View.VISIBLE);
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
                ChatChannel chatChannel = application.getChannel(data);
                ChatMessage lastMessage = application.getLastMessage(data);
                if (chatChannel != null && lastMessage != null) {
                    MessageNotificationHelper.showNotification(this, chatChannel.getName(),
                            chatChannel.getLastMessage());
                }
            } break;
        }
    }

    @OnClick(R.id.btn_add_event)
    public void createEvent() {
        String eventName = String.valueOf(eventNameInput.getText());
        String description = String.valueOf(descriptionInput.getText());
        String place = String.valueOf(eventPlaceInput.getText());
        DateTime date = DateHelper.parseDateFromString(String.valueOf(dateInput.getText()));
        DateTime time = DateHelper.parseTimeFromString(String.valueOf(timeInput.getText()));
        DateTime dateTime = date.withTime(time.getHourOfDay(), time.getMinuteOfHour(), time.getSecondOfMinute(), time.getMillisOfSecond());
        String ownerId = UserHelper.getCurrentUserId();
        Event createdEvent = ModelFactory.createNewEvent(null, eventName, place, description,
                DateHelper.removeTimeFromDate(date.toDate()).getTime(), ownerId, dateTime.getMillis());
        storeEvent(createdEvent, getString(R.string.message_saved_event));

        Intent resultIntent = new Intent();
        resultIntent.putExtra(GeneralConstants.EXTRA_ADDED_EVENT, createdEvent);
        resultIntent.putStringArrayListExtra(GeneralConstants.EVENT_ATTENDANTS, (ArrayList<String>) event.getAttendantIds());
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @OnClick(R.id.btn_edit_event)
    public void editEvent() {
        event.setName(String.valueOf(eventNameInput.getText()));
        event.setPlace(String.valueOf(eventPlaceInput.getText()));
        event.setDescription(String.valueOf(descriptionInput.getText()));
        DateTime date = DateHelper.parseDateFromString(String.valueOf(dateInput.getText()));
        DateTime time = DateHelper.parseTimeFromString(String.valueOf(timeInput.getText()));
        DateTime dateTime = date.withTime(time.getHourOfDay(), time.getMinuteOfHour(),
                time.getSecondOfMinute(), time.getMillisOfSecond());
        event.setDate(DateHelper.removeTimeFromDate(date.toDate()).getTime());
        event.setDateTime(dateTime.getMillis());
        storeEvent(event, getString(R.string.success_message_edit_event));

        Intent resultIntent = new Intent();
        resultIntent.putExtra(GeneralConstants.EXTRA_EDITED_EVENT, event);
        resultIntent.putStringArrayListExtra(GeneralConstants.EVENT_ATTENDANTS, (ArrayList<String>) event.getAttendantIds());
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
            storeEvent(event, oldJoined ? getString(R.string.join_event) : getString(R.string.message_leave_event));

            Intent resultIntent = new Intent();
            resultIntent.putExtra(GeneralConstants.EXTRA_JOINED_EVENT, event);
            resultIntent.putStringArrayListExtra(GeneralConstants.EVENT_ATTENDANTS, (ArrayList<String>) event.getAttendantIds());
            setResult(RESULT_OK, resultIntent);
            finish();
        }
    }

    @OnClick(R.id.btn_synchronize)
    public void synchronizeEvents() {
        //TODO is working?
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        long calID = 0;
        long startMillis;
        long endMillis;
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
}
