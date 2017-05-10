package de.uni_freiburg.es.sensorrecordingtool.sensors;

import android.content.Context;
import android.os.Handler;

import java.io.OutputStream;
import java.nio.ByteBuffer;

import de.uni_freiburg.es.sensorrecordingtool.FFMpegProcess;

/**
 * Created by phil on 9/1/16.
 */
public class NonBlockSensorProcess extends SensorProcess {
    public NonBlockSensorProcess(Context c, String sensor, double rate, String format, double dur,
                                 OutputStream bf) throws Exception {
        super(c, sensor, rate, format, dur, bf, new Handler(c.getMainLooper()));
    }

    public NonBlockSensorProcess(Context c, String sensor, double rate, String format, double dur, OutputStream os, Handler handler) throws Exception {
        super(c, sensor, rate, format, dur, os, handler);
    }

    public NonBlockSensorProcess(Context c, String sensor, double rate, String format, double dur, FFMpegProcess p, int j, Handler handler) throws Exception {
        super(c, sensor, rate, format, dur, p, j, handler);
    }

    @Override
    public byte[] transfer(SensorEvent sensorEvent) {
        if (mBuf == null)
            mBuf = ByteBuffer.allocate(sensorEvent.values.length * 4);
        else
            mBuf.clear();

        for (float v : sensorEvent.values)
            mBuf.putFloat(v);

        return mBuf.array();
    }
}
