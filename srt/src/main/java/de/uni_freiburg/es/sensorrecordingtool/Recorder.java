package de.uni_freiburg.es.sensorrecordingtool;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedList;

import de.uni_freiburg.es.intentforwarder.ForwardedUtils;
import de.uni_freiburg.es.sensorrecordingtool.sensors.BlockSensorProcess;
import de.uni_freiburg.es.sensorrecordingtool.sensors.SensorProcess;

/**
 * A tool for recording arbitrary combinations of sensor attached and reachable via Android. The
 * idea is to run this service with a single Intent call, similar to how videos or images are
 * captured. This makes it also possible to start and stop recording via the adb shell. For example
 * if you would want to record the accelerometer and the orientation at 50Hz you can do so with
 * the following Intent:
 *
 *      Intent i = new Intent(Recorder.RECORD_ACTION);
 *      i.putString('-i', ['accelerometer', 'orientation']);
 *      i.putFloat('-r', 50.0);
 *      sendBroadcast(i);
 *
 *  or from the adb shell:
 *
 *      adb shell am broadcast -a senserec -e -i accelerometer
 *
 *   Supported arguments are:
 *
 *     -i [String or list of String]
 *        sensors to record, providing an unknown one will print a list on logcat
 *
 *     -r [int/float or list of int/float]
 *        recording rate of each input, or if only a single value is given the rate for all sensors
 *
 *     -o [String]
 *        output directory under /sdcard/DCIM under which the recordings are stored
 *
 *     -f [single string or list of strings]
 *        list of string specifying the sensor format for each input, null to use the default,
 *        currently only the video sensor has any specs, which is the recording size given as
 *        widthxheight, e.g. 1280x720.
 *
 *   A Broadcast Intent is sent once the recording is started or canceled. The latest recording
 *   can be canceled with the senserec_cancel broadcast action, e.g.:
 *
 *      adb shell am broadcast -a senserec_cancel
 *
 *   it is also possible to cancel a specific recording by supplying its id:
 *
 *      adb shell am broadcast -a senserec_cancel -r <id>
 *
 * Created by phil on 2/22/16.
 */
public class Recorder extends Service {
    static final String TAG = Recorder.class.getName();

    /* designate the sensor you want to record, can either be a single String or a list thereof,
     * possible values are the String identifier of Android sensors, visible here:
      * https://developer.android.com/reference/android/hardware/Sensor.html
      *
      * Accelerometer for example is android.sensor.accelerometer, you can leave the android.sensor
      * prefix out. */
    static final String RECORDER_INPUT = "-i";

    /* the rate at which to record, a single one will apply to all input. If multiple sensors are
     * given, multiple rates can be applied to each input. Default rate is 50Hz */
    static final String RECORDER_RATE  = "-r";

    /* the duration of the recording, given in seconds, default is 10 seconds. */
    static final String RECORDER_DURATION = "-d";

    /* the optional output path */
    static final String RECORDER_OUTPUT = "-o";

    /* optional format specifier for each sensor */
    static final String RECORDER_FORMAT = "-f";

    /* the main action for recording */
    public static final String RECORD_ACTION = ForwardedUtils.RECORD_ACTION;

    /* action for reporting error from the recorder service */
    static final String ERROR_ACTION  = "recorder_error";
    static final String ERROR_REASON  = "error_reason";
    static final String FINISH_ACTION = "recorder_done";
    static final String FINISH_PATH   = "recording_path";

    /* for handing over the cancel action from a notification */
    public static final String CANCEL_ACTION = "senserec_cancel";
    public static final String RECORDING_ID = "-r";

    /* keep track of all running recordings */
    private LinkedList<RecordingProcess> mRecordings = new LinkedList<RecordingProcess>();

