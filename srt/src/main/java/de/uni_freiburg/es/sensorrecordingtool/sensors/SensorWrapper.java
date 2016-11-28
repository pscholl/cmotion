package de.uni_freiburg.es.sensorrecordingtool.sensors;

import android.content.Context;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;

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
        mSensorWrapper = new HashMap<>();
    }


    @Override
    public void prepareSensor() {
        setPrepared();
    }

    @Override
    public void startRecording() {

    }

    @Override
    public String getStringName() {
        return mSensor.getName();
    }

    @Override
    public String getStringType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            return mSensor.getStringType();
        } else {
            String mStringType;
            
            switch (mSensor.getType()) {
                case android.hardware.Sensor.TYPE_ACCELEROMETER:
                    mStringType =  android.hardware.Sensor.STRING_TYPE_ACCELEROMETER;
                    break;
                case android.hardware.Sensor.TYPE_AMBIENT_TEMPERATURE:
                    mStringType =  android.hardware.Sensor.STRING_TYPE_AMBIENT_TEMPERATURE;
                    break;
                case android.hardware.Sensor.TYPE_GAME_ROTATION_VECTOR:
                    mStringType =  android.hardware.Sensor.STRING_TYPE_GAME_ROTATION_VECTOR;
                    break;
                case android.hardware.Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                    mStringType =  android.hardware.Sensor.STRING_TYPE_GEOMAGNETIC_ROTATION_VECTOR;
                    break;
//                case android.hardware.Sensor.TYPE_GLANCE_GESTURE:
//                    mStringType =  android.hardware.Sensor.STRING_TYPE_GLANCE_GESTURE;
//                    break;
                case android.hardware.Sensor.TYPE_GRAVITY:
                    mStringType =  android.hardware.Sensor.STRING_TYPE_GRAVITY;
                    break;
                case android.hardware.Sensor.TYPE_GYROSCOPE:
                    mStringType =  android.hardware.Sensor.STRING_TYPE_GYROSCOPE;
                    break;
                case android.hardware.Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                    mStringType =  android.hardware.Sensor.STRING_TYPE_GYROSCOPE_UNCALIBRATED;
                    break;
                case android.hardware.Sensor.TYPE_HEART_RATE:
                    mStringType =  android.hardware.Sensor.STRING_TYPE_HEART_RATE;
                    break;
                case android.hardware.Sensor.TYPE_LIGHT:
                    mStringType =  android.hardware.Sensor.STRING_TYPE_LIGHT;
                    break;
                case android.hardware.Sensor.TYPE_LINEAR_ACCELERATION:
                    mStringType =  android.hardware.Sensor.STRING_TYPE_LINEAR_ACCELERATION;
                    break;
                case android.hardware.Sensor.TYPE_MAGNETIC_FIELD:
                    mStringType =  android.hardware.Sensor.STRING_TYPE_MAGNETIC_FIELD;
                    break;
                case android.hardware.Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                    mStringType =  android.hardware.Sensor.STRING_TYPE_MAGNETIC_FIELD_UNCALIBRATED;
                    break;
//                case android.hardware.Sensor.TYPE_PICK_UP_GESTURE:
//                    mStringType =  android.hardware.Sensor.STRING_TYPE_PICK_UP_GESTURE;
//                    break;
                case android.hardware.Sensor.TYPE_PRESSURE:
                    mStringType =  android.hardware.Sensor.STRING_TYPE_PRESSURE;
                    break;
                case android.hardware.Sensor.TYPE_PROXIMITY:
                    mStringType =  android.hardware.Sensor.STRING_TYPE_PROXIMITY;
                    break;
                case android.hardware.Sensor.TYPE_RELATIVE_HUMIDITY:
                    mStringType =  android.hardware.Sensor.STRING_TYPE_RELATIVE_HUMIDITY;
                    break;
                case android.hardware.Sensor.TYPE_ROTATION_VECTOR:
                    mStringType =  android.hardware.Sensor.STRING_TYPE_ROTATION_VECTOR;
                    break;
                case android.hardware.Sensor.TYPE_SIGNIFICANT_MOTION:
                    mStringType =  android.hardware.Sensor.STRING_TYPE_SIGNIFICANT_MOTION;
                    break;
                case android.hardware.Sensor.TYPE_STEP_COUNTER:
                    mStringType =  android.hardware.Sensor.STRING_TYPE_STEP_COUNTER;
                    break;
                case android.hardware.Sensor.TYPE_STEP_DETECTOR:
                    mStringType =  android.hardware.Sensor.STRING_TYPE_STEP_DETECTOR;
                    break;
//                case android.hardware.Sensor.TYPE_TILT_DETECTOR:
//                    mStringType =  android.hardware.Sensor.STRING_TYPE_TILT_DETECTOR;
//                    break;
//                case android.hardware.Sensor.TYPE_WAKE_GESTURE:
//                    mStringType =  android.hardware.Sensor.STRING_TYPE_WAKE_GESTURE;
//                    break;
                case android.hardware.Sensor.TYPE_ORIENTATION:
                    mStringType =  android.hardware.Sensor.STRING_TYPE_ORIENTATION;
                    break;
                case android.hardware.Sensor.TYPE_TEMPERATURE:
                    mStringType =  android.hardware.Sensor.STRING_TYPE_TEMPERATURE;
                    break;
                default:
                    mStringType = "unknown";
            }
            return mStringType;
        }
    }

    @Override
    public void registerListener(SensorEventListener l, int rate, int delay, String format, Handler h) {
        SensorEventListenerWrapper wl = new SensorEventListenerWrapper(l);
        mSensorMgr.registerListener(get(l), mSensor, rate, delay, h);
    }

    @Override
    public void unregisterListener(SensorEventListener l) {
        mSensorMgr.unregisterListener(pop(l));
    }

    /** Determine whethe the code is runnong on Google Glass
     * @return True if and only if Manufacturer is Google and Model begins with Glass
     */
    public boolean isRunningOnGlass() {
        return "Google".equalsIgnoreCase(Build.MANUFACTURER) && Build.MODEL.startsWith("Glass");
    }

    @Override
    public void flush(SensorEventListener l) {
        if (isRunningOnGlass())
            l.onFlushCompleted();
        else
            mSensorMgr.flush(get(l));
    }

    protected android.hardware.SensorEventListener2 get(SensorEventListener l) {
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
