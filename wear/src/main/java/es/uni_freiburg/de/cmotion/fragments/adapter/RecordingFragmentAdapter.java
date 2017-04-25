package es.uni_freiburg.de.cmotion.fragments.adapter;

import android.content.Context;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.uni_freiburg.es.sensorrecordingtool.WearPositionManager;
import es.uni_freiburg.de.cmotion.R;
import es.uni_freiburg.de.cmotion.shared_ui.adapter.AbstractDataAdapter;
import es.uni_freiburg.de.cmotion.shared_ui.model.SensorModel;

public class RecordingFragmentAdapter extends WearableListView.Adapter implements AbstractDataAdapter<SensorModel> {
    private final LayoutInflater mInflater;
    private WearPositionManager.Position mActivePosition;
    private List<SensorModel> mData = new ArrayList<>();

    public RecordingFragmentAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
    }

    public List<SensorModel> getSelectedItems() {
        ArrayList<SensorModel> mList = new ArrayList<>();
        for (SensorModel model : mData)
            if (model.isEnabled())
                mList.add(model);
        return mList;
    }

    public void toggle(int i) {
        mData.get(i).setEnabled(!mData.get(i).isEnabled());
        notifyDataSetChanged();
    }

    @Override
    public WearableListView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new WearableListView.ViewHolder(
                mInflater.inflate(R.layout.row_node_item_layout, null));
    }

    @Override
    public void onBindViewHolder(WearableListView.ViewHolder holder, int position) {

        final SensorModel model = mData.get(position);
        String name = model.getName().contains(".") ?
                model.getName().substring(model.getName().lastIndexOf(".") + 1).replace("_", " ") :
                model.getName();

        TextView nameTextView = (TextView) holder.itemView.findViewById(R.id.nameTextView);
        TextView platformTextView = (TextView) holder.itemView.findViewById(R.id.platformTextView);
        RadioButton radioButton = (RadioButton) holder.itemView.findViewById(R.id.radioButton);

        nameTextView.setText(name);
        platformTextView.setText(model.getAvailablePlatforms().toString());
        radioButton.setChecked(model.isEnabled());

    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public SensorModel getItem(int i) {
        return mData.get(i);
    }

    @Override
    public void setData(List<SensorModel> collection) {
        mData = collection;
        notifyDataSetChanged();
    }
}