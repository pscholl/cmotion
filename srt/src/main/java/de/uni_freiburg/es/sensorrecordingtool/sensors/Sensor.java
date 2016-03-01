package de.uni_freiburg.es.sensorrecordingtool.sensors;

import android.content.Context;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.LinkedList;
import java.util.List;

/**
 * This is required since the Android sensor class is defined as final, so we can't create
 * our own sensors. Thumbs up!
 *
 * Created by phil on 3/1/16.
 */
public abstract class Sensor {
    public abstract String getStringType();
    public abstract void registerListener(SensorEventListener l, int rate, int delay);
    public abstract void unregisterListener(SensorEventListener l);
    public abstract void flush(SensorEventListener l);

    public static List<Sensor> getAvailableSensors(Context c) {
        LinkedList<Sensor> result = new LinkedList<>();
        SensorManager mgr = (SensorManager) c.getSystemService(Context.SENSOR_SERVICE);

        for (android.hardware.Sensor s : mgr.getSensorList(android.hardware.Sensor.TYPE_ALL))
            result.add(new SensorWrapper(c, s));

        result.add(new LocationSensor(c));
        return result;
    }
}
