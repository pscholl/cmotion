package es.uni_freiburg.de.cmotion.ui;

import android.content.Context;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;
import android.view.View;

import es.uni_freiburg.de.cmotion.R;

/**
 * A Floating button with recording state, changes icon according to state.
 */
public class RecordFloatingActionButton extends FloatingActionButton {
    private boolean mRecording = false;

    public RecordFloatingActionButton(Context context) {
        super(context);
        init();
    }

    public RecordFloatingActionButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RecordFloatingActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setImageResource(R.drawable.ic_fiber_manual_record_white_24dp);
    }

    public void setRecording(boolean recording) {
        this.mRecording = recording;
        setImageResource(!mRecording ? R.drawable.ic_fiber_manual_record_white_24dp : R.drawable.ic_stop_white_24dp);
    }

    public boolean isRecording() {
        return mRecording;
    }
}
