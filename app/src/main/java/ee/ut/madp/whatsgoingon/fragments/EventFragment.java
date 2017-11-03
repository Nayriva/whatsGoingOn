package ee.ut.madp.whatsgoingon.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.imanoweb.calendarview.CalendarListener;
import com.imanoweb.calendarview.CustomCalendarView;
import com.imanoweb.calendarview.DayDecorator;
import com.imanoweb.calendarview.DayView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ee.ut.madp.whatsgoingon.ChatApplication;
import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.activities.EventFormActivity;
import ee.ut.madp.whatsgoingon.activities.EventsOnDayActivity;
import ee.ut.madp.whatsgoingon.chat.Observable;
import ee.ut.madp.whatsgoingon.chat.Observer;
import ee.ut.madp.whatsgoingon.constants.FirebaseConstants;
import ee.ut.madp.whatsgoingon.constants.GeneralConstants;
import ee.ut.madp.whatsgoingon.helpers.DateHelper;
import ee.ut.madp.whatsgoingon.helpers.DialogHelper;
import ee.ut.madp.whatsgoingon.helpers.MessageNotificationHelper;
import ee.ut.madp.whatsgoingon.helpers.UserHelper;
import ee.ut.madp.whatsgoingon.models.ChatChannel;
import ee.ut.madp.whatsgoingon.models.ChatMessage;
import ee.ut.madp.whatsgoingon.models.Event;

import static android.app.Activity.RESULT_OK;
import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.EVENTS_REQUEST_CODE;
import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.EVENT_DAY_BECAME_EMPTY;
import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.PARAM_EVENT_DAY;

public class EventFragment extends Fragment implements Observer {

    private static final int CREATE_EVENT_REQUEST_CODE = 0;
    @BindView(R.id.calendar_view)
    CustomCalendarView calendarView;

    private Calendar currentCalendar;
    private DatabaseReference eventsRef;
    private List<Date> daysWithEvents = new ArrayList<>();
    private List<Event> events = new ArrayList<>();
    private ValueEventListener valueEventListener;
    private ChatApplication application;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CREATE_EVENT_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data.hasExtra(GeneralConstants.EXTRA_ADDED_EVENT)) {
                Event event = data.getParcelableExtra(GeneralConstants.EXTRA_ADDED_EVENT);
                daysWithEvents.add(DateHelper.removeTimeFromDate(new Date(event.getDateTime())));
            }
        }
        if (requestCode == EVENTS_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data.hasExtra(EVENT_DAY_BECAME_EMPTY)) {
                long day = data.getLongExtra(EVENT_DAY_BECAME_EMPTY, 0);
                if (day != 0) {
                    daysWithEvents.remove(DateHelper.removeTimeFromDate(new Date(day)));
                }
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        application = (ChatApplication) getActivity().getApplication();

        DialogHelper.showProgressDialog(getContext(), getResources().getString(R.string.downloading_data));
        eventsRef = FirebaseDatabase.getInstance().getReference().child(FirebaseConstants.FIREBASE_CHILD_EVENTS);
        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot eventSnapshot : dataSnapshot.getChildren()) {
                    Event event = eventSnapshot.getValue(Event.class);
                    if (event != null) {
                        events.add(event);
                        if (event.getAttendantIds().contains(UserHelper.getCurrentUserId())) {
                            event.setJoined(true);
                        } else {
                            event.setJoined(false);
                        }
                        daysWithEvents.add(DateHelper.removeTimeFromDate(new Date(event.getDateTime())));
                    }
                }
                setDaysWithEvents();
                DialogHelper.hideProgressDialog();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                //left blank intentionally
            }
        };

        eventsRef.addListenerForSingleValueEvent(valueEventListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        eventsRef.removeEventListener(valueEventListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        application.addObserver(this);
        setDaysWithEvents();
        calendarView.setCalendarListener(new CalendarListener() {
            @Override
            public void onDateSelected(Date date) {
                if (daysWithEvents.contains(DateHelper.removeTimeFromDate(date))) {
                    Intent intent = new Intent(getActivity(), EventsOnDayActivity.class);
                    intent.putExtra(PARAM_EVENT_DAY, DateHelper.removeTimeFromDate(date).getTime());
                    getActivity().startActivityForResult(intent, EVENTS_REQUEST_CODE);
                }
            }

            @Override
            public void onMonthChanged(Date date) {
                //left blank intentionally
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        application.deleteObserver(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_event, container, false);
        ButterKnife.bind(this, view);

        currentCalendar = Calendar.getInstance();
        return view;
    }

    @OnClick(R.id.btn_add_event)
    public void showAddEventForm() {
        startActivityForResult(
                new Intent(getActivity(), EventFormActivity.class),
                CREATE_EVENT_REQUEST_CODE);
    }

    @Override
    public void update(Observable o, int qualifier, String data) {
        switch (qualifier) {
            case ChatApplication.ONE_TO_ONE_MESSAGE_RECEIVED:
            case ChatApplication.GROUP_MESSAGE_RECEIVED: {
                ChatChannel chatChannel = application.getChannel(data);
                ChatMessage lastMessage = application.getLastMessage(data);
                if (chatChannel != null && lastMessage != null) {
                    MessageNotificationHelper.showNotification(getContext(), chatChannel.getName(),
                            chatChannel.getLastMessage());
                }
            } break;
        }
    }

    private void setDaysWithEvents() {
        if (daysWithEvents.size() > 0) {
            List<DayDecorator> decorators = new ArrayList<>();
            decorators.add(new EventColorDecorator());
            calendarView.setDecorators(decorators);
            calendarView.refreshCalendar(currentCalendar);
        }
    }

    private class EventColorDecorator implements DayDecorator {

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void decorate(DayView dayView) {
            for (Date eventDay : daysWithEvents) {
                if (DateHelper.removeTimeFromDate(dayView.getDate()).equals(eventDay)) {
                    dayView.setTextColor(Color.WHITE);
                    dayView.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.colorAccent));
                }

                if (DateHelper.isToday(dayView.getDate().getTime())) {
                    dayView.setPaintFlags(dayView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
                    dayView.setTextColor(Color.WHITE);
                }
            }
        }
    }
}
