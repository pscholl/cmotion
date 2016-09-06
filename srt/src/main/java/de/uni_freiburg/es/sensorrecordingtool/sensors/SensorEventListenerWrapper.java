package de.uni_freiburg.es.sensorrecordingtool.sensors;

import android.hardware.*;
import android.hardware.Sensor;
import android.hardware.SensorEvent;

/** FUBAR++ pn Android API19 for definining all classes in android.hardware.* final.
 *
 * Created by phil on 4/29/16.
 */
public class SensorEventListenerWrapper implements android.hardware.SensorEventListener2 {
    private final de.uni_freiburg.es.sensorrecordingtool.sensors.SensorEventListener mListener;
    private final de.uni_freiburg.es.sensorrecordingtool.sensors.SensorEvent mEvent;

    public SensorEventListenerWrapper(de.uni_freiburg.es.sensorrecordingtool.sensors.SensorEventListener l) {
        mListener = l;
        mEvent = new de.uni_freiburg.es.sensorrecordingtool.sensors.SensorEvent(0);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        mEvent.values = sensorEvent.values;
        mEvent.timestamp = sensorEvent.timestamp;
        mListener.onSensorChanged(mEvent);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    @Override
    public void onFlushCompleted(Sensor sensor) {
        System.err.println(String.format("%s %S complete flush", this.toString(), sensor.toString()));
        mListener.onFlushCompleted();
    }
}
