    package es.uni_freiburg.de.cmotion;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.TextView;

import java.util.HashMap;

import de.uni_freiburg.es.sensorrecordingtool.Notification;
import de.uni_freiburg.es.sensorrecordingtool.Recorder;

    /**
 * A {@link Service} that publishes a {@link LiveCard} in the timeline.
 */
public class CMotionCardService extends Service {

    private static final String LIVE_CARD_TAG = "CMotionCardService";
    private static final String TAG = CMotionCardService.class.getSimpleName();

    protected HashMap<Integer, LiveCard> cards = new HashMap<Integer, LiveCard>();
    private Handler mHandler = new Handler();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        Integer id = intent.getIntExtra(Recorder.RECORDING_ID, -1);

        if (id==-1) {
            Log.d(TAG, "unknown recording");
            return START_STICKY;
        }

        if (Notification.NEW_RECORDING.equals(action))
            newRecording(id, intent.getIntExtra(Notification.EXTRA_NUM_SENSORS, -1),
                             intent.getDoubleExtra(Notification.EXTRA_DURATION, -1.));
        else if (Notification.CANCEL_RECORDING.equals(action))
            cancelRecording(id);
        else if (Notification.FINISHED_RECORDING.equals(action))
            finishRecording(id);
        else
            Log.d(TAG, "unknown action " + action);

        return START_STICKY;
    }

    private void finishRecording(Integer id) {
        Log.d(TAG, "finished recording " + id);

        UpdateLiveCard card = (UpdateLiveCard) cards.get(id);
        if (card != null)
            card.setFinished(true);
    }

    private void cancelRecording(Integer id) {
        Log.d(TAG, "cancel recording " + id);
        String tag = LIVE_CARD_TAG+id.toString();
        LiveCard card = cards.get(id);
        if (card != null && card.isPublished())
            card.unpublish();
    }

    private void newRecording(Integer id, Integer numsensor, double duration) {
        Log.d(TAG, "new recording " + id);

        LiveCard card = cards.get(id);
        if (card == null) {
            String tag = LIVE_CARD_TAG + id.toString();
            card = new UpdateLiveCard(this, tag, numsensor, duration);
            cards.put(id, card);
        }

        Intent menuIntent = new Intent(this, LiveCardMenuActivity.class);
        menuIntent.putExtra(Recorder.RECORDING_ID, id);
        card.setAction(PendingIntent.getActivity(this, 0,
                menuIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        card.publish(PublishMode.REVEAL);
    }

    private CharSequence recordingAsString(Integer numsensor) {
        return String.format("recording %d sensors", numsensor);
    }

    public CharSequence getRecorded(Integer numsensor) {
        return String.format("recorded %d sensors", numsensor);
    }

    private CharSequence durationAsString(double duration) {
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
        private double mElapsed = -DELAY_MS/1000.;
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
