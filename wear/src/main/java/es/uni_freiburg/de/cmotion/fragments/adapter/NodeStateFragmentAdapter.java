package es.uni_freiburg.de.cmotion.fragments.adapter;

import android.content.Context;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.Node;
import es.uni_freiburg.de.cmotion.R;
import es.uni_freiburg.de.cmotion.shared_ui.adapter.AbstractDataAdapter;

public class NodeStateFragmentAdapter extends WearableListView.Adapter implements AbstractDataAdapter<Node>{
    private final LayoutInflater mInflater;
    private List<Node> mData = new ArrayList<>();

    public NodeStateFragmentAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
    }


    @Override
    public WearableListView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new WearableListView.ViewHolder(
                mInflater.inflate(R.layout.row_node_state_layout, null));
    }

    @Override
    public void onBindViewHolder(WearableListView.ViewHolder holder, int position) {
        Node node = mData.get(position);

        String stateString;
        if(node.getDrift() == Long.MIN_VALUE)
            stateString = ("drift not determined");
        else if(node.getDrift() == Long.MAX_VALUE)
            stateString = ("drift computation failed");
        else
            stateString = (node.getDrift() + "ms drift");

        ((TextView) holder.itemView.findViewById(R.id.devicePlatformTextView)).setText(String.format("%s[%s]", node.getPlatform(), node.getAid()));
        ((TextView) holder.itemView.findViewById(R.id.deviceStateTextView)).setText(node.getNodeStatus().toString());

        holder.itemView.setTag(position);
    }


    @Override
    public int getItemCount() {
        return mData.size();
    }

    @Override
    public void setData(List<Node> collection) {
        mData = collection;
        notifyDataSetChanged();
    }
}