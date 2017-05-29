package de.uni_freiburg.es.sensorrecordingtool.sensors;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTimestamp;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

import de.uni_freiburg.es.sensorrecordingtool.PermissionDialog;

/**
 * Not wokring at all.
 * <p>
 * Created by phil on 4/26/16.
 */
public class AudioSensor extends Sensor {
    protected static final String TAG = AudioSensor.class.getName();
    protected final Context context;

    private int mChannelConfig, mSampleRate;
    private static int mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;

    private RecorderThread mRecorderThread;


    public AudioSensor(Context c, int channelConfig) {
        super(c, 1);
        context = c;
        assert channelConfig == AudioFormat.CHANNEL_IN_MONO || channelConfig == AudioFormat.CHANNEL_IN_STEREO;
        mChannelConfig = channelConfig;
    }

    @Override
    public void prepareSensor() {
        setPrepared();
    }


    @Override
    public String getStringName() {
        return String.format("Audio %s", mChannelConfig == AudioFormat.CHANNEL_IN_MONO ? "mono" : "stereo");
    }

    @Override
    public String getStringType() {
        return String.format("android.hardware.audio_%s", mChannelConfig == AudioFormat.CHANNEL_IN_MONO ? "mono" : "stereo");
    }

    @Override
    public void registerListener(SensorEventListener l, double rate, String format, Handler h) {
        if (!PermissionDialog.audio(context)) {
            Log.e(TAG, "no permission to record microphone");
            return;
        }

        Log.d(TAG, String.format("default rate is %d", getAudioSampleRate()));

        if (mListeners.size() == 0) {
            mSampleRate = (int) rate;
            if (mRecorderThread == null)
                mRecorderThread = new RecorderThread();
            mRecorderThread.startRecording();
        }

        super.registerListener(l, rate, format, h);
    }


    @Override
    public void unregisterListener(SensorEventListener l) {
        super.unregisterListener(l);

        if (mListeners.size() == 0)
            mRecorderThread.stopRecording();
    }

    @Override
    public int getFifoSize() {
        return 0;
    }


    /*
     * Valid Audio Sample rates
     *
     * @see <a
     * href="http://en.wikipedia.org/wiki/Sampling_%28signal_processing%29"
     * >Wikipedia</a>
     */
    private static final int validSampleRates[] = new int[]{8000, 11025, 16000, 22050,
            32000, 37800, 44056, 44100, 47250, 4800, 50000, 50400, 88200,
            96000, 176400, 192000, 352800, 2822400, 5644800};

    public static int getAudioSampleRate() {
        return getSupportedAudioSampleRates().get(0);
    }

    public static List<Integer> getSupportedAudioSampleRates() {
        List<Integer> list = new ArrayList<>();
        /*
        * Selecting default audio input source for recording since
        * AudioFormat.CHANNEL_CONFIGURATION_DEFAULT is deprecated and selecting
        * default encoding format.
         */

        for (int samplingRate : validSampleRates)
        try {
            int minBufSize = AudioRecord.getMinBufferSize(samplingRate,
                    AudioFormat.CHANNEL_IN_MONO, mAudioFormat);
            AudioRecord aud = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                    samplingRate, AudioFormat.CHANNEL_IN_MONO, mAudioFormat, minBufSize);
            list.add(samplingRate);
        } catch (Exception e) {}

        return list;
    }

    public int getChannels() {
        return mChannelConfig == AudioFormat.CHANNEL_IN_MONO ? 1 : 2;
    }

    class RecorderThread extends Thread {

        private boolean status = true;

        @Override
        public void run() {
            int minBufSize = AudioRecord.getMinBufferSize(mSampleRate, mChannelConfig, mAudioFormat);
            AudioRecord aud = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                    mSampleRate, mChannelConfig, mAudioFormat, minBufSize);
            AudioTimestamp ts = new AudioTimestamp();
            byte buf[] = new byte[minBufSize];

            for (aud.startRecording();
                 aud.getState() == AudioRecord.STATE_UNINITIALIZED;
                 aud.startRecording())
                _sleep(200);

            for (int err = aud.read(buf,0, minBufSize);
                 err >= 0 && status;
                 err = aud.read(buf, 0, minBufSize))
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    aud.getTimestamp(ts, AudioTimestamp.TIMEBASE_MONOTONIC);
                    mEvent.timestamp = ts.nanoTime;
                } else
                    mEvent.timestamp = System.currentTimeMillis() * 1000 * 1000;

                mEvent.rawdata = buf;
                notifyListeners();

                if (err == 0)
                    Log.d(TAG, "skipped block");
            }

            aud.stop();
            aud.release();
        }

        private void _sleep(int i) {
            try {
                Thread.sleep(i);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void startRecording() {
            status = true;
            this.start();
        }

        public void stopRecording() {
            status = false;
        }


    }
}
