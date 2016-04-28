package de.uni_freiburg.es.sensorrecordingtool.sensors;

import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.BlockSensorEvent;
import android.hardware.Camera;
import android.hardware.SensorEventListener;
import android.media.MediaRecorder;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.io.BufferedInputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.Selector;
import java.util.Timer;
import java.util.TimerTask;

import de.uni_freiburg.es.sensorrecordingtool.PermissionDialog;

/** Not wokring at all.
 *
 * Created by phil on 4/26/16.
 */
public class AudioSensor extends Sensor {
    protected static final String TAG = AudioSensor.class.getName();
    public static final String SENSOR_NAME = "audio";
    protected final Context context;
    private final MediaRecorder mRecord;
    private Timer mTimer;
    private int mRateinMus = 0;
    private FileInputStream mInputStream;

    public AudioSensor(Context c) {
        super(c, 1);
        context = c;
        mEvent = new BlockSensorEvent();
        mRecord = new MediaRecorder();
        mTimer = new Timer();
    }

    @Override
    public String getStringType() {
        return SENSOR_NAME;
    }

    @Override
    public void registerListener(SensorEventListener l, int rate_in_mus, int delay) {
        if (!PermissionDialog.camera(context))
            return;

        if (mListeners.size() == 0) {
            mRateinMus = rate_in_mus;
            startRecording();
        }

        super.registerListener(l,rate_in_mus,delay);
    }

    public void startRecording() {
        try {
            int sampling_rate = (1000*1000) / mRateinMus;
            ParcelFileDescriptor[] pipes = ParcelFileDescriptor.createPipe();
            mRecord.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
            mRecord.setOutputFormat(MediaRecorder.OutputFormat.AMR_WB);
            mRecord.setOutputFile(pipes[1].getFileDescriptor());
            mRecord.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
            mRecord.setAudioChannels(1);
            mRecord.setAudioSamplingRate(sampling_rate);
            mInputStream = new FileInputStream(pipes[0].getFileDescriptor());
            mTimer.cancel();
            mTimer = new Timer();
            mTimer.scheduleAtFixedRate(readInputStream, 500, mRateinMus/1000);
            ((BlockSensorEvent) mEvent).rawdata = new byte[sampling_rate];
            mRecord.prepare();
            mRecord.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private TimerTask readInputStream = new TimerTask() {
        @Override
        public void run() {
            try {
                BlockSensorEvent e = (BlockSensorEvent) mEvent;
                e.timestamp = System.currentTimeMillis() * 1000 * 1000;
                mInputStream.read(e.rawdata, 0, e.rawdata.length);
                notifyListeners();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    };

    public void stopRecording() {
        mRecord.stop();
        mRecord.release();
    }

    @Override
    public void unregisterListener(SensorEventListener l) {
        super.unregisterListener(l);

        if (mListeners.size() == 0)
            stopRecording();
    }
}
