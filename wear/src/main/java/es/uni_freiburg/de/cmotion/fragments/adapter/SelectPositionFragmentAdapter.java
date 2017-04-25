package es.uni_freiburg.de.cmotion.fragments.adapter;

import android.content.Context;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import de.uni_freiburg.es.sensorrecordingtool.WearPositionManager;
import es.uni_freiburg.de.cmotion.R;

public class SelectPositionFragmentAdapter extends WearableListView.Adapter {
    private final LayoutInflater mInflater;
    private final WearPositionManager.Position[] mPositions;
    private WearPositionManager.Position mActivePosition;

    public SelectPositionFragmentAdapter(Context context, WearPositionManager.Position[] positions) {
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