package de.uni_freiburg.es.sensorrecordingtool.sensors;

/**
 * Created by phil on 4/29/16.
 */
public class SensorEvent {
    public float[] values;
    public byte[] rawdata;
    public long timestamp;

    public SensorEvent(int i) {
        values = new float[i];
    }
}
