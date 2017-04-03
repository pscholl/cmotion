package de.uni_freiburg.es.sensorrecordingtool;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.NodeStatus;

/**
 * Notify the system about the current recorder stats, as well as the user.
 * The former via Broadcast intents, the latter via Notifications.
 */
public class RecorderStatus {
    private static final String TAG = RecorderStatus.class.getSimpleName();
    public final Context c;
    public final NotificationManagerCompat mService;
    public final NotificationCompat.Builder mNotification;

    /* action for reporting error from the recorder service */
    public static final String STATUS_ACTION = "recorder_status";
    public static final String ASK_STATUS_ACTION = "recorder_ask_status";
    public static final String STATUS_ELAPSED = "recorder_status_elapsed";
    public static final String STATUS_DURATION = "recorder_status_duration";
    public static final String ERROR_ACTION = "recorder_error";
    public static final String ERROR_REASON = "error_reason";
    public static final String FINISH_ACTION = "recorder_done";
    public static final String FINISH_PATH = "recording_path";
    public static final String RECORDING_UUID = "recording_uuid";
    public static final String SENSORS = "recording_sensors";
    public static final String ANDROID_ID = "recording_aid";
    public static final String PLATFORM = "recording_platform";
    public static final String START_TIME = "recording_starttime";
    public static final String DRIFT = "recording_drift";
    public static final String DRIFT_VALID = "recording_drift_valid";
    public static final String STATE = "recording_state";
    public static final String CONNECTIONTECH = "recording_connectiontech";
    public static final String CONNECTIONTECH_ID = "recording_connectiontech_ID";

    /* store the duration to handle the progressbar */
    public final int NOTIFICATION_ID = 123;
    public final int mDuration;
    public String mRecordUUID;

    private NodeStatus mSrtStatus = NodeStatus.UNKNOWN;

    /**
     * creates a new Notification and updates it for the user. Every other update
     * must be done through one of the finished, recording, or error function.
     *
     * @param context to create the notification on
     * @param recordUUID
     */
    public RecorderStatus(Context context, int inputs, double duration, String recordUUID) {
        c = context;
        mService = (NotificationManagerCompat) NotificationManagerCompat.from(c);
        mDuration = duration < 0 ? 0 : (int) duration * 1000;
        this.mRecordUUID = recordUUID;

        String content;
        content = c.getResources().getQuantityString(
                R.plurals.notification_text,
                inputs,
                inputs);

        PendingIntent pi = PendingIntent.getBroadcast(context, 0,
                new Intent(Recorder.SHOWUI_ACTION), 0);

        mNotification = new NotificationCompat.Builder(c)
                .setContentTitle(c.getString(R.string.notification_title))
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_fiber_manual_record_white_24dp)
                .setLocalOnly(true)
                .setProgress(mDuration, 0, mDuration == 0)
                .setContentIntent(pi)
                .setOngoing(true)
        ;


        Intent intent = new Intent();
        intent.setAction("senserec_cancel");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(c, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mNotification.addAction(R.drawable.ic_stop_white_24dp, c.getString(android.R.string.cancel), pendingIntent);

        mSrtStatus = NodeStatus.PREPARING;
        sendStatusIntent();

        if (!isRunningOnGlass())
            mService.notify(NOTIFICATION_ID, mNotification.build());
    }


    /**
     * to be called when the recording to which this Status is attached, is
     * finished. The Notification will then be stopped and a status intent sent.
     *
     * @param output path of the finished recording
     */
    public void finished(String output) {
        mSrtStatus = NodeStatus.FINISHED;

        Intent i = new Intent(FINISH_ACTION);
        i.putExtra(FINISH_PATH, output);
        includeIdentifier(i);
        c.sendBroadcast(i);

        sendStatusIntent();

        mNotification.mActions.clear(); // remove cancel button

        mNotification
                .setContentTitle(c.getString(R.string.done_title))
                .setProgress(0, 0, false)
                .setOngoing(false)
        ;

        if (!isRunningOnGlass())
            mService.notify(NOTIFICATION_ID, mNotification.build());
    }

