package es.uni_freiburg.de.cmotion.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.uni_freiburg.es.sensorrecordingtool.WearPositionManager;
import es.uni_freiburg.de.cmotion.HeadWearableListView;
import es.uni_freiburg.de.cmotion.R;
import es.uni_freiburg.de.cmotion.shared_ui.AutoDiscoveryWrapper;
import es.uni_freiburg.de.cmotion.shared_ui.CMotionBroadcastReceiver;
import es.uni_freiburg.de.cmotion.shared_ui.RecordFloatingActionButton;
import es.uni_freiburg.de.cmotion.shared_ui.RecordingIntentFilter;
import es.uni_freiburg.de.cmotion.shared_ui.SRTHelper;
import es.uni_freiburg.de.cmotion.shared_ui.TimedProgressBar;
import es.uni_freiburg.de.cmotion.shared_ui.adapter.AbstractDataAdapter;
import es.uni_freiburg.de.cmotion.shared_ui.model.SensorModel;

public class RecordingFragment extends Fragment implements WearableListView.ClickListener {

    private RecordFloatingActionButton mRecFab;
    private HeadWearableListView<View> mListView;
    private MyAdapter mAdapter;
    private AutoDiscoveryWrapper mAutoDiscoveryWrapper;
    private CMotionBroadcastReceiver cMotionBroadcastReceiver;
    private TimedProgressBar mTimedProgress;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.fragment_recording, container, false);
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);
        mListView = (HeadWearableListView<View>) getActivity().findViewById(R.id.headWearableListViewRec);
        View header = LayoutInflater.from(getContext()).inflate(R.layout.header_recordinglist, null);
        mListView.setHeader(header);
        mAdapter = new MyAdapter(getContext());
        mListView.getWearableListView().setAdapter(mAdapter);
        mListView.getWearableListView().setClickListener(this);

        mAutoDiscoveryWrapper = new AutoDiscoveryWrapper(getContext(), mAdapter);

        mRecFab = (RecordFloatingActionButton) getActivity().findViewById(R.id.recordingFab);

        mRecFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    if (mRecFab.isRecording()) {
                        SRTHelper.sendCancelIntent(getContext());
                    } else {
//                        mRecyclerViewAdapter.setFrozen(true);
                        mRecFab.setFreeze(true);
                        SRTHelper.sendRecordIntent(getContext(), mAdapter.getSelectedItems());
                    }

            }
        });

        mTimedProgress = (TimedProgressBar) getActivity().findViewById(R.id.timedProgressBar);
        cMotionBroadcastReceiver = new CMotionBroadcastReceiver(getActivity(), mTimedProgress, mRecFab, null);
        getActivity().registerReceiver(cMotionBroadcastReceiver, new RecordingIntentFilter());
    }

    @Override
    public void onResume() {
        super.onResume();
        mAutoDiscoveryWrapper.refresh();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mAutoDiscoveryWrapper.close();
        getActivity().unregisterReceiver(cMotionBroadcastReceiver);
    }

    @Override
    public void onClick(WearableListView.ViewHolder viewHolder) {
        int i = viewHolder.getAdapterPosition();
        mAdapter.toggle(i);
        SRTHelper.persistCheckedSensors(getContext(), mAdapter.getSelectedItems());
    }

    @Override
    public void onTopEmptyRegionClick() {
        mAutoDiscoveryWrapper.refresh();

    }

    class MyAdapter extends WearableListView.Adapter implements AbstractDataAdapter<SensorModel> {
        private final LayoutInflater mInflater;
        private WearPositionManager.Position mActivePosition;
        private List<SensorModel> mData = new ArrayList<>();

        private MyAdapter(Context context) {
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
}
