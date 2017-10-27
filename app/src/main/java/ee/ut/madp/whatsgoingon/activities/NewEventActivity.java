package ee.ut.madp.whatsgoingon.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.mobsandgeeks.saripaar.annotation.NotEmpty;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ee.ut.madp.whatsgoingon.R;

public class NewEventActivity extends AppCompatActivity {

    @NotEmpty
    @BindView(R.id.input_layout_eventname)
    TextInputLayout eventName;
    @NotEmpty
    @BindView(R.id.input_layout_date)
    TextInputLayout date;
    @NotEmpty
    @BindView(R.id.input_layout_time) TextInputLayout time;


    @BindView(R.id.input_eventname)
    TextInputEditText eventNameInput;
    @BindView(R.id.input_date)
    TextInputEditText dateInput;
    @BindView(R.id.input_time) TextInputEditText timeInput;
    @BindView(R.id.input_description) TextInputEditText descriptionInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_event);
        ButterKnife.bind(this);
    }

    //
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            Intent data = new Intent();
            setResult(Activity.RESULT_OK, data);
            finish();
        }
        return true;
    }

    @OnClick(R.id.btn_add_event)
    public void createNewEvent() {

    }


}
