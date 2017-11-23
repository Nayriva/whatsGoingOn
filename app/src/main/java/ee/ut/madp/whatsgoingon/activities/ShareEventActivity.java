package ee.ut.madp.whatsgoingon.activities;

import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import ee.ut.madp.whatsgoingon.ApplicationClass;
import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.adapters.GroupParticipantsAdapter;
import ee.ut.madp.whatsgoingon.comparators.GroupParticipantComparator;
import ee.ut.madp.whatsgoingon.models.ChatChannel;
import ee.ut.madp.whatsgoingon.models.GroupParticipant;

public class ShareEventActivity extends AppCompatActivity {

    @BindView(R.id.list_participants)
    ListView participantsList;
    @BindView(R.id.tv_no_users)
    TextView noUsersTv;

    GroupParticipantsAdapter participantsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_event);
        ButterKnife.bind(this);
        setTitle(getString(R.string.choose_participants));
        setupListView();
    }

    private void setupListView() {
        List<GroupParticipant> participants = new ArrayList<>();
        for (ChatChannel tmp : ((ApplicationClass) getApplication()).getChannels()) {
            if (!((ApplicationClass) getApplication()).isGroup(tmp.getId())) {
                participants.add(new GroupParticipant(tmp.getId(), tmp.getName(), tmp.getPhoto(), false));
            }
        }
        Collections.sort(participants, new GroupParticipantComparator());

        participantsAdapter = new GroupParticipantsAdapter(this,
                R.layout.dialog_group_list_item, participants);
        participantsList.setAdapter(participantsAdapter);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.users_share_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        SearchView searchView = null;
        if (searchItem != null) {
            searchView = (SearchView) searchItem.getActionView();
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    participantsAdapter.getFilter().filter(query);
                    if (participantsAdapter.getCount() == 0) noUsersTv.setVisibility(View.VISIBLE);
                    else noUsersTv.setVisibility(View.GONE);
                    return false;
                }
                @Override
                public boolean onQueryTextChange(String s) {
                    participantsAdapter.getFilter().filter(s);

                    return false;
                }
            });
        }

        return super.onCreateOptionsMenu(menu);
    }
    
}
