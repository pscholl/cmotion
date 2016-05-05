package de.uni_freiburg.es.sensorrecordingtool;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.text.format.DateUtils;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

/** This is just a placeholder for showing, hiding and updating the notification shown for
 *  each recording process. Mainly to unclutter the code in the Recorder class.
 *
 * Created by phil on 2/29/16.
 */
public class Notification {
    private static final long DELAY = 250;
    private static final String RECORDING_GROUP_KEY = "recording_group_key";
    public static final String NEW_RECORDING = "es.uni_freiburg.es.sensorrecordingtool.NEW_RECORDING";
    public static final String CANCEL_RECORDING = "es.uni_freiburg.es.sensorrecordingtool.CANCEL_RECORDING";
    public static final String FINISHED_RECORDING = "es.uni_freiburg.es.sensorrecordingtool.FINISHED_RECORDING";
    public static final String EXTRA_NUM_SENSORS = "NUM_SENSORS";
    public static final String EXTRA_DURATION = "DURATION";

    static Set<Integer> isCanceled = new HashSet<Integer>();

    static public void newRecording(final Context c, final int id, final Recorder.Recording r) {
        Intent cancel_intent = new Intent(Recorder.CANCEL_ACTION);
        cancel_intent.putExtra(Recorder.RECORDING_ID, id);
        PendingIntent pending  = PendingIntent.getBroadcast(c, id, cancel_intent,
                                                            PendingIntent.FLAG_UPDATE_CURRENT);

        Intent openfolder = new Intent(Intent.ACTION_GET_CONTENT);
        openfolder.setDataAndType(Uri.parse("file://"+r.mOutputPath), "*/*");
        Log.d(Recorder.TAG, r.mOutputPath);
        PendingIntent openfolderp = PendingIntent.getActivity(c, id,
                Intent.createChooser(openfolder, c.getString(R.string.choose)),
                                            PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Action.Builder cancel = new NotificationCompat.Action.Builder(
                R.drawable.cancel, c.getString(R.string.cancel_text), pending);

        final NotificationCompat.Builder notification = new NotificationCompat.Builder(c)
                .setContentTitle(c.getString(R.string.notification_title))
                .setContentText(String.format(c.getResources()
                    .getQuantityString(R.plurals.notification_text, r.mInputList.size()),
                        r.mInputList.size(),
                        DateUtils.formatElapsedTime((long) r.mInputList.get(0).mDur)))
                .setSmallIcon(R.drawable.recording)
                .setLocalOnly(true)
                .setContentIntent(openfolderp)
                .setDeleteIntent(pending)
                .setProgress(100, 0, false)
                .setGroup(RECORDING_GROUP_KEY)
                .addAction(cancel.build());

        final NotificationManager mgr = (NotificationManager) c.getSystemService(c.NOTIFICATION_SERVICE);

        if (!isRunningOnGlass())
            mgr.notify(id, notification.build());


        /*
         * also create a handler for updating the progress on the Recording, and make sure to
         * stop updating once this recording has been canceled.
         */
        final Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                double total = 0, done = 0;
                for (Recorder.SensorProcess p : r.mInputList) {
                    done += p.mElapsed;
                    total += p.mDur;
                }

                if (r.mInputList.size() == 0) {
                    notification.mActions.clear();
                    notification.setContentTitle(c.getString(R.string.done_title));

                    Intent i = new Intent(FINISHED_RECORDING);
                    i.putExtra(Recorder.RECORDING_ID, id);
                    c.sendBroadcast(i);
                }

                notification.setProgress((int) total, (int) done, false);
                if (!isCanceled.contains(id)) {
                    if (!isRunningOnGlass())
                        mgr.notify(id, notification.build());

                    if (r.mInputList.size() != 0)
                        h.postDelayed(this, DELAY);
                }

            }
        }, DELAY);

        if (isCanceled.contains(id))
            isCanceled.remove(id);

        /** also send a broadcast to notify other components, for example on Glass to update
         * their displays. */
        Intent i = new Intent(NEW_RECORDING);
        i.putExtra(EXTRA_NUM_SENSORS, r.mInputList.size());
        i.putExtra(EXTRA_DURATION, r.mInputList.get(0).mDur);
        i.putExtra(Recorder.RECORDING_ID, id);
        c.sendBroadcast(i);
    }

    public static void cancelRecording(Context c, int id) {
        NotificationManager mgr = (NotificationManager) c.getSystemService(c.NOTIFICATION_SERVICE);
        isCanceled.add(id);
        mgr.cancel(id);

        /** and also send the cancel action */
        Intent i = new Intent(CANCEL_RECORDING);
        i.putExtra(Recorder.RECORDING_ID, id);
        c.sendBroadcast(i);
    }

    /** Determine whethe the code is runnong on Google Glass
     * @return True if and only if Manufacturer is Google and Model begins with Glass
     */
    public static boolean isRunningOnGlass() {
        return "Google".equalsIgnoreCase(Build.MANUFACTURER) && Build.MODEL.startsWith("Glass");
    }

}
