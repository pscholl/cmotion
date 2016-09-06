package de.uni_freiburg.es.sensorrecordingtool.sensors;

import android.content.Context;
import android.os.Build;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedList;

import de.uni_freiburg.es.sensorrecordingtool.RecordingProcess;
import de.uni_freiburg.es.sensorrecordingtool.sensors.Sensor;
import de.uni_freiburg.es.sensorrecordingtool.sensors.SensorEvent;
import de.uni_freiburg.es.sensorrecordingtool.sensors.SensorEventListener;

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
public class SensorProcess implements SensorEventListener {
    public static final int DEFAULT_LATENCY_US = 0;
    private final Context context;
    final Sensor mSensor;
    final double mRate;
    final OutputStream mOut;
    public final double mDur;
    ByteBuffer mBuf;
    long mLastTimestamp = -1;
    double mDiff = 0;
    public double mElapsed = 0;

    public SensorProcess(Context context, String sensor, double rate, String format, double dur,
                         OutputStream bf) throws Exception  {
        this.context = context;
        mRate = rate;
        mDur = dur;
        mOut = bf;

        int maxreportdelay_us = DEFAULT_LATENCY_US;
        if ( mDur-1 > 0 ) // make it one second shorter
            maxreportdelay_us = (int) (mDur-1.) * 1000 * 1000;

        mSensor = getMatchingSensor(context, sensor);
        mSensor.registerListener(this, (int) (1 / rate * 1000 * 1000), maxreportdelay_us, format);
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
    public static Sensor getMatchingSensor(Context context, String sensor) throws Exception {
        LinkedList<Sensor> candidates = new LinkedList<Sensor>();

        for (Sensor s : Sensor.getAvailableSensors(context))
            if (s.getStringType().toLowerCase().contains(sensor.toLowerCase()))
                candidates.add(s);

        if (candidates.size() == 0) {
            StringBuilder b = new StringBuilder();
            for (Sensor s : Sensor.getAvailableSensors(context)) {
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
            while (mDiff >= 1/mRate) {
                mDiff -= 1 / mRate;
                mElapsed += 1 / mRate;

                if (mDur > 0 && mElapsed > mDur+.5/mRate) {
                    terminate();
                    return;
                } else
                    mOut.write(arr);
            }

            mLastTimestamp = sensorEvent.timestamp;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public byte[] transfer(SensorEvent sensorEvent) {
        if (mBuf == null)
            mBuf = ByteBuffer.allocate(sensorEvent.values.length * 4);
        else
            mBuf.clear();

        for (float v : sensorEvent.values)
            mBuf.putFloat(v);

        return mBuf.array();
    }

    public void terminate() throws IOException {
        if (mDur < mElapsed || mDur < 0)
            mSensor.flush(this);
        else
            onFlushCompleted();
    }

    @Override
    public void onFlushCompleted() {
        mSensor.unregisterListener(this);
        try { mOut.close();}
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int getSampleSize(Context context, String sensor) throws Exception {
        Sensor s = getMatchingSensor(context, sensor);

        if (s instanceof SensorWrapper) {
            Method m = android.hardware.Sensor.class.getDeclaredMethod("getMaxLengthValuesArray",
                        new Class[]{android.hardware.Sensor.class, int.class});
            m.setAccessible(true);
            return (int) m.invoke(null, ((SensorWrapper) s).mSensor, Build.VERSION.SDK_INT);
        }

        throw new Exception("unknown sensor: " + sensor);
    }
}
