package es.uni_freiburg.de.cmotion.shared_ui;

import android.content.Context;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;

/**
 * A Floating button with recording state, changes icon according to state.
 */
public class RecordFloatingActionButton extends FloatingActionButton {
    private boolean mRecording = false;

    final float animFrom = 1f, animTo = 0.75f;
    ScaleAnimation anim = new ScaleAnimation(animFrom, animTo, animFrom, animTo, Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f);


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

        anim.setRepeatMode(Animation.INFINITE);
        anim.setRepeatCount(-1);

        anim.setFillAfter(false); // revert to normal
        anim.setDuration(500);
    }

    public void setRecording(boolean recording) {
        this.mRecording = recording;
        setImageResource(!mRecording ? R.drawable.ic_fiber_manual_record_white_24dp : R.drawable.ic_stop_white_24dp);
    }

    public boolean isRecording() {
        return mRecording;
    }

    public void setFreeze(boolean b) {
        if (b) {
            setRecording(true);
            startAnimation(anim);
        } else
            anim.cancel();

//        setEnabled(!b);
    }
}
