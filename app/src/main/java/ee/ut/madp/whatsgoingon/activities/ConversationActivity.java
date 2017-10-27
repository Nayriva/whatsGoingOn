package ee.ut.madp.whatsgoingon.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.adapters.MessageAdapter;
import ee.ut.madp.whatsgoingon.models.ChatMessage;

public class ConversationActivity extends AppCompatActivity {

    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;

    private MessageAdapter messageAdapter;
    private List<ChatMessage> chatMessageList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);
        ButterKnife.bind(this);
        chatMessageList = new ArrayList<>();
//        chatMessageList.add(new ChatMessage("ja jsem rekla", "eiemsmeie", "eieeei", true));
//        chatMessageList.add(new ChatMessage("ty jsi rekl", "eiemsmeie", "eieeei", false));
//        chatMessageList.add(new ChatMessage("ja opet", "eiemsmeie", "eieeei", true));
//        chatMessageList.add(new ChatMessage("ty", "eiemsmeie", "eieeei", false));

        setupRecyclerView();
    }

    private void setupRecyclerView() {
        messageAdapter = new MessageAdapter(chatMessageList);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(messageAdapter);
    }
}
