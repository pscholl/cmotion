package de.uni_freiburg.es.sensorrecordingtool;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import de.uni_freiburg.es.sensorrecordingtool.sensors.Sensor;

/**
 * A tool for recording arbitrary combinations of sensor attached and reachable via Android. The
 * idea is to run this service with a single Intent call, similar to how videos or images are
 * captured. This makes it also possible to start and stop recording via the adb shell. For example
 * if you would want to record the accelerometer and the orientation at 50Hz you can do so with
 * the following Intent:
 *
 *     Intent i = new Intent();
 *     i.putString('-i', ['accelerometer', 'orientation']);
 *     i.putFloat('-r', 50.0);
 *     sendBroadcast(i);
 *
 *  or from the adb shell:
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

    /* the main action for recording */
    public static final String RECORD_ACTION = "senserec";

    /* action for reporting error from the recorder service */
    static final String ERROR_ACTION  = "recorder_error";
    static final String ERROR_REASON  = "error_reason";
    static final String FINISH_ACTION = "recorder_done";
    static final String FINISH_PATH   = "recording_path";

    /* for handing over the cancel action from a notification */
    static final String CANCEL_ACTION = "cancel";
    static final String RECORDING_ID = "-r";

    /* keep track of all running recordings */
    private LinkedList<Recording> mRecordings = new LinkedList<>();

    /** called with the startService routine, will parse all RECORDER_INPUTs for known values
     *  and start the recording. If no RECORDER_OUTPUT directory is given it will default to
     *  a directory named with the current timestamp in ISO-format on the sdcard.
     */
    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        String[] sensors = i.getStringArrayExtra(RECORDER_INPUT);
        String output    = i.getStringExtra(RECORDER_OUTPUT);
        double[] rates   = getIntFloatOrDoubleArray(i, RECORDER_RATE, 50.);
        double duration  = getIntFloatOrDouble(i, RECORDER_DURATION, 5.);

        if (i == null)
            return START_NOT_STICKY;

        /*
         * make sure that we have permission to write the external folder, if we do not have
         * permission currently the user will be bugged about it. And this service will be
         * restarted with a null action intent.
         */
        if (!PermissionDialog.externalStorage(this)) {
            Intent diag = new Intent(this, PermissionDialog.class);
            if (i.getExtras() != null)
                diag.putExtras(i.getExtras());
            diag.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(diag);
            return START_NOT_STICKY;
        }

        /*
         * terminate the recording right now, if the user wishes so.
         */
        if (CANCEL_ACTION.equals(i.getAction())) {
            int id = i.getIntExtra(RECORDING_ID, -1);

            try {
                Notification.cancelRecording(this, id);

                Recording r = mRecordings.get(id);
                for( Iterator<SensorProcess> it = r.mInputList.iterator(); it.hasNext(); ) {
                    SensorProcess p = it.next();
                    p.terminate();
                }
                Log.d(TAG, "terminated recording " + id);
            } catch(IndexOutOfBoundsException e) {
                Log.d(TAG, "unable to find recording with id " + id);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return START_NOT_STICKY;
        }

        /*
         * check the output file
         */
        if (output == null)
            output = getDefaultOutputPath();

        /*
         * check if we have sensor input
         */
        if (sensors == null && i.getStringExtra(RECORDER_INPUT)!=null)
            sensors = new String[] {i.getStringExtra(RECORDER_INPUT)};

        if (sensors == null)
            return error("no input supplied");

        /*
         * convert a single rate to an array, convert a single element array to a length
         * matching the sensor input array, check whether lengths are matching.
         */
        if (rates.length == 1) {
            rates = Arrays.copyOf(rates, sensors.length);
            Arrays.fill(rates, rates[0]);
        }

        if (rates.length != sensors.length)
            return error("either rates and sensors must of same length or a single rate must be given");

        for (double r : rates)
            if (r <= 0)
                return error("rate must be larger than zero, but was " + r);

        /* now try and create the output folder */
        File path = new File(output);
        if ( !path.exists() && !path.mkdirs() )
            return error("unable to create " + path);

        if ( path.exists() && !path.isDirectory() )
            return error("path is not a directory " + path);
        /*
         * All good here, now start to create a sensorlistener for each input and create its
         * accompanying output file and start the whole process. Give each bundle a maximum
         * duration. And create the output folder beforehand.
         */
        assert(rates.length==sensors.length);

        Recording r = new Recording(output, duration);
        for (int j=0; j<rates.length; j++) {
            try {
                BufferedOutputStream bf =  new BufferedOutputStream(
                        new FileOutputStream(new File(output, sensors[j])));
                SensorProcess sp = new SensorProcess(sensors[j], rates[j], duration, bf);
                r.add(sp);
            } catch (Exception e) {
                error(sensors[j] + ": " + e.getLocalizedMessage());
                /* we do best effort recordings */
            }
        }
        mRecordings.add(r);

        /*
         * create the notification.
         */
        Notification.newRecording(this, mRecordings.indexOf(r), r);

        return START_NOT_STICKY;
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
            double out[] = new double[farr.length];
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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private int error(String s) {
        Intent err = new Intent(ERROR_ACTION);
        err.putExtra(ERROR_REASON, s);
        sendBroadcast(err);
        Log.e(TAG, s);
        return START_NOT_STICKY;
    }

    /** utility function for ISO datetime folder on public storage */
    static String getDefaultOutputPath() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
        df.setTimeZone(tz);
        File path = new File("/sdcard/DCIM/");
        return new File(path, df.format(new Date())).toString();
    }

    /** Copies *sensor* data for *dur* seconds at *rate* to a bufferedwriter *bf*. Automatically
     * closes the output buffer when done.
     *
     * This is all done in binary float format, all the channels are recorded and the current
     * accuracy of the process. For example the accelerometer reports all three axes, so one sample
     * is 3 (channels) * 4 (bytes per float) + 4 (bytes for accuracy as float).
     *
     * Arguments:
     *  sensor - a string representation for the sensor, must match Android Sensor types
     *  dur    - a double seconds of recording duration
     *  rate   - double Hz for the recording, non-negative
     *  bf     - a bufferedwriter, where data gets written to in binary format.
     *
     */
    protected class SensorProcess implements SensorEventListener {
        public static final int LATENCY_US = 5 * 60 * 1000 * 1000;
        final Sensor mSensor;
        float mAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_LOW;
        final double mRate;
        final BufferedOutputStream mOut;
        final double mDur;
        ByteBuffer mBuf;
        long mLastTimestamp = -1;
        double mDiff = 0;
        double mElapsed = 0;

        public SensorProcess(String sensor, double rate, double dur,
                             BufferedOutputStream bf) throws Exception  {
            mRate = rate;
            mDur = dur;
            mOut = bf;

            int maxreportdelay_us = Math.min((int) mDur * 1000 * 1000 / 2, LATENCY_US);

            /* TODO setting maxreport to 5minutes can get problematic later for ffmpeg */
            mSensor = getMatchingSensor(sensor);
            mSensor.registerListener(this, (int) (1 / rate * 1000 * 1000), maxreportdelay_us);
        }

        /** given a String tries to find a matching sensor given these rules:
         *
         *   1. find all sensors which string description (getStringType()) contains the *sensor*
         *   2. choose the shortest one of that list
         *
         *  e.g., when "gyro" is given, choose android.sensor.type.gyroscope rather than
         *  android.sensor.type.gyroscope_uncalibrated. Matching is case-insensitive.
         *
         * @param sensor sensor name to match for
         * @return the sensor for which the description is shortest and contains the *sensor*
         * @throws Exception when no or multiple matches are found
         */
        public Sensor getMatchingSensor(String sensor) throws Exception {
            LinkedList<Sensor> candidates = new LinkedList<>();

            for (Sensor s : Sensor.getAvailableSensors(getApplicationContext()))
                if (s.getStringType().toLowerCase().contains(sensor.toLowerCase()))
                    candidates.add(s);

            if (candidates.size() == 0) {
                StringBuilder b = new StringBuilder();
                for (Sensor s : Sensor.getAvailableSensors(getApplicationContext())) {
                    b.append(s.getStringType());
                    b.append("\n");
                }
                throw new Exception("no matches for " + sensor + " found."+
                                    "Options are: \n"+b.toString());
            }

            int minimum = Integer.MAX_VALUE;

            for (Sensor s : candidates)
                minimum = Math.min(minimum, s.getStringType().length());

            Iterator<Sensor> it = candidates.iterator();
            while(it.hasNext())
                if (it.next().getStringType().length() != minimum)
                    it.remove();

            if (candidates.size() != 1) {
                StringBuilder b = new StringBuilder();
                for (Sensor s : candidates) {
                    b.append(s.getStringType());
                    b.append(", ");
                }
                throw new Exception("too many sensor candidates for " + sensor +
                        " options are " + b.toString());
            }

            return candidates.getFirst();
        }

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            if (mBuf == null)
                mBuf = ByteBuffer.allocate(sensorEvent.values.length * 4 + 1 * 4 );

            if (mLastTimestamp == -1) {
                mLastTimestamp = sensorEvent.timestamp;
                return;
            }

            try {
                /*
                 * the sensorrate might not be constant, which is why we do a simple repetition
                 * interpolation here. I.e. we make sure that at least 1/rate seconds have passed
                 * between samples.
                 */
                assert( mLastTimestamp < sensorEvent.timestamp );
                mDiff += (sensorEvent.timestamp - mLastTimestamp) * 1e-9;

                /*
                 * transfer a sensor sample and the current accuracy measure
                 */
                mBuf.clear();
                for (float v : sensorEvent.values)
                    mBuf.putFloat(v);
                mBuf.putFloat(mAccuracy);

                /*
                 * store it or multiple copies of the same, close when done.
                 */
                while (mDiff >= 1/mRate) {
                    mDiff -= 1 / mRate;
                    mElapsed += 1 / mRate;

                    if (mElapsed > mDur+.5/mRate) {
                        terminate();
                        return;
                    } else
                        mOut.write(mBuf.array());
                }

                mLastTimestamp = sensorEvent.timestamp;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        @Override
        public void onAccuracyChanged(android.hardware.Sensor sensor, int i) {
            mAccuracy = (float) i;
        }

        private void terminate() throws IOException {
            mSensor.unregisterListener(this);
            mOut.close();
        }
    }

    public class Recording {
        final String mOutputPath;
        final ArrayList<SensorProcess> mInputList;
        final PowerManager mpm;
        final PowerManager.WakeLock mwl;
        final Handler mTimeout;

        public Recording(String output, double duration) {
            mOutputPath = output;
            mInputList = new ArrayList<SensorProcess>();

            mpm = (PowerManager) getSystemService(POWER_SERVICE);
            mwl = mpm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Integer.toString(this.hashCode()));

            /* have a timeout after the duration to flush the sensor and terminate the process
             * afterwards. This is to avoid sensor that do not report data continuously. For example
             * in the case of a failure condition */
            mTimeout = new Handler();
            mTimeout.postDelayed(mCallFlush, (long) (duration*1000) + 2000);
        }

        public void add(SensorProcess s) {
            if (mInputList.size() == 0)
                mwl.acquire();
            mInputList.add(s);
        }

        public void terminate() throws IOException {
            /* this is a special hack for completely failing sensors: terminate them as
             * soon as another process terminates. */
            for (Iterator<SensorProcess> it = mInputList.iterator(); it.hasNext();) {
                SensorProcess p = it.next();
                p.terminate();
                it.remove();
            }

            Intent i = new Intent(FINISH_ACTION);
            i.putExtra(FINISH_PATH, mOutputPath);
            sendBroadcast(i);
            mwl.release();
        }


        private Runnable mCallFlush = new Runnable() {
            @Override
            public void run() {
                for (SensorProcess p : mInputList)
                    p.mSensor.flush(p);

                // TODO 1sec for flushing? there is no way to know when flushing finished?!
                mTimeout.postDelayed(mCallTerminate, 1000);
            }
        };

        private final Runnable mCallTerminate = new Runnable() {
            @Override
            public void run() {
                try {
                    terminate();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
    }
}
