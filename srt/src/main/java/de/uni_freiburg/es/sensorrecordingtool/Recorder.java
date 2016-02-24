package de.uni_freiburg.es.sensorrecordingtool;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TimeZone;

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

    /* action for reporting error from the recorder service */
    static final String ERROR_ACTION  = "recorder_error";
    static final String ERROR_REASON  = "error_reaseon";
    static final String FINISH_ACTION = "recorder_done";
    static final String FINISH_PATH   = "recording_path";

    /** called with the startService routine, will parse all RECORDER_INPUTs for known values
     *  and start the recording. If no RECORDER_OUTPUT directory is given it will default to
     *  a directory named with the current timestamp in ISO-format on the sdcard.
     */
    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        String[] sensors = i.getStringArrayExtra(RECORDER_INPUT);
        double[] rates   = i.getDoubleArrayExtra(RECORDER_RATE);
        String output    = i.getStringExtra(RECORDER_OUTPUT);
        double duration  = i.getDoubleExtra(RECORDER_DURATION, 5);

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
         * matching the sensor input array, check whether length are macthing.
         */
        if (rates == null)
            rates = new double[] {i.getDoubleExtra(RECORDER_RATE, 50)};

        if (rates.length == 1) {
            rates = Arrays.copyOf(rates, sensors.length);
            Arrays.fill(rates, rates[0]);
        }

        if (rates.length != sensors.length)
            return error("either rates and sensors must be the same length or a single rate must be given");

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
        Recording r = new Recording(output);

        for (int j=0; j<rates.length && j<sensors.length; j++) {
            try {
                BufferedOutputStream bf =  new BufferedOutputStream(
                        new FileOutputStream(new File(output, sensors[j])));
                SensorProcess sp = new SensorProcess(sensors[j], rates[j], duration, bf, r);
            } catch (Exception e) {
                return error(sensors[j] + ": " + e.getLocalizedMessage());
            }
        }

        return START_NOT_STICKY;
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

        System.err.println(s);
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
     * Arguments:
     *  sensor - a string representation for the sensor, must match Android Sensor types
     *  dur    - a double seconds of recording duration
     *  rate   - double Hz for the recording, non-negative
     *  bf     - a bufferedwriter, where data gets written to in binary format.
     *
     */
    protected class SensorProcess implements SensorEventListener {
        public static final int LATENCY_US = 5 * 60 * 1000 * 1000;
        private final SensorManager msm;
        private final double mRate;
        private final BufferedOutputStream mOut;
        private final double mDur;
        private final Recording mRecording;
        private ByteBuffer mBuf;
        private long mLastTimestamp = -1;
        private double mDiff = 0;
        private double mElapsed = 0;

        public SensorProcess(String sensor, double rate, double dur, BufferedOutputStream bf, Recording r) throws Exception  {
            msm = (SensorManager) getSystemService(SENSOR_SERVICE);
            mRate = rate;
            mDur = dur;
            mOut = bf;
            mRecording = r;
            mRecording.add(this);

            /* TODO settting maxreport to 5minutes can get problematic later for ffmpeg */
            Sensor s = getMatchingSensor(sensor);
            if (!msm.registerListener(this, s, (int) (1 / rate * 1000 * 1000),
                         Math.min((int) mDur * 1000 * 1000 / 2, LATENCY_US)))
                throw new Exception("unable to register sensor " + sensor);
        }

        /** given a String tries to find a matching sensor given this ruleset:
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

            for (Sensor s : msm.getSensorList(Sensor.TYPE_ALL))
                if (s.getStringType().toLowerCase().contains(sensor.toLowerCase()))
                    candidates.add(s);

            if (candidates.size() == 0)
                throw new Exception("no matches for " + sensor + " found");

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
                throw new Exception("too many sensor candidates for " + sensor + " options are " +
                        b.toString());
            }

            return candidates.getFirst();
        }

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            if (mBuf == null)
                mBuf = ByteBuffer.allocate(sensorEvent.values.length * 4);

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
                 * transfer a sensor sample
                 */
                mBuf.clear();
                for (float v : sensorEvent.values)
                    mBuf.putFloat(v);

                /*
                 * store it or multiple copies of the same, close when done.
                 */
                while (mDiff >= 1/mRate) {
                    mDiff -= 1 / mRate;
                    mElapsed += 1 / mRate;

                    if (mElapsed > mDur+.5/mRate) {
                        msm.unregisterListener(this);
                        mOut.close();
                        mRecording.remove(this);
                    } else
                        mOut.write(mBuf.array());
                }

                mLastTimestamp = sensorEvent.timestamp;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    }

    private class Recording {
        private final String mOutputPath;
        private final ArrayList<SensorProcess> mInputList;
        private final PowerManager mpm;
        private final PowerManager.WakeLock mwl;

        public Recording(String output) {
            mOutputPath = output;
            mInputList = new ArrayList<SensorProcess>();

            mpm = (PowerManager) getSystemService(POWER_SERVICE);
            mwl = mpm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Integer.toString(this.hashCode()));
        }

        public void add(SensorProcess s) {
            if (mInputList.size() == 0)
                mwl.acquire();
            mInputList.add(s);
        }

        public void remove(SensorProcess s) {
            if(!mInputList.remove(s))
                return;
            
            if (mInputList.size() == 0) {
                Intent i = new Intent(FINISH_ACTION);
                i.putExtra(FINISH_PATH, mOutputPath);
                sendBroadcast(i);
                mwl.release();
            }
        }
    }
}
