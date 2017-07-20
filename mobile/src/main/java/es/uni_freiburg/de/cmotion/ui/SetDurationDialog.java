package es.uni_freiburg.de.cmotion.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import es.uni_freiburg.de.cmotion.R;


/**
 * A dialog for editing numbers.
 */
public class SetDurationDialog extends AlertDialog {


    private static final String TAG = SetDurationDialog.class.getSimpleName();

    public static AlertDialog build(final Context context, final String title, String value, final Object tag, final OnTextChangedListener listener) {

        LayoutInflater myLayout = LayoutInflater.from(context);
        final View dialogView = myLayout.inflate(R.layout.layout_dialog_duration, null);
        final EditText editText = (EditText) dialogView.findViewById(R.id.edit);
        editText.setText(value);

        final ViewGroup viewGroup = (ViewGroup) dialogView.findViewById(R.id.timeButtonHolder);

        for(int i=0; i<viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);
            child.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    editText.setText((String) v.getTag());
                }
            });
        }

        return new Builder(context)
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        if (listener != null && !TextUtils.isEmpty(editText.getText()))
                            listener.onTextChanged(tag, "" + Long.parseLong(editText.getText().toString()));
                        else
                            Log.w(TAG, "listener or text empty");
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
    }

    public SetDurationDialog(Context context) {
        super(context);

    }




}
