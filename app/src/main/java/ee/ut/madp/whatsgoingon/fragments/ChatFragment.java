package ee.ut.madp.whatsgoingon.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;

import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.chat.ChatApplication;
import ee.ut.madp.whatsgoingon.chat.ChatMessageAdapter;
import ee.ut.madp.whatsgoingon.helpers.ChatHelper;

public class ChatFragment extends Fragment {

    private ChatApplication application;
    private String channelDisplayName;
    private ListView messagesListView;
    private ChatMessageAdapter adapter;
    private EditText messageET;
    private FirebaseAuth firebaseAuth;
    private boolean isGroup;
    private String[] receivers;

    public ChatFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        application = (ChatApplication) getActivity().getApplication();
        firebaseAuth = FirebaseAuth.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        messagesListView = (ListView) view.findViewById(R.id.messagesListView);
        adapter = new ChatMessageAdapter(getContext(), R.layout.message_list_item, application.getHistory(channelDisplayName));
        messagesListView.setAdapter(adapter);

        messageET = (EditText) view.findViewById(R.id.messageEditText);
        FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.sendFloatingActionButton);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String messageText = String.valueOf(messageET.getText());
                String sender = firebaseAuth.getCurrentUser().getEmail();
                String message;
                if (isGroup) {
                    message = ChatHelper.groupMessage(sender, channelDisplayName, receivers, messageText);
                } else {
                    message = ChatHelper.oneToOneMessage(sender, channelDisplayName, messageText);
                }
                application.sendMessage(message);
            }
        });
        updateHistory();

        return view;
    }

    private void updateHistory() {
        application.getHistory(channelDisplayName);
    }

    public void setData(String channelDisplayName, boolean isGroup, String[] receivers) {
        this.channelDisplayName = channelDisplayName;

    }

}
