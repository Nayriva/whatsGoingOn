package ee.ut.madp.whatsgoingon.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
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
import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.activities.EventsOnDayActivity;
import ee.ut.madp.whatsgoingon.activities.NewEventActivity;
import ee.ut.madp.whatsgoingon.constants.FirebaseConstants;
import ee.ut.madp.whatsgoingon.helpers.DateHelper;
import ee.ut.madp.whatsgoingon.models.Event;

import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.EVENTS_REQUEST_CODE;
import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.EVENT_DAY_REQUEST_CODE;

public class EventFragment extends Fragment {

    @BindView(R.id.calendar_view)
    CustomCalendarView calendarView;

    private Calendar currentCalendar;
    private DatabaseReference eventsRef;
    private List<Date> eventDays = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        eventsRef = FirebaseDatabase.getInstance().getReference().child(FirebaseConstants.FIREBASE_CHILD_EVENTS);
        eventsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    Event event = postSnapshot.getValue(Event.class);
                    if (event != null) {
                        eventDays.add(DateHelper.removeTimeFromDate(new Date(event.getDateTime())));
                    }
                }

                setDaysWithEvents();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void setDaysWithEvents() {
        if (eventDays.size() > 0) {
            List<DayDecorator> decorators = new ArrayList<>();
            decorators.add(new EventColorDecorator());
            calendarView.setDecorators(decorators);
            calendarView.refreshCalendar(currentCalendar);

        }

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_event, container, false);
        ButterKnife.bind(this, view);

        currentCalendar = Calendar.getInstance();

        setDaysWithEvents();

        calendarView.setCalendarListener(new CalendarListener() {
            @Override
            public void onDateSelected(Date date) {
                if (eventDays.contains(DateHelper.removeTimeFromDate(date))) {
                    Intent intent = new Intent(new Intent(getActivity(), EventsOnDayActivity.class));
                    startActivityForResult(intent, EVENT_DAY_REQUEST_CODE);
                }

            }

            @Override
            public void onMonthChanged(Date date) {
            }
        });
        return view;
    }

    @OnClick(R.id.btn_add_event)
    public void showAddEventForm() {
        startActivityForResult(new Intent(getActivity(), NewEventActivity.class), EVENTS_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    class EventColorDecorator implements DayDecorator {
        private long day;

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void decorate(DayView dayView) {
            for (Date eventDay : eventDays) {
                if (DateHelper.removeTimeFromDate(dayView.getDate()).equals(eventDay)) {
                    dayView.setTextColor(Color.WHITE);
                    dayView.setBackgroundColor(getActivity().getColor(R.color.colorAccent));
                }

                if (DateHelper.isToday(dayView.getDate().getTime())) {
                    dayView.setPaintFlags(dayView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
                    dayView.setTextColor(Color.WHITE);

                }
            }

        }

    }


}
