    package es.uni_freiburg.de.cmotion;

import com.google.android.glass.app.Card;
import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;
import com.google.android.glass.widget.CardBuilder;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.HashMap;

import de.uni_freiburg.es.sensorrecordingtool.Notification;
import de.uni_freiburg.es.sensorrecordingtool.Recorder;

    /**
 * A {@link Service} that publishes a {@link LiveCard} in the timeline.
 */
public class CMotionCardService extends Service {

    private static final String LIVE_CARD_TAG = "CMotionCardService";
    private static final String TAG = CMotionCardService.class.getSimpleName();

    protected UpdateLiveCard card;
    private Handler mHandler = new Handler();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null)
            return START_STICKY;

        String action = intent.getAction();

        if (Notification.NEW_RECORDING.equals(action))
            newRecording(intent.getIntExtra(Notification.EXTRA_NUM_SENSORS, -1),
                         intent.getDoubleExtra(Notification.EXTRA_DURATION, -1.));
        else if (Notification.CANCEL_RECORDING.equals(action))
            cancelRecording();
        else if (Notification.FINISHED_RECORDING.equals(action))
            finishRecording();
        else
            Log.d(TAG, "unknown action " + action);

        return START_STICKY;
    }

    private void finishRecording() {
        if (card != null) {
            card.setFinished(true);
            card.unpublish();
        }
    }

    private void cancelRecording() {
        finishRecording();
    }

    private void newRecording(Integer numsensor, double duration) {
        if (card == null) {
            card = new UpdateLiveCard(this, LIVE_CARD_TAG, numsensor, duration);
            card.setVoiceActionEnabled(true);
        }

        Intent menuIntent = new Intent(this, CMotionCardMenuActivity.class);
        card.setAction(PendingIntent.getActivity(this, 0,
                menuIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        if (!card.isPublished())
            card.publish(PublishMode.REVEAL);
    }

    private CharSequence recordingAsString(Integer numsensor) {
        return String.format("recording %d sensors", numsensor);
    }

    public CharSequence getRecorded(Integer numsensor) {
        return String.format("recorded %d sensors", numsensor);
    }

    public CharSequence durationAsString(double duration) {
        if (duration > 0)
            return String.format("%02d:%02d",
                    (int) (duration/60.), (int) (duration%60));
        else
            return "";
    }

    private class UpdateLiveCard extends LiveCard implements Runnable {
        private static final long DELAY_MS = 1000;
        private final RemoteViews mViews;
        private final double mDuration;
        private final int mNumSensors;
        public double mElapsed = -DELAY_MS/1000.;
        private boolean mFinished;

        public UpdateLiveCard(Context context, String tag, int numsensor, double duration) {
            super(context, tag);

            RemoteViews rv = new RemoteViews(getPackageName(), R.layout.cmotion_card);
            rv.setTextViewText(R.id.elapsed, "00:00");
            rv.setTextViewText(R.id.duration, durationAsString(duration));
            rv.setTextViewText(R.id.recording, recordingAsString(numsensor));
            setViews(rv);

            mNumSensors = numsensor;
            mDuration = duration;
            mViews = rv;
            mHandler.post(this);
        }

        @Override
        public void run() {
            mElapsed += DELAY_MS/1000.;
            String elapsed;

            if (mFinished) {
                elapsed = String.format("%02d:%02d", (int) (mDuration / 60), (int) (mDuration % 60));
                mViews.setTextViewText(R.id.recording, getRecorded(mNumSensors));
            } else
                elapsed = String.format("%02d:%02d", (int) (mElapsed/60), (int) (mElapsed%60));

            mViews.setTextViewText(R.id.elapsed, elapsed);
            setViews(mViews);

            if (isPublished() && !mFinished)
                mHandler.postDelayed(this, DELAY_MS);
        }

        public void setFinished(boolean finished) {
            this.mFinished = finished;
        }
    }
}
