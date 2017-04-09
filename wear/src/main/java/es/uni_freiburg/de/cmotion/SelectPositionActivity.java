package es.uni_freiburg.de.cmotion;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import de.uni_freiburg.es.sensorrecordingtool.WearPositionManager;

public class SelectPositionActivity extends Activity implements WearableListView.ClickListener{

    private WearableListView mView;
    private MyAdapter mAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_position);
        mView = (WearableListView) findViewById(R.id.wearable_list);
        mAdapter = new MyAdapter(this, WearPositionManager.Position.values());
        mAdapter.setActive(WearPositionManager.getPosition(this));
        mView.setAdapter(mAdapter);
        mView.setClickListener(this);
    }

    @Override
    public void onClick(WearableListView.ViewHolder viewHolder) {
        int i = viewHolder.getAdapterPosition();
        WearPositionManager.Position position = mAdapter.getItem(i);
        WearPositionManager.setPosition(this, position);
        mAdapter.setActive(WearPositionManager.getPosition(this));
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
            WearPositionManager.Position item = mPositions[position];
            view.setText(item.name());
            if(item.equals(mActivePosition))
                view.setTypeface(null, Typeface.BOLD);
            else
                view.setTypeface(null, Typeface.NORMAL);


            holder.itemView.setTag(position);
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
