package de.uni_freiburg.es.sensorrecordingtool.sensors;

import android.content.Context;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * This is required since the Android sensor class is defined as final, so we can't create
 * our own sensors. Thumbs up!
 *
 * Created by phil on 3/1/16.
 */
public abstract class Sensor {
    protected final Context mContext;
    protected final LinkedList<ParameterizedListener> mListeners;
    protected SensorEvent mEvent;

    public Sensor(Context context, int num) {
        mContext = context;
        mListeners = new LinkedList<ParameterizedListener>();

        try {         // XXX yay for nice permissions
            Class<?> c = Class.forName(SensorEvent.class.getCanonicalName());
            Constructor<?> co = c.getDeclaredConstructors()[0];
            co.setAccessible(true);
            mEvent = (SensorEvent) co.newInstance(num);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public abstract String getStringType();

    public void flush(SensorEventListener l) {
        notifyListeners();
    }

    public static List<Sensor> getAvailableSensors(Context c) {
        LinkedList<Sensor> result = new LinkedList<Sensor>();
        SensorManager mgr = (SensorManager) c.getSystemService(Context.SENSOR_SERVICE);

        for (android.hardware.Sensor s : mgr.getSensorList(android.hardware.Sensor.TYPE_ALL))
            result.add(new SensorWrapper(c, s));

        result.add(new LocationSensor(c));
        result.add(new VideoSensor(c));
        //result.add(new AudioSensor(c));
        return result;
    }

    protected void onNewListener() {
    }

    protected void notifyListeners() {
        for (ParameterizedListener pl : mListeners)
            pl.l.onSensorChanged(mEvent);
    }

    public void registerListener(SensorEventListener l, int rate, int delay) {
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
