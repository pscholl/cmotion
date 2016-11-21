package de.uni_freiburg.es.sensorrecordingtool;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.util.List;

import de.uni_freiburg.es.sensorrecordingtool.sensors.SensorProcess;

/** Notify the system about the current recorder stats, as well as the user.
 * The former via Broadcast intents, the latter via Notifications.
 *
 */
public class RecorderStatus {
  public final Context c;
  public final NotificationManagerCompat mService;
  public final NotificationCompat.Builder mNotification;

  /* action for reporting error from the recorder service */
  public static final String STATUS_ACTION  = "recorder_status";
  public static final String STATUS_ELAPSED = "recorder_status_elapsed";
  public static final String STATUS_DURATION = "recorder_status_duration";
  public static final String ERROR_ACTION  = "recorder_error";
  public static final String ERROR_REASON  = "error_reason";
  public static final String FINISH_ACTION = "recorder_done";
  public static final String FINISH_PATH   = "recording_path";

  /* store the duration to handle the progressbar */
  public final int mDuration;

  /** creates a new Notification and updates it for the user. Every other update
   * must be done through one of the finished, recording, or error function.
   *
   * @param context to create the notification on
   */
  public RecorderStatus(Context context, List<SensorProcess> inputs, double duration) {
    c = context;
    mService  = (NotificationManagerCompat) NotificationManagerCompat.from(c);
    mDuration = duration < 0 ? 0 : (int) duration * 1000;

    String content;
    content = c.getResources().getQuantityString(
              R.plurals.notification_text,
              inputs.size(),
              inputs.size());

    mNotification = new NotificationCompat.Builder(c)
      .setContentTitle( c.getString(R.string.notification_title) )
      .setContentText( content )
      .setSmallIcon( R.drawable.ic_fiber_manual_record_white_24dp )
      .setLocalOnly( true )
      .setProgress(mDuration, 0, mDuration == 0)
      .setOngoing(true)
      ;

    if (!isRunningOnGlass())
      mService.notify(123, mNotification.build());
  }


  /** to be called when the recording to which this Status is attached, is
   * finished. The Notification will then be stopped and a status intent sent.
   *
   * @param output path of the finished recording
   */
  public void finished(String output) {
    Intent i = new Intent(FINISH_ACTION);
    i.putExtra(FINISH_PATH, output);
    c.sendBroadcast(i);

    mNotification
      .setContentTitle( c.getString(R.string.done_title) )
      .setProgress(0, 0, false)
      .setOngoing(false)
      ;

    if (!isRunningOnGlass())
      mService.notify(123, mNotification.build());
  }

  /** to be called as a status update while the recording is ongoing, this will
   * then update the current notification and broadcast an intent.
   *
   * @param elapsed timespan in milli-seconds which was recorded
   * @param duration timespan which will be recorded for
   */
  public void recording(long elapsed, long duration) {
    Intent i = new Intent(STATUS_ACTION);
    i.putExtra(STATUS_ELAPSED, elapsed);
    i.putExtra(STATUS_DURATION, duration);
    c.sendBroadcast(i);

    if (mDuration <= 0)
      return;

    mNotification
      .setProgress(mDuration, (int) elapsed, false)
      ;

    if (!isRunningOnGlass())
      mService.notify(123, mNotification.build());
  }

  /** to be called when an error happended, the Exception that caused this error
   * will be broadcast to the rest of the system and the notification modified.
   *
   * @param exception reason for this error
   */
  public void error(Exception e) {
    Intent i = new Intent(ERROR_ACTION);
    i.putExtra(ERROR_REASON, e.getMessage());
    c.sendBroadcast(i);

    mNotification
      .setContentTitle( c.getString(R.string.done_with_error_title) )
      .setProgress(0, 0, false)
      .setOngoing(false)
      ;

    if (!isRunningOnGlass())
      mService.notify(123, mNotification.build());
  }

  /** Determine whethe the code is runnong on Google Glass
   *
   * @return True if and only if Manufacturer is Google and Model begins with Glass
   */
  public static boolean isRunningOnGlass() {
    return "Google".equalsIgnoreCase(Build.MANUFACTURER) && Build.MODEL.startsWith("Glass");
  }
}
