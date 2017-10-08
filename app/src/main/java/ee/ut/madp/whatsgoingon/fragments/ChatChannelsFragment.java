package ee.ut.madp.whatsgoingon.fragments;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.List;

import butterknife.BindView;
import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.activities.LoginActivity;
import ee.ut.madp.whatsgoingon.activities.MainActivity;

public class ChatChannelsFragment extends Fragment {

    private static final String TAG = LoginActivity.class.getSimpleName();
    @BindView(R.id.channels_list_view) ListView channelsListView;
    public ChatChannelsFragment() {
        super();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat_channels, container, false);
    }
}
