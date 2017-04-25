package es.uni_freiburg.de.cmotion.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import es.uni_freiburg.de.cmotion.HeadWearableListView;
import es.uni_freiburg.de.cmotion.R;
import es.uni_freiburg.de.cmotion.fragments.adapter.RecordingFragmentAdapter;
import es.uni_freiburg.de.cmotion.shared_ui.AutoDiscoveryWrapper;
import es.uni_freiburg.de.cmotion.shared_ui.CMotionBroadcastReceiver;
import es.uni_freiburg.de.cmotion.shared_ui.RecordFloatingActionButton;
import es.uni_freiburg.de.cmotion.shared_ui.RecordingIntentFilter;
import es.uni_freiburg.de.cmotion.shared_ui.SRTHelper;
import es.uni_freiburg.de.cmotion.shared_ui.TimedProgressBar;

public class RecordingFragment extends Fragment implements WearableListView.ClickListener {

    private RecordFloatingActionButton mRecFab;
    private HeadWearableListView<View> mListView;
    private RecordingFragmentAdapter mAdapter;
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
        mAdapter = new RecordingFragmentAdapter(getContext());
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

}
