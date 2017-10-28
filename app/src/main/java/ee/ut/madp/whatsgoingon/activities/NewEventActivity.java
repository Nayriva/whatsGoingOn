package ee.ut.madp.whatsgoingon.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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

public class NewEventActivity extends AppCompatActivity
        implements Validator.ValidationListener, Observer {

    @NotEmpty
    @BindView(R.id.input_layout_eventname) TextInputLayout eventName;
    @NotEmpty
    @BindView(R.id.input_layout_date) TextInputLayout date;
    @NotEmpty
    @BindView(R.id.input_layout_time) TextInputLayout time;

    @BindView(R.id.input_eventname) TextInputEditText eventNameInput;
    @BindView(R.id.input_date) TextInputEditText dateInput;
    @BindView(R.id.input_time) TextInputEditText timeInput;
    @BindView(R.id.input_description) TextInputEditText descriptionInput;
    @BindView(R.id.btn_add_event) Button addEventButton;

    private List<TextInputLayout> inputLayoutList;
    private DatabaseReference eventsRef;
    private Intent data;
    private boolean saved = false;

    private ChatApplication application;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_event);
        ButterKnife.bind(this);

        application = (ChatApplication) getApplication();
        application.addObserver(this);
        setValidation();
        eventsRef = FirebaseDatabase.getInstance().getReference().child(FirebaseConstants.FIREBASE_CHILD_EVENTS);
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
           // if (saved )
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
            } break;
        }
    }

    @OnClick(R.id.btn_add_event)
    public void createNewEvent() {
        String eventName = String.valueOf(eventNameInput.getText());
        String description = String.valueOf(descriptionInput.getText());
        DateTime date = DateHelper.parseDateFromString(String.valueOf(dateInput.getText()));
        DateTime time = DateHelper.parseTimeFromString(String.valueOf(timeInput.getText()));
        date = date.withTime(time.getHourOfDay(), time.getMinuteOfHour(), time.getSecondOfMinute(), time.getMillisOfSecond());
        storeNewEvent(ModelFactory.createNewEvent(null, eventName, date.getMillis(), description));
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
            add(date);
            add(time);
        }};

        MyTextWatcherHelper.setTextWatcherListeners(inputLayoutList, validator);

        addEventButton.setEnabled(false);
        addEventButton.setAlpha(0.7f);
    }

    private void storeNewEvent(Event event) {
        String id = eventsRef.push().getKey();
        event.setId(id);
        eventsRef.child(id).setValue(event);
        saved = true;
    }

    private void closeKeyboard() {
        //TODO finalize this method
        View view = getCurrentFocus();
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
