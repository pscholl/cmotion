package de.uni_freiburg.es.sensorrecordingtool.sensors;

/**
 * Created by phil on 4/29/16.
 */
public interface SensorEventListener {
    public void onSensorChanged(SensorEvent e);
    public void onFlushCompleted();
}
