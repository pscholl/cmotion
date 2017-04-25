package es.uni_freiburg.de.cmotion.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.AutoDiscovery;
import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.Node;
import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.OnNodeSensorsDiscoveredListener;
import es.uni_freiburg.de.cmotion.HeadWearableListView;
import es.uni_freiburg.de.cmotion.R;
import es.uni_freiburg.de.cmotion.fragments.adapter.NodeStateFragmentAdapter;

public class NodeStatusFragment extends Fragment implements OnNodeSensorsDiscoveredListener {

    private HeadWearableListView<TextView> mListView;
    private NodeStateFragmentAdapter mAdapter;
    private AutoDiscovery mAutoDiscovery;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.fragment_node_state, container, false);
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);
        mListView = (HeadWearableListView<TextView>) getActivity().findViewById(R.id.nodeStateHeadWearableListView);
        TextView textView = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.header_node_state_list, null);
        mListView.setHeader(textView);
        mAdapter = new NodeStateFragmentAdapter(getContext());
        mListView.getWearableListView().setAdapter(mAdapter);
        mAutoDiscovery = AutoDiscovery.getInstance(getContext());
        mAutoDiscovery.setListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mAutoDiscovery.discover();
    }


    @Override
    public void onNodeSensorsDiscovered(Node node, String[] availableSensors) {
        mAdapter.setData(mAutoDiscovery.getDiscoveredSensors());
    }
}
