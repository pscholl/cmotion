package de.uni_freiburg.es.sensorrecordingtool.merger;

import android.content.Context;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import java.util.Random;

import de.uni_freiburg.es.sensorrecordingtool.R;

/**
 * Notify the system about the current recorder stats, as well as the user.
 * The former via Broadcast intents, the latter via Notifications.
 */
public class MergeStatus {
    private static final String TAG = MergeStatus.class.getSimpleName();
    public final Context c;
    public final NotificationManagerCompat mService;
    public final NotificationCompat.Builder mNotification;

    /* store the duration to handle the progressbar */
    public int NOTIFICATION_ID = new Random().nextInt();
    public String mRecordUUID;

    private int mCounter = 0, mMax = 0;



    public MergeStatus(Context context, String recordUUID, int maxThreads) {
        c = context;
        mService = (NotificationManagerCompat) NotificationManagerCompat.from(c);

        mMax = maxThreads;

        mNotification = new NotificationCompat.Builder(c)
                .setContentTitle("Merging "+recordUUID)
                .setContentText("Waiting for inputs")
                .setSmallIcon(R.drawable.ic_merge_type_black_24dp)
                .setLocalOnly(true)
                .setOngoing(true)
        ;


        if (!isRunningOnGlass())
            mService.notify(NOTIFICATION_ID, mNotification.build());
    }



    public void error(Exception exception) {

//        mNotification.mActions.clear(); // remove cancel button

        mNotification
                .setContentTitle("Merging Error")
                .setProgress(0, 0, false)
                .setOngoing(false)
        ;

        if (!isRunningOnGlass())
            mService.notify(NOTIFICATION_ID, mNotification.build());

    }

    /**
     * Determine when the the code is running on Google Glass
     *
     * @return True if and only if Manufacturer is Google and Model begins with Glass
     */
    public static boolean isRunningOnGlass() {
        return "Google".equalsIgnoreCase(Build.MANUFACTURER) && Build.MODEL.startsWith("Glass");
    }


    public void finished(String output) {
        mNotification
                .setContentTitle("Merging finished")
                .setContentText(output)
                .setProgress(0, 0, false)
                .setOngoing(false)
        ;

        if (!isRunningOnGlass())
            mService.notify(NOTIFICATION_ID, mNotification.build());
    }

    public void incrementProgress() {
        mCounter++;
        mNotification
                .setContentTitle("Merging in progress")
                .setProgress(mMax, mCounter, false)
                .setOngoing(false)
        ;

    }
}
