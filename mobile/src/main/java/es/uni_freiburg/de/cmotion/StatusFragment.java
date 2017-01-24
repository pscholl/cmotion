package es.uni_freiburg.de.cmotion;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import de.uni_freiburg.es.sensorrecordingtool.RecorderStatus;
import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.AutoDiscovery;
import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.Node;
import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.NodeStatus;
import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.OnNodeSensorsDiscoveredListener;
import es.uni_freiburg.de.cmotion.adapter.NodeAdapter;

import static de.uni_freiburg.es.sensorrecordingtool.RecorderCommands.getBooleanOrString;

public class StatusFragment extends Fragment {

    private RecyclerView mRecyclerView;
    private NodeAdapter mRecyclerViewAdapter;

    private HashMap<String, Node> mNodeList = new HashMap<>();

    private TextView mStatusTextView;

    private OnNodeSensorsDiscoveredListener mListener = new OnNodeSensorsDiscoveredListener() {

        @Override
        public void onNodeSensorsDiscovered(Node node, String[] availableSensors) {
            invalidateData();
        }
    };

    private void invalidateData() {
        ArrayList<Node> set = mAutoDiscovery.getDiscoveredSensors();
        for (Node n : set) {
            if (mNodeList.containsKey(n.getAid())) { // do right intersect
                n.setNodeStatus(mNodeList.get(n.getAid()).getNodeStatus());
                n.setReadySensors(mNodeList.get(n.getAid()).getReadySensors());
                n.setDrift(mNodeList.get(n.getAid()).getDrift());
            }
        }
        mRecyclerViewAdapter.setData(set);
        mStatusTextView.setText(mAutoDiscovery.getConnectedNodes()+" recorder(s) available");
    }

    private AutoDiscovery mAutoDiscovery;
    private BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !intent.getAction().equals(RecorderStatus.STATUS_ACTION))
                return;

            NodeStatus state = NodeStatus.valueOf(intent.getStringExtra(RecorderStatus.STATE));

//            if (state == NodeStatus.RECORDING)
//                mNodeList.clear();

            String nodeId = intent.getStringExtra(RecorderStatus.ANDROID_ID);
            String nodePlatform = intent.getStringExtra(RecorderStatus.PLATFORM);


            if (!mNodeList.containsKey(nodeId)) {
                Node node = new Node(nodePlatform, nodeId);
                mNodeList.put(nodeId, node);
            }

            mNodeList.get(nodeId).setNodeStatus(state);

            switch (state) {
                case READY:
                    String[] sensors = intent.getStringArrayExtra(RecorderStatus.SENSORS);
                    long drift = Double.valueOf(intent.getDoubleExtra(RecorderStatus.DRIFT, Long.MAX_VALUE)).longValue();

                    if(!getBooleanOrString(intent, RecorderStatus.DRIFT_VALID, false))
                        drift = Long.MAX_VALUE;

                    mNodeList.get(nodeId).setReadySensors(sensors);
                    mNodeList.get(nodeId).setDrift(drift);
                    break;
                default:
                    break;
            }

            invalidateData();
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.content_status, container, false);
    }

    @Override
    public void onActivityCreated(Bundle b) {
        super.onActivityCreated(b);

        mStatusTextView = (TextView) getActivity().findViewById(R.id.statusTextView);

        mAutoDiscovery = AutoDiscovery.getInstance(getActivity());
        mRecyclerView = (RecyclerView) getActivity().findViewById(R.id.nodeRecycler);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerViewAdapter = new NodeAdapter(getActivity(), mAutoDiscovery.getDiscoveredSensors());
        mRecyclerView.setAdapter(mRecyclerViewAdapter);
        mAutoDiscovery.setListener(mListener);

        getActivity().registerReceiver(mStatusReceiver, new StatusIntentFilter());

        mStatusTextView.setText(mAutoDiscovery.getConnectedNodes()+" recorder(s) available");

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(mStatusReceiver);

    }
}