    /**
     * to be called as a status update while the recording is ongoing, this will
     * then update the current notification and broadcast an intent.
     *
     * @param elapsed  timespan in milli-seconds which was recorded
     * @param duration timespan which will be recorded for
     */
    public void recording(long elapsed, long duration) {
        mSrtStatus = NodeStatus.RECORDING;

        Bundle extras = new Bundle();
        extras.putLong(STATUS_ELAPSED, elapsed);
        extras.putLong(STATUS_DURATION, duration);
        sendStatusIntent(extras);
        if (mDuration <= 0)
            return;
        mNotification
                .setProgress(mDuration, (int) elapsed, false)
        ;

        if (!isRunningOnGlass())
            mService.notify(NOTIFICATION_ID, mNotification.build());
    }

    /**
     * to be called when an error happended, the Exception that caused this error
     * will be broadcast to the rest of the system and the notification modified.
     *
     * @param exception reason for this error
     */
    public void error(Exception exception) {
        mSrtStatus = NodeStatus.ERROR;


        Intent i = new Intent(ERROR_ACTION);
        i.putExtra(ERROR_REASON, exception.getMessage());
        includeIdentifier(i);
        c.sendBroadcast(i);

        sendStatusIntent();

        mNotification.mActions.clear(); // remove cancel button

        mNotification
                .setContentTitle(c.getString(R.string.done_with_error_title))
                .setProgress(0, 0, false)
                .setOngoing(false)
        ;

        if (!isRunningOnGlass())
            mService.notify(NOTIFICATION_ID, mNotification.build());

    }


    /**
     * to be called when an all sensors are initialised, only sent by slaves.
     *
     * @param sensors all initialised sensors
     */
    public void ready(String[] sensors, long drift, boolean driftSet) {
        mSrtStatus = NodeStatus.READY;

        Bundle extra = new Bundle();
        extra.putStringArray(SENSORS, sensors);
        extra.putDouble(DRIFT, drift);
        extra.putBoolean(DRIFT_VALID, driftSet);

        Intent i = new Intent(Recorder.READY_ACTION);
        i.putExtras(extra);
        includeIdentifier(i);
        c.sendBroadcast(i);
        Log.e(TAG, "sending ready");

        sendStatusIntent(extra);
    }

    private void includeIdentifier(Intent i) {
        i.putExtra(ANDROID_ID, Settings.Secure.getString(c.getContentResolver(),
                Settings.Secure.ANDROID_ID));
        i.putExtra(PLATFORM, Build.BOARD);
        i.putExtra(RECORDING_UUID, mRecordUUID);
    }

    /**
     * to be called when an all nodes are initialised, only sent by master node.
     *
     * @param startTime wall-clock time at which all nodes shall start recording the intinitalised sensors
     */
    public void steady(long startTime) {
        Intent i = new Intent(Recorder.STEADY_ACTION);
        i.putExtra(START_TIME, startTime * 1d);
        includeIdentifier(i);
        c.sendBroadcast(i);
        Log.e(TAG, "sending steady");
    }

    private void sendStatusIntent() {
        sendStatusIntent(null);
    }

    private void sendStatusIntent(@Nullable Bundle extras) {
        Intent intent = new Intent(STATUS_ACTION);
        intent.putExtra(STATE, mSrtStatus.toString());
        if (extras != null)
            intent.putExtras(extras);
        intent.putExtra("doWearForward", !Recorder.isMaster);
        includeIdentifier(intent);
        c.sendBroadcast(intent);
    }


    /**
     * Determine when the the code is running on Google Glass
     *
     * @return True if and only if Manufacturer is Google and Model begins with Glass
     */
    public static boolean isRunningOnGlass() {
        return "Google".equalsIgnoreCase(Build.MANUFACTURER) && Build.MODEL.startsWith("Glass");
    }


}
