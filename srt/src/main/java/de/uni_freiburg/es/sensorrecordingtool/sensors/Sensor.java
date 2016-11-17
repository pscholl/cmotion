package de.uni_freiburg.es.sensorrecordingtool.sensors;

import android.content.Context;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * This is required since the Android sensor class is defined as final, so we can't create
 * our own sensors. Thumbs up (why would you want that anyway)!
 *
 * Created by phil on 3/1/16.
 */
public abstract class Sensor {
    private static final String TAG = "Sensor";
    protected final Context mContext;
    protected final LinkedList<ParameterizedListener> mListeners;
    protected SensorEvent mEvent;

    public boolean isPrepared() {
        return isPrepared;
    }

    public void setPrepared(boolean prepared) {
        isPrepared = prepared;
    }

    protected boolean isPrepared = false;

    public Sensor(Context context, int num) {
        mContext = context;
        mListeners = new LinkedList<>();
        mEvent = new SensorEvent(4);
    }

    public void prepareSensor() {
        if(isPrepared)
            Log.w(TAG, "Sensor is already in prepared state, no need to prepare it again.");
    }

    public void setPrepared() {
        isPrepared = true;
        Log.e(TAG, "Sensor "+getStringType()+" prepared");

    }

    public void startRecording() {
    }

    public abstract String getStringType();

    public void flush(SensorEventListener l) {
        for (ParameterizedListener pl : mListeners)
            pl.l.onFlushCompleted();
    }

    public static List<Sensor> getAvailableSensors(Context c) {
        LinkedList<Sensor> result = new LinkedList<Sensor>();
        SensorManager mgr = (SensorManager) c.getSystemService(Context.SENSOR_SERVICE);

        for (android.hardware.Sensor s : mgr.getSensorList(android.hardware.Sensor.TYPE_ALL))
            result.add(new SensorWrapper(c, s));

        result.add(new LocationSensor(c));
        result.add(new VideoSensor(c));
        result.add(new AudioSensor(c));
        return result;
    }

    protected void onNewListener() {
        if(!isPrepared)
            throw new IllegalStateException("sensor was not prepared");
    }

    protected void notifyListeners() {
        for (ParameterizedListener pl : mListeners)
            pl.l.onSensorChanged(mEvent);
    }

    public void registerListener(SensorEventListener l, int rate, int delay, String format, Handler h) {
        mListeners.add(new ParameterizedListener(l, rate / 1000, delay / 1000));
    }

    public void unregisterListener(SensorEventListener l) {
        for(Iterator<ParameterizedListener> it = mListeners.iterator(); it.hasNext(); ) {
            ParameterizedListener pl = it.next();
            if (pl.l.equals(l)) it.remove();
        }
    }

    protected class ParameterizedListener {
        public ParameterizedListener(SensorEventListener li, int r, int d) {
            l = li;
            rate = r;
            delay = d;
        }

        SensorEventListener l;
        int rate, delay;
    }
}
