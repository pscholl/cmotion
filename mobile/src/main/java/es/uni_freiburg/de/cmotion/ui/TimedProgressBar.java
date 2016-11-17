package es.uni_freiburg.de.cmotion.ui;


import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ProgressBar;

/**
 * Progressbar that progresses with time.
 */
public class TimedProgressBar extends ProgressBar {

    private static final long TIMESTEP = 100; // time in ms
    private Handler animateHandler = new Handler();

    public TimedProgressBar(Context context) {
        super(context);
        init();
    }

    public TimedProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TimedProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public TimedProgressBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
    }

    /**
     * Stops the animation and resets the Progressbar. Also renders it invisible.
     */
    public void stopAnimation() {
        animateHandler.removeCallbacksAndMessages(null);
        setIndeterminate(false);
        setProgress(0);
    }

    /**
     * Starts an animation for a specific time defined by the parameter. Will use intermediate state
     * when time is set to -1
     *
     * @param seconds
     */
    public void startAnimation(int seconds) {

        if (seconds == -1) {
            setIndeterminate(true);
            return;
        }

        stopAnimation();
        setMax(seconds * 1000);
        animationStep(0, (int) ((seconds * 1000f) / TIMESTEP));
    }


    private void animationStep(final int progress, final int maxStep) {
        if (progress == maxStep) {
            setProgress(getMax());
            Log.e("progress", "done");
            return;
        }

        setProgress((int) ((progress / (float) maxStep) * getMax()));

        animateHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                animationStep(progress + 1, maxStep);
            }
        }, TIMESTEP);

    }
}