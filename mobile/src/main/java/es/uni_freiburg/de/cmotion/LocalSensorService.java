package es.uni_freiburg.de.cmotion;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Reads local sensors and transports them via UDP.
 *
 * Created by phil on 1/4/16.
 */
public class LocalSensorService extends Service  implements SensorEventListener {

    private static final String TAG = LocalSensorService.class.getName();
    private SensorManager mSensorManager;
    private int mID = 0;
    private long mStarttime;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mStarttime = System.currentTimeMillis();

        /*
         * register a sensor listener for the local sensors
         */
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorManager.registerListener((SensorEventListener) this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_GAME, 0);


        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        mSensorManager.unregisterListener(this);
        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        /*
         * add bytebuffers to the queue continuously. The packet is made of a
         *  - 32bit unsigned int timestamp, with ms since start of the service
         *  - 32bit unsigned int sensor id
         *  - 4 floats of quaternion data.
         */
        float[] rot = new float[4];
        SensorManager.getQuaternionFromVector(rot, sensorEvent.values);

        UDPTransport.getInstance().send(
          ByteBuffer.allocate(6 * 4).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(mID)
            .putInt((int) (System.currentTimeMillis() - mStarttime))
            .putFloat(rot[0]) // q
            .putFloat(rot[1]) // x
            .putFloat(rot[2]) // y
            .putFloat(rot[3]) // z
            .array());
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
