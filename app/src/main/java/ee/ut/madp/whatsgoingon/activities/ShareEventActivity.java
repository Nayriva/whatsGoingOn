package ee.ut.madp.whatsgoingon.activities;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ee.ut.madp.whatsgoingon.ApplicationClass;
import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.adapters.GroupParticipantsAdapter;
import ee.ut.madp.whatsgoingon.comparators.GroupParticipantComparator;
import ee.ut.madp.whatsgoingon.helpers.ChatHelper;
import ee.ut.madp.whatsgoingon.models.ChatChannel;
import ee.ut.madp.whatsgoingon.models.GroupParticipant;

import static ee.ut.madp.whatsgoingon.constants.GeneralConstants.EXTRA_EVENT_ID;

public class ShareEventActivity extends AppCompatActivity {

    private static final String TAG = ShareEventActivity.class.getSimpleName();
    @BindView(R.id.list_participants)
    ListView participantsList;
    @BindView(R.id.tv_no_users)
    TextView noUsersTv;

    GroupParticipantsAdapter participantsAdapter;
    List<GroupParticipant> pickedParticipants = new ArrayList<>();
    private ApplicationClass application;
    private String encodedMessage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_event);
        ButterKnife.bind(this);
        setTitle(getString(R.string.choose_participants));
        setupListView();

        if (getIntent().hasExtra(android.content.Intent.EXTRA_SUBJECT) && getIntent().hasExtra(Intent.EXTRA_TEXT)
                && getIntent().hasExtra(EXTRA_EVENT_ID)) {

            encodedMessage = ChatHelper.encodeEventMessage(getIntent().getStringExtra(EXTRA_EVENT_ID),
                    getIntent().getStringExtra(android.content.Intent.EXTRA_SUBJECT),
                    getIntent().getStringExtra(android.content.Intent.EXTRA_TEXT));
        }

        application = (ApplicationClass) getApplication();
    }

    @OnClick(R.id.btn_share_event)
    public void shareEvent() {
        List<ChatChannel> receivers = new ArrayList<>();
        for (GroupParticipant groupParticipant : participantsAdapter.getPickedParticipants()) {
            receivers.add(application.getChannel(groupParticipant.getId()));
        }
        new ShareEventActivity.ShareEventAsyncTask(application.getLoggedUser().getId(),
                application.getLoggedUser().getName(), receivers).execute();

    }


    private void setupListView() {
        List<GroupParticipant> participants = new ArrayList<>();
        for (ChatChannel tmp : ((ApplicationClass) getApplication()).getChannels()) {
            participants.add(new GroupParticipant(tmp.getId(), tmp.getName(), tmp.getPhoto(), false));
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

    private class ShareEventAsyncTask extends AsyncTask<Uri, Void, Void> {

        private String sender;
        private String displayName;
        private boolean isGroup;
        private List<ChatChannel> receivers;
        private String[] groupReceivers;
        private String channelId;

        public ShareEventAsyncTask(String sender, String displayName, List<ChatChannel> receivers) {
            this.sender = sender;
            this.displayName = displayName;
            this.receivers = receivers;
        }

        @Override
        protected Void doInBackground(Uri... uris) {
            if (uris != null) {
                try {
                    for (ChatChannel receiver : receivers) {
                        isGroup = receiver.isGroup();
                        channelId = receiver.getId();
                        if (isGroup)
                            groupReceivers = application.getGroupReceivers(receiver.getId());
                        sendMessage(ChatHelper.eventText(encodedMessage));
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        private void sendMessage(String text) {
            Log.i(TAG, "SendTakenPhotoAsyncTask.sendMessage");
            String message;
            if (isGroup) {
                message = ChatHelper.groupMessage(sender, displayName, channelId, groupReceivers, text);
            } else {
                message = ChatHelper.oneToOneMessage(sender, displayName, channelId, text);
            }
            application.sendChatMessage(message);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Toast.makeText(ShareEventActivity.this, getString(R.string.succes_message_shared_event), Toast.LENGTH_SHORT).show();
        }
    }

}
