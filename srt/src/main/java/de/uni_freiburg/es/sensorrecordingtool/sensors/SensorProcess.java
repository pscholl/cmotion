package de.uni_freiburg.es.sensorrecordingtool.sensors;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

import de.uni_freiburg.es.sensorrecordingtool.FFMpegProcess;


/**
 * Copies *sensor* data for *dur* seconds at *rate* to a bufferedwriter *bf*. Automatically
 * closes the output buffer when done.
 * <p>
 * This is all done in binary float format, all the channels are recorded and the current
 * accuracy of the process. For example the accelerometer reports all three axes, so one sample
 * is 3 (channels) * 4 (bytes per float) + 4 (bytes for accuracy as float).
 * <p>
 * Arguments:
 * sensor - a string representation for the sensor, must match Android Sensor types
 * dur    - a double seconds of recording duration
 * rate   - double Hz for the recording, non-negative
 * bf     - a bufferedwriter, where data gets written to in binary format.
 */
public abstract class SensorProcess implements SensorEventListener {
    /**
     * when no duration is set this constant is used for the maximum delay to report
     * on new sensor data. We chose ten minutes for no specific reason.
     */
    public static final int DEFAULT_LATENCY_US = 10 * 1000 * 1000;
    final Sensor mSensor;
    final double mRate;
    OutputStream mOut = null;
    public final double mDur;
    private final PowerManager.WakeLock mWl = null;
    private final String mFormat;
    private final Handler mHandler;
    ByteBuffer mBuf;
    long mLastTimestamp = -1;
    public double mElapsed = 0;
    double mDiff = 0;
    private boolean isClosed = false;

    private HandlerThread mHandlerThread;

    public SensorProcess(Context context, String sensor, double rate, String format, double dur,
                         OutputStream bf, Handler h) throws Exception {
        mRate = rate;
        mDur = dur;
        mOut = bf;
        mFormat = format;
        mHandler = h;

        mSensor = getMatchingSensor(context, sensor);
        mSensor.prepareSensor();
    }

    public SensorProcess(Context c, String sensor, double rate, String format, double dur,
                         final FFMpegProcess p, final int j, Handler handler) throws Exception {
        mRate = rate;
        mDur = dur;
        mFormat = format;
        mHandler = handler;

        mSensor = getMatchingSensor(c, sensor);
        mSensor.prepareSensor();

        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mOut = p.getOutputStream(j);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    public Sensor getSensor() {
        return mSensor;
    }

    public void startRecording() {
        mSensor.registerListener(this, mRate, mFormat, mHandler);
    }

    /**
     * given a String tries to find a matching sensor given these rules:
     * <p>
     * 1. find all sensors which string description (getStringName()) contains the *sensor*
     * 2. choose the shortest one of that list
     * <p>
     * e.g., when "gyro" is given, choose android.sensor.type.gyroscope rather than
     * android.sensor.type.gyroscope_uncalibrated. Matching is case-insensitive.
     *
     * @param sensor sensor name to match for
     * @return the sensor for which the description is shortest and contains the *sensor*
     * @throws Exception when no or multiple matches are found
     */
    public static Sensor getMatchingSensor(Context context, String sensor) throws Exception {
        LinkedList<Sensor> candidates = new LinkedList<Sensor>();

        for (Sensor s : Sensor.getAvailableSensors(context))
            if (s.equals(sensor))
                candidates.add(s);

        if (candidates.size() == 0) {
            StringBuilder b = new StringBuilder();
            for (Sensor s : Sensor.getAvailableSensors(context)) {
                b.append(s.getStringName());
                b.append(" ");
                b.append(s.getStringType());
                b.append("\n");
            }
            throw new Exception("no matches for " + sensor + " found." +
                    "Options are: \n" + b.toString());
        }

        int minimum = Integer.MAX_VALUE;

        for (Sensor s : candidates)
            minimum = Math.min(minimum, s.getStringName().length());

        Iterator<Sensor> it = candidates.iterator();
        while (it.hasNext())
            if (it.next().getStringName().length() != minimum)
                it.remove();

        if (candidates.size() != 1) {
            StringBuilder b = new StringBuilder();
            for (Sensor s : candidates) {
                b.append(s.getStringName());
                b.append(" ");
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

        if (mLastTimestamp == -1) {
            mLastTimestamp = sensorEvent.timestamp;
            return;
        }

        if (mOut == null)
            return;

        try {
            /*
             * the sensorrate might not be constant, which is why we do a simple repetition
             * interpolation here. I.e. we make sure that at least 1/rate seconds have passed
             * between samples.
             */
            assert (mLastTimestamp < sensorEvent.timestamp);
            mDiff += (sensorEvent.timestamp - mLastTimestamp) * 1e-9;

            if (mDur > 0 && mElapsed > mDur) {
                terminate();
                return;
            }

            /*
             * transfer a sensor sample and the current accuracy measure
             */
            byte[] arr = transfer(sensorEvent);

            /*
             * store it or multiple copies of the same, close when done.
             */
            int tointerpolate = (int) Math.floor( mDiff * mRate ) - 1;

            if (tointerpolate > 1 && !(mSensor instanceof  AudioSensor))
                Log.d("SensorProcess", String.format("%s interpolating %d frames", mSensor.getStringName(), tointerpolate));

            while (mDiff >= 1. / mRate) {
                mOut.write(arr);
                mDiff -= 1. / mRate;
                mElapsed += 1. / mRate;

                if (mDur > 0 && mElapsed > mDur + .5 / mRate) {
                    terminate();
                    return;
                }

                /** audio sensor does not provide timestamps, so interpolating can not work */
                if (mSensor instanceof AudioSensor)
                    break;
            }

            mLastTimestamp = sensorEvent.timestamp;
        } catch (IOException e) {
            e.printStackTrace();
            //terminate();
        }
    }

    public abstract byte[] transfer(SensorEvent sensorEvent);

    public void terminate() {

        if (Thread.currentThread() == Looper.getMainLooper().getThread())
            Log.wtf("SensorProcess", "Terminate called on UI Thread!!!");

        // XXX avoid flushing completly, as on LOLLIPOP no onFlushCompleted() is called?
        if (mElapsed < mDur || mDur < 0) {
            mSensor.flush(SensorProcess.this);
            while (!isClosed) Thread.currentThread().yield();
        } else
            onFlushCompleted();
    }

    @Override
    public void onFlushCompleted() {
        if (mWl != null && mWl.isHeld())
            mWl.release();

        mSensor.unregisterListener(this);
        try { mOut.close(); }
        catch (IOException e) {}

        Looper wtf = mHandlerThread.getLooper();
        if (wtf != null)
            wtf.quit();

        mHandlerThread.interrupt();
        isClosed = true;
    }

    public static int getSampleSize(Context context, String sensor) throws Exception {
        Sensor s = getMatchingSensor(context, sensor);

        if (s instanceof SensorWrapper) {
            Method m = android.hardware.Sensor.class.getDeclaredMethod("getMaxLengthValuesArray",
                    new Class[]{android.hardware.Sensor.class, int.class});
            m.setAccessible(true);
            return (int) m.invoke(null, ((SensorWrapper) s).mSensor, Build.VERSION.SDK_INT);
        } else if (s instanceof LocationSensor)
            return 4;

        throw new Exception("unknown sensor: " + sensor);
    }

    public void setHandlerThread(HandlerThread handlerThread) {
        this.mHandlerThread = handlerThread;
    }
}
