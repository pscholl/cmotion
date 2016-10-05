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

import de.uni_freiburg.es.sensorrecordingtool.sensors.SensorProcess;

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

    static public void newRecording(final Context c, final int id, String output) {
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

    /** Determine whether the code is running on Google Glass
     * @return True if and only if Manufacturer is Google and Model begins with Glass
     */
    public static boolean isRunningOnGlass() {
        return "Google".equalsIgnoreCase(Build.MANUFACTURER) && Build.MODEL.startsWith("Glass");
    }

}
