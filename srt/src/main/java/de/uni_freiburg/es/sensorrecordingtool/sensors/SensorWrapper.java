package de.uni_freiburg.es.sensorrecordingtool.sensors;

import android.content.Context;
import android.hardware.SensorManager;

import java.util.HashMap;

/**
 * A wrapper for the Android Sensor class.
 *
 * Created by phil on 3/1/16.
 */
public class SensorWrapper extends Sensor {
    protected final android.hardware.Sensor mSensor;
    protected final SensorManager mSensorMgr;
    protected final HashMap<SensorEventListener, SensorEventListenerWrapper> mSensorWrapper;

    public SensorWrapper(Context c, android.hardware.Sensor s) {
        super(c,0);
        mSensorMgr = (SensorManager) c.getSystemService(Context.SENSOR_SERVICE);
        mSensor = s;
        mSensorWrapper = new HashMap<SensorEventListener, SensorEventListenerWrapper>();
    }


    @Override
    public String getStringType() {
        return mSensor.getName();
    }

    @Override
    public void registerListener(SensorEventListener l, int rate, int delay, String format) {
        SensorEventListenerWrapper wl = new SensorEventListenerWrapper(l);
        mSensorMgr.registerListener(get(l), mSensor, rate, delay);
    }

    @Override
    public void unregisterListener(SensorEventListener l) {
        mSensorMgr.unregisterListener(pop(l));
    }

    @Override
    public void flush(SensorEventListener l) { mSensorMgr.flush(get(l));  }

    protected android.hardware.SensorEventListener get(SensorEventListener l) {
        SensorEventListenerWrapper wl = mSensorWrapper.get(l);
        if (wl == null) {
            wl = new SensorEventListenerWrapper(l);
            mSensorWrapper.put(l, wl);
        }
        return wl;
    }

    protected android.hardware.SensorEventListener pop(SensorEventListener l) {
        SensorEventListenerWrapper wl = mSensorWrapper.get(l);
        mSensorWrapper.remove(l);
        return wl;
    }
}
