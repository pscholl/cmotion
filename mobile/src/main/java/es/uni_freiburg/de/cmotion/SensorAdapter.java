package es.uni_freiburg.de.cmotion;


import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import es.uni_freiburg.de.cmotion.model.SensorModel;
import es.uni_freiburg.de.cmotion.ui.DigitEditDialog;

public class SensorAdapter extends RecyclerView.Adapter<SensorAdapter.ViewHolder> {

    private final Context context;
    private List<SensorModel> mCollection = null;
    private DigitEditDialog.OnTextChangedListener mEditTextListener = new DigitEditDialog.OnTextChangedListener() {
        @Override
        public void onTextChanged(Object tag, String newText) {
            int pos = (int) tag;
            mCollection.get(pos).setSamplingRate(Integer.parseInt(newText));
            notifyItemChanged(pos);
        }
    };

    public SensorAdapter(Context context, List<SensorModel> data) {
        this.context = context; setData(data);
    }

    public void setData(List<SensorModel> data) {
        this.mCollection = data;
        notifyDataSetChanged();
    }

    /**
     *
     * @return All selected SensorModels or an empty list. NullSafe!.
     */
    public List<SensorModel> getSelectedItems() {
        ArrayList<SensorModel> selected = new ArrayList<>();

        for (SensorModel model : mCollection)
            if (model.isEnabled())
                selected.add(model);

        return selected;
    }

    /**
     *
     * @return Amount of selected items or 0. NullSafe!
     */
    public int getSelectedItemsSize() {
        return getSelectedItems().size();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View contactView = inflater.inflate(R.layout.recyclerview_sensor, parent, false);

        // Return a new holder instance
        ViewHolder viewHolder = new ViewHolder(contactView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final SensorModel model = mCollection.get(position);
        holder.nameTextView.setText(model.getName());
        holder.checkBox.setChecked(model.isEnabled());
        holder.samplingRateButton.setText(model.getSamplingRate()+" Hz");

        holder.samplingRateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DigitEditDialog.build(context, "Enter Sampling Rate", model.getSamplingRate()+"", position, mEditTextListener).show();
            }
        });


        holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                model.setEnabled(isChecked);
            }
        });
    }


    @Override
    public int getItemCount() {
        if (mCollection == null)
            return 0;
        else return mCollection.size();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView nameTextView;
        public CheckBox checkBox;
        public Button samplingRateButton;

        public ViewHolder(View itemView) {
            super(itemView);

            samplingRateButton = (Button) itemView.findViewById(R.id.samplingRateButton);
            nameTextView = (TextView) itemView.findViewById(R.id.nameTextView);
            checkBox = (CheckBox) itemView.findViewById(R.id.checkBox);

        }
    }
}
