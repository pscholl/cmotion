package de.uni_freiburg.es.sensorrecordingtool.sensors;

import android.content.Context;
import android.hardware.*;

/**
 * A wrapper for the Android Sensor class.
 *
 * Created by phil on 3/1/16.
 */
public class SensorWrapper extends Sensor {
    protected final android.hardware.Sensor mSensor;
    protected final SensorManager mSensorMgr;

    public SensorWrapper(Context c, android.hardware.Sensor s) {
        mSensorMgr = (SensorManager) c.getSystemService(Context.SENSOR_SERVICE);
        mSensor = s;
    }

    @Override
    public String getStringType() {
        return mSensor.getStringType();
    }

    @Override
    public void registerListener(SensorEventListener l, int rate, int delay) {
        mSensorMgr.registerListener(l, mSensor, rate, delay);

    }

    @Override
    public void unregisterListener(SensorEventListener l) {
        mSensorMgr.unregisterListener(l);
    }

    @Override
    public void flush(SensorEventListener l) { mSensorMgr.flush(l);  }
}
