package ee.ut.madp.whatsgoingon.adapters;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Filter;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import ee.ut.madp.whatsgoingon.R;
import ee.ut.madp.whatsgoingon.helpers.ImageHelper;
import ee.ut.madp.whatsgoingon.models.GroupParticipant;

/**
 * Created by dominikf on 3. 10. 2017.
 */

public class GroupParticipantsAdapter extends ArrayAdapter<GroupParticipant> {
    private Context context;
    private int layoutResourceId;
    private List<GroupParticipant> data = null;
    private List<GroupParticipant> origData = null;

    public GroupParticipantsAdapter(Context context, int resource, List<GroupParticipant> objects) {
        super(context, resource, objects);
        this.layoutResourceId = resource;
        this.context = context;
        this.data = objects;
        this.origData = new ArrayList<>(this.data);
    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
        View row = convertView;
        ChatChannelHolder holder;

        if(row == null) {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new ChatChannelHolder();
            holder.photo = (CircleImageView) row.findViewById(R.id.ciw_group_participant_picture);
            holder.channelName = (TextView) row.findViewById(R.id.tw_group_participant_name);
            holder.isSelected = (CheckBox) row.findViewById(R.id.chb_group_participant);

            holder.isSelected.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    data.get(position).setSelected(b);
                }
            });

            row.setTag(holder);
        } else {
            holder = (ChatChannelHolder) row.getTag();
        }

        GroupParticipant item = data.get(position);
        String photo = item.getPhoto();
        if (photo != null) {
            if (photo.contains("http")) {
                Picasso.with(getContext()).load(photo).into(holder.photo);
            } else {
                holder.photo.setImageBitmap(ImageHelper.decodeBitmap(photo));
            }
        }
        holder.channelName.setText(item.getName());
        holder.isSelected.setChecked(item.isSelected());

        return row;
    }

    private class ChatChannelHolder {
        CircleImageView photo;
        TextView channelName;
        CheckBox isSelected;
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    public List<GroupParticipant> getItems() {
        return data;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                constraint = constraint.toString().toLowerCase();
                FilterResults result = new FilterResults();

                if (constraint != null) {
                    List<GroupParticipant> found = new ArrayList<>();
                    for (GroupParticipant item : origData) {
                        if (item.getName().toLowerCase().contains(constraint)) {
                            found.add(item);
                        }
                    }

                    result.values = found;
                    result.count = found.size();
                } else {
                    result.values = origData;
                    result.count = origData.size();
                }
                return result;


            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                clear();
                for (GroupParticipant item : (List<GroupParticipant>) results.values) {
                    add(item);
                }
                notifyDataSetChanged();

            }

        };
    }
}
