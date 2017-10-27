package ee.ut.madp.whatsgoingon.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import butterknife.ButterKnife;
import butterknife.OnClick;
import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.activities.NewEventActivity;

import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.EVENTS_REQUEST_CODE;

public class EventFragment extends Fragment {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_event, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @OnClick(R.id.btn_add_event)
    public void showAddEventForm() {
        startActivityForResult(new Intent(getActivity(), NewEventActivity.class), EVENTS_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EVENTS_REQUEST_CODE) {
            //setUpInitialFragment("Events");
        }
    }
}
