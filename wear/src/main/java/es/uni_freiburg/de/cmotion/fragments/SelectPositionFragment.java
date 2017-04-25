package es.uni_freiburg.de.cmotion.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.uni_freiburg.es.sensorrecordingtool.WearPositionManager;
import es.uni_freiburg.de.cmotion.HeadWearableListView;
import es.uni_freiburg.de.cmotion.R;
import es.uni_freiburg.de.cmotion.fragments.adapter.SelectPositionFragmentAdapter;

/**
 * A "preferences" class to define the position of the watch, by selecting an element from a list.
 */
public class SelectPositionFragment extends Fragment implements WearableListView.ClickListener {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.fragment_select_position, container, false);
        return rootView;
    }


    private HeadWearableListView<TextView> mListView;
    private SelectPositionFragmentAdapter mAdapter;


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);
        mListView = (HeadWearableListView<TextView>) getActivity().findViewById(R.id.headWearableListView);

        TextView textView = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.header_select_position_list, null);
        mListView.setHeader(textView);

        mAdapter = new SelectPositionFragmentAdapter(getContext(), WearPositionManager.Position.values());
        mAdapter.setActive(WearPositionManager.getPosition(getContext()));
        mListView.getWearableListView().setAdapter(mAdapter);
        mListView.getWearableListView().setClickListener(this);
    }

    @Override
    public void onClick(WearableListView.ViewHolder viewHolder) {
        int i = viewHolder.getAdapterPosition();
        WearPositionManager.Position position = mAdapter.getItem(i);
        WearPositionManager.setPosition(getContext(), position);
        mAdapter.setActive(WearPositionManager.getPosition(getContext()));
    }

    @Override
    public void onTopEmptyRegionClick() {

    }
}
