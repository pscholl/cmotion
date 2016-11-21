package es.uni_freiburg.de.cmotion;

import android.view.View;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.Toast;

import java.io.File;

import de.uni_freiburg.es.sensorrecordingtool.RecorderStatus;
import es.uni_freiburg.de.cmotion.ui.RecordFloatingActionButton;
import es.uni_freiburg.de.cmotion.ui.TimedProgressBar;

public class CMotionBroadcastReceiver extends BroadcastReceiver {

    private final Activity mActivity;
    private TimedProgressBar mProgressBar;
    private RecordFloatingActionButton mRecFab;
    private CoordinatorLayout mCoordinatorLayout;

    public CMotionBroadcastReceiver(Activity activity) {
        mProgressBar = (TimedProgressBar) activity.findViewById(R.id.progressBar);
        mRecFab = (RecordFloatingActionButton) activity.findViewById(R.id.fab);
        mCoordinatorLayout = (CoordinatorLayout) activity.findViewById(R.id.coordinatorLayout);
        mActivity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case RecorderStatus.STATUS_ACTION:
                long duration = intent.getLongExtra(RecorderStatus.STATUS_DURATION, -1);
                startRecordingAnimations(duration);
                break;
            case RecorderStatus.ERROR_ACTION:
                Snackbar
                        .make(mCoordinatorLayout, "Error: " + intent.getStringExtra(RecorderStatus.ERROR_REASON), Snackbar.LENGTH_LONG)
                        .show();
                stopRecordingAnimations();
                break;
            case RecorderStatus.FINISH_ACTION:
                stopRecordingAnimations();

                final String path = intent.getStringExtra(RecorderStatus.FINISH_PATH);
                Snackbar
                        .make(mCoordinatorLayout, "Written to: " + path, Snackbar.LENGTH_LONG)
                        .setAction("Open", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent newIntent = new Intent(Intent.ACTION_VIEW);
                                newIntent.setDataAndType(Uri.fromFile(new File(path)), "video/x-matroska");
                                newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                try {
                                    mActivity.startActivity(newIntent);
                                } catch (ActivityNotFoundException e) {
                                    Toast.makeText(mActivity, "No handler for this type of file.", Toast.LENGTH_LONG).show();
                                }
                            }
                        })
                        .show();
                break;

            default:
        }
    }

    private void startRecordingAnimations(long duration) {
        mProgressBar.startAnimation((int) duration); // TODO Long <-> INT
        mRecFab.setRecording(true);
    }

    private void stopRecordingAnimations() {
        mProgressBar.stopAnimation();
        mRecFab.setRecording(false);
    }

}
