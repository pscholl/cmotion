package de.uni_freiburg.es.sensorrecordingtool;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

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

    private Notification() {} // static access, disable constructor

    private static Notification sInstance;
    public static Notification getInstance() {
        if (sInstance == null)
            sInstance = new Notification();
        return sInstance;
    }

    static public void newRecording(final Context c, final int id, String output) {
        getInstance().toggleState(c, id, NotificationState.PREPARE, output);
    }

    public void newRecording(final Context c, final int id) {
        getInstance().toggleState(c, id, NotificationState.PREPARE);
    }


    public void toggleState(final Context c, final int id, NotificationState state, final String... string) {

        String title, message;

        title = message = "";

        switch (state) {
            case PREPARE:
                title = "Preparing";
                message = "Waiting for sensors";
                break;
            case RECORD:
                title = "Recording has started";
                message = "Saving to "+string[0];
                break;
            case FINISH:
                title = "Recording has finished";
                message = "Saved to "+string[0];
                break;
            case ERROR:
                title = "Recording error";
                message = string[0];
                break;
        }

        NotificationManager mgr = (NotificationManager) c.getSystemService(c.NOTIFICATION_SERVICE);
        android.app.Notification.Builder builder = new android.app.Notification.Builder(c)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.common_google_signin_btn_icon_light)
                .setPriority(android.app.Notification.PRIORITY_MAX)
                .setWhen(0)
                .setOngoing(state != NotificationState.FINISH && state != NotificationState.ERROR)
                // allow only error and finish notifications to be dismissable
        ;

        if(state == NotificationState.RECORD) {
            Intent intent = new Intent();
            intent.setAction("senserec_cancel");
            PendingIntent pendingIntent = PendingIntent.getBroadcast(c, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(android.R.drawable.radiobutton_off_background, c.getString(android.R.string.cancel), pendingIntent);
//           builder.addAction(new android.app.Notification.Action(R.drawable.cancel, c.getString(android.R.string.cancel), pendingIntent));

        }

        mgr.notify(id, builder.build());
    }

    public void hide(final Context c, final int id) {
        NotificationManager mgr = (NotificationManager) c.getSystemService(c.NOTIFICATION_SERVICE);
        mgr.cancel(id);
    }

    public static void cancelRecording(Context c, int id) {
        NotificationManager mgr = (NotificationManager) c.getSystemService(c.NOTIFICATION_SERVICE);
        isCanceled.add(id);
        mgr.cancel(id);

        /** and also send the cancel action */
        Intent i = new Intent(CANCEL_RECORDING);
        c.sendBroadcast(i);
    }

    /** Determine whether the code is running on Google Glass.
     *
     * @return True if and only if Manufacturer is Google and Model begins with Glass
     */
    public static boolean isRunningOnGlass() {
        return "Google".equalsIgnoreCase(Build.MANUFACTURER) && Build.MODEL.startsWith("Glass");
    }

    enum NotificationState {
        PREPARE, RECORD, FINISH, ERROR
    }

}
