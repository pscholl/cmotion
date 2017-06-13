package es.uni_freiburg.de.cmotion.shared_ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import de.uni_freiburg.es.sensorrecordingtool.RecorderStatus;
import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.NodeStatus;
import de.uni_freiburg.es.sensorrecordingtool.merger.MergeStatus;


public class CMotionBroadcastReceiver extends BroadcastReceiver {

    private final Activity mActivity;
    private TimedProgressBar mProgressBar;
    private RecordFloatingActionButton mRecFab;
    private CoordinatorLayout mCoordinatorLayout;

    private OnRecordingStateChangedListener mListener;

    public CMotionBroadcastReceiver(Activity activity, TimedProgressBar timedProgressBar, RecordFloatingActionButton recFab, @Nullable CoordinatorLayout coordinatorLayout) {
        mActivity = activity;
        mRecFab = recFab;
        mProgressBar = timedProgressBar;
        mCoordinatorLayout = coordinatorLayout;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent == null)
            return;

        if (!Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID).equals(intent.getStringExtra(RecorderStatus.ANDROID_ID))) // block slave responses => might destroy logic
            return;

        switch (intent.getAction()) {
            case RecorderStatus.STATUS_ACTION:
                if (NodeStatus.valueOf(intent.getStringExtra(RecorderStatus.STATE)) == NodeStatus.RECORDING) {
                    long elapsed = intent.getLongExtra(RecorderStatus.STATUS_ELAPSED, -1);
                    long duration = intent.getLongExtra(RecorderStatus.STATUS_DURATION, -1);
                    startRecordingAnimations(elapsed, duration);
                }
                break;
            case RecorderStatus.ERROR_ACTION:
                notify("Error: " + intent.getStringExtra(RecorderStatus.ERROR_REASON));
                stopRecordingAnimations();
                break;
            case RecorderStatus.FINISH_ACTION:
                stopRecordingAnimations();
                break;
            case MergeStatus.FINISH_ACTION:
                if (intent.hasExtra(RecorderStatus.FINISH_PATH) && mCoordinatorLayout != null) {
                    final String path = intent.getStringExtra(RecorderStatus.FINISH_PATH);

                    if (isAutoplayEnabled(context))
                        openFileIfPossible(path);
                    else
                        Snackbar
                                .make(mCoordinatorLayout, "Written to: " + path, Snackbar.LENGTH_LONG)
                                .setAction("Open", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        openFileIfPossible(path);
                                    }
                                })
                                .show();
                }
                break;
            default:
        }
    }

    public static boolean isAutoplayEnabled(Context c) {
        return PreferenceManager
                .getDefaultSharedPreferences(c)
                .getBoolean("autoplay", true);
    }


    private void notify(String message) {
        if (mCoordinatorLayout != null) {
            Snackbar snackbar = Snackbar
                    .make(mCoordinatorLayout, message, Snackbar.LENGTH_INDEFINITE);

            /**
             * The following code is to "hack" the textview inside the SnackBar in order to show
             * more lines, this may not work for future API, since it is not an official way.
             * Thus null checks are mandatory.
             */
            View snackbarView = snackbar.getView();
            if (snackbar != null && snackbarView.findViewById(android.support.design.R.id.snackbar_text) != null) {
                TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
                textView.setTextColor(Color.RED);
                textView.setMovementMethod(new ScrollingMovementMethod());
                textView.setMaxLines(10);
            }

            snackbar.show();
        } else
            Toast.makeText(mActivity, message, Toast.LENGTH_LONG).show();
    }

    private void openFileIfPossible(String path) {
        Intent newIntent = new Intent(Intent.ACTION_VIEW);
        newIntent.setDataAndType(Uri.fromFile(new File(path)), "video/x-matroska");
        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            mActivity.startActivity(newIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(mActivity, "No handler for this type of file.", Toast.LENGTH_LONG).show();
        }
    }

    private void startRecordingAnimations(long elapsed, long duration) {
        if (mListener != null)
            mListener.onStateChanged(true);
        mRecFab.setFreeze(false);
        if (duration < 0)
            mProgressBar.startAnimation(-1);
        else {
            mProgressBar.stopAnimation();
            mProgressBar.setMax(100);
            mProgressBar.setProgress((int) ((elapsed / (double) duration) * mProgressBar.getMax()));
        }
        mRecFab.setRecording(true);
    }

    private void stopRecordingAnimations() {
        if (mListener != null)
            mListener.onStateChanged(false);
        mProgressBar.stopAnimation();
        mRecFab.setFreeze(false);
        mRecFab.setRecording(false);
    }

    public OnRecordingStateChangedListener getListener() {
        return mListener;
    }

    public void setListener(OnRecordingStateChangedListener listener) {
        this.mListener = listener;
    }

    public interface OnRecordingStateChangedListener {
        void onStateChanged(boolean isRecording);
    }

}
