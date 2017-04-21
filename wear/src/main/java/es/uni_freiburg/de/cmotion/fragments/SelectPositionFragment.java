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

import de.uni_freiburg.es.sensorrecordingtool.WearPositionManager;
import es.uni_freiburg.de.cmotion.HeadWearableListView;
import es.uni_freiburg.de.cmotion.R;

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
    private MyAdapter mAdapter;


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);
        mListView = (HeadWearableListView<TextView>) getActivity().findViewById(R.id.headWearableListView);

        TextView textView = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.header_select_position_list, null);
        mListView.setHeader(textView);

        mAdapter = new MyAdapter(getContext(), WearPositionManager.Position.values());
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

    class MyAdapter extends WearableListView.Adapter {
        private final LayoutInflater mInflater;
        private final WearPositionManager.Position[] mPositions;
        private WearPositionManager.Position mActivePosition;

        private MyAdapter(Context context, WearPositionManager.Position[] positions) {
            mInflater = LayoutInflater.from(context);
            mPositions = positions;
        }

        public void setActive(WearPositionManager.Position position) {
            mActivePosition = position;
            notifyDataSetChanged();
        }

        @Override
        public WearableListView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new WearableListView.ViewHolder(
                    mInflater.inflate(R.layout.row_simple_item_layout, null));
        }

        @Override
        public void onBindViewHolder(WearableListView.ViewHolder holder, int position) {
            TextView view = (TextView) holder.itemView.findViewById(R.id.textView);
            RadioButton radioButton = (RadioButton) holder.itemView.findViewById(R.id.radioButton);
            WearPositionManager.Position item = mPositions[position];
            view.setText(toCamelCase(item.name().replaceAll("_", " ")));

//            view.setTypeface(null, item.equals(mActivePosition) ? Typeface.BOLD : Typeface.NORMAL);
            radioButton.setChecked(item.equals(mActivePosition));


            holder.itemView.setTag(position);
        }

        private String toCamelCase(String in) {
            in = in.toLowerCase().trim();
            String[] a = in.split(" ");

            StringBuilder builder = new StringBuilder();

            for (int i = 0; i < a.length; i++) {
                String s = a[i];

                if (s == null || s.length() == 0)
                    continue;

                s = String.valueOf(s.charAt(0)).toUpperCase() + s.substring(1);
                builder.append(s);
                if (i != a.length - 1)
                    builder.append(" ");
            }

            return builder.toString();
        }

        @Override
        public int getItemCount() {
            return mPositions.length;
        }

        public WearPositionManager.Position getItem(int i) {
            return mPositions[i];
        }
    }
}