    /** called with the startService routine, will parse all RECORDER_INPUTs for known values
     *  and start the recording. If no RECORDER_OUTPUT directory is given it will default to
     *  a directory named with the current timestamp in ISO-format on the sdcard.
     */
    @Override
    public int onStartCommand(final Intent i, int flags, int startId) {
        if (i == null)
            return START_NOT_STICKY;

        /*
         * make sure that we have permission to write the external folder, if we do not have
         * permission currently the user will be bugged about it. And this service will be
         * restarted with a null action intent.
        if (!PermissionDialog.externalStorage(this)) {
            Intent diag = new Intent(this, PermissionDialog.class);
            if (i.getExtras() != null)
                diag.putExtras(i.getExtras());
            diag.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(diag);
            return START_NOT_STICKY;
        }
         */

        /*
         * terminate the recording right now, if the user wishes so.
         */
        if (CANCEL_ACTION.equals(i.getAction())) {
            int id = i.getIntExtra(RECORDING_ID, mRecordings.size() - 1);

            try {
                Notification.cancelRecording(this.getApplicationContext(), id);

                if (id < 0 || id >= mRecordings.size()) {
                    Log.d(TAG, "invalid recording id " + id);
                    return START_NOT_STICKY;
                }

                mRecordings.get(id).terminate();
                mRecordings.remove(id);
                Log.d(TAG, "terminated recording " + id);
            } catch (IndexOutOfBoundsException e) {
                Log.d(TAG, "unable to find recording with id " + id);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return START_NOT_STICKY;
        } else {
            newRecordingProcess(i);
            return START_NOT_STICKY;
        }
    }

    public void newRecordingProcess(Intent i) {
        try {
            RecordingProcess recording = new RecordingProcess(this,
                i.getStringExtra(RECORDER_OUTPUT),
                getStringOrArray(i, RECORDER_INPUT),
                getStringOrArray(i, RECORDER_FORMAT),
                getIntFloatOrDoubleArray(i, RECORDER_RATE, 50.),
                getIntFloatOrDouble(i, RECORDER_DURATION, -1) );

            for (int j=0; j<recording.sensors.length; j++) {
                OutputStream os = recording.getOutputStream(j);
                SensorProcess sp = newSensorProcess( recording.sensors[j],
                                                     recording.formats[j],
                                                     recording.rates[j],
                                                     recording.duration, os );
                recording.mInputList.add(sp);
            }

            mRecordings.add(recording);
            Notification.newRecording(this, mRecordings.indexOf(recording), recording);
        } catch (Exception e) {
            i = new Intent();
            i.setAction(ERROR_ACTION);
            i.putExtra(ERROR_REASON, e.getMessage());
            sendBroadcast(i);
            e.printStackTrace();
        }
    }

    private SensorProcess newSensorProcess(String sensor, String format, double rate,
                                           double dur, OutputStream os) throws Exception {
        Context c = this.getApplicationContext();

        if (sensor.contains("video"))
            return new BlockSensorProcess(c, sensor, rate, format, dur, os);
        else
            return new SensorProcess(c, sensor, rate, format, dur, os);
    }

    public static String[] getStringOrArray(Intent i, String extra) {
        String[] arr = i.getStringArrayExtra(extra);
        if (arr != null)
            return arr;
        else if (i.getStringExtra(extra) != null)
            return new String[] { i.getStringExtra(extra) };
        else
            return new String[] { };
    }

    public static double[] getIntFloatOrDoubleArray(Intent i, String extra, double def) {
        int iarr[] = i.getIntArrayExtra(extra);
        float farr[] = i.getFloatArrayExtra(extra);
        double darr[] = i.getDoubleArrayExtra(extra);

        if (darr != null)
            return darr;

        if (farr != null) {
            double out[] = new double[farr.length];
            for (int j=0; j<out.length; j++)
                out[j] = farr[j];
            return out;
        }

        if (iarr != null) {
            double out[] = new double[iarr.length];
            for (int j=0; j<out.length; j++)
                out[j] = iarr[j];
            return out;
        }

        return new double[]{getIntFloatOrDouble(i,extra,def)};
    }

    public static double getIntFloatOrDouble(Intent i, String extra, double def) {
        int ivalue = i.getIntExtra(extra, -1);
        float fvalue = i.getFloatExtra(extra, -1);
        double dvalue = i.getDoubleExtra(extra, -1);

        if (dvalue != -1)
            return dvalue;

        if (fvalue != -1)
            return fvalue;

        if (ivalue != -1)
            return ivalue;

        return def;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void finished(RecordingProcess recordingProcess) {
        Intent i = new Intent(Recorder.FINISH_ACTION);
        i.putExtra(Recorder.FINISH_PATH, recordingProcess.output);
        i.putExtra(Recorder.RECORDING_ID, mRecordings.indexOf(this));
        sendBroadcast(i);
        mRecordings.remove(this);
    }
}
