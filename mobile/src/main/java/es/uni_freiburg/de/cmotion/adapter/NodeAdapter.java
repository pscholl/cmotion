package es.uni_freiburg.de.cmotion.adapter;


import android.content.Context;
import android.graphics.Typeface;
import android.provider.Settings;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Collection;

import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.Node;
import es.uni_freiburg.de.cmotion.R;

public class NodeAdapter extends RecyclerView.Adapter<NodeAdapter.ViewHolder> {

    private final Context context;
    private Collection<Node> mCollection = null;


    public NodeAdapter(Context context, Collection<Node> data) {
        this.context = context;
        setData(data);
    }

    public void setData(Collection<Node> data) {
        this.mCollection = data;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View contactView = inflater.inflate(R.layout.recyclerview_node, parent, false);

        // Return a new holder instance
        ViewHolder viewHolder = new ViewHolder(contactView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final Node node = mCollection.toArray(new Node[mCollection.size()])[position];

        boolean thisDevice = node.getAid().equals(Settings.Secure.getString(holder.context.getContentResolver(),
                Settings.Secure.ANDROID_ID));
        holder.nodeNameTextView.setTypeface(null, !thisDevice ? Typeface.NORMAL : Typeface.BOLD);

        holder.nodeNameTextView.setText(String.format("%s[%s]", node.getPlatform(), node.getAid()));
        holder.nodeStateTextView.setText(node.getNodeStatus().toString());

        if(node.getDrift() == Long.MIN_VALUE)
            holder.nodeDriftTextView.setText("drift not determined");
        else if(node.getDrift() == Long.MAX_VALUE)
            holder.nodeDriftTextView.setText("drift computation failed");
        else
            holder.nodeDriftTextView.setText(node.getDrift() + "ms drift");

        holder.nodeTechTextView.setText(Arrays.toString(node.getConnectionTechnologies()));


        holder.nodeListTextView.setText("");
        if (node.getReadySensors() != null)
            for (String s : node.getReadySensors())
                holder.nodeListTextView.append(s + "\n");
    }


    @Override
    public int getItemCount() {
        if (mCollection == null)
            return 0;
        else return mCollection.size();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ArrayAdapter arrayAdapter;
        public TextView nodeNameTextView, nodeStateTextView, nodeListTextView, nodeDriftTextView, nodeTechTextView;
        public ListView nodeSensorListView;
        public Context context;

        public ViewHolder(View itemView) {
            super(itemView);

            context = itemView.getContext();

            arrayAdapter = new ArrayAdapter(itemView.getContext(), android.R.layout.simple_list_item_1, android.R.id.text1);


            nodeNameTextView = (TextView) itemView.findViewById(R.id.nodeNameTextView);
            nodeStateTextView = (TextView) itemView.findViewById(R.id.nodeStateTextView);
            nodeStateTextView = (TextView) itemView.findViewById(R.id.nodeStateTextView);
            nodeListTextView = (TextView) itemView.findViewById(R.id.nodeListTextView);
            nodeDriftTextView = (TextView) itemView.findViewById(R.id.nodeDriftTextView);
            nodeTechTextView = (TextView) itemView.findViewById(R.id.nodeTechnologyTextView);
            nodeSensorListView = (ListView) itemView.findViewById(R.id.nodeSensorListView);

            nodeSensorListView.setAdapter(arrayAdapter);

        }
    }
}
