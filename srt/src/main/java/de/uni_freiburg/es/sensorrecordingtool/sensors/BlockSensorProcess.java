package de.uni_freiburg.es.sensorrecordingtool.sensors;

import android.content.Context;

import java.io.BufferedOutputStream;
import java.io.OutputStream;

/**
 * Created by phil on 9/1/16.
 */
public class BlockSensorProcess extends SensorProcess {
    public BlockSensorProcess(Context c, String sensor, double rate, String format, double dur,
                              OutputStream bf) throws Exception {
        super(c, sensor, rate, format, dur, bf);
    }

    @Override
    public byte[] transfer(SensorEvent sensorEvent) {
        return sensorEvent.rawdata;
    }
}
