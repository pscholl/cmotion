package de.uni_freiburg.es.sensorrecordingtool;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.os.Vibrator;
import android.util.Log;

import java.io.OutputStream;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.List;

import de.uni_freiburg.es.intentforwarder.ForwardedUtils;
import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.AutoDiscovery;
import de.uni_freiburg.es.sensorrecordingtool.sensors.AudioSensor;
import de.uni_freiburg.es.sensorrecordingtool.sensors.BlockSensorProcess;
import de.uni_freiburg.es.sensorrecordingtool.sensors.NonBlockSensorProcess;
import de.uni_freiburg.es.sensorrecordingtool.sensors.SensorProcess;
import de.uni_freiburg.es.sensorrecordingtool.sensors.VideoSensor;

/**
 * A tool for recording arbitrary combinations of sensor attached and reachable via Android. The
 * idea is to run this service with a single Intent call, similar to how videos or images are
 * captured. This makes it also possible to start and stop recording via the adb shell. For example
 * if you would want to record the accelerometer and the orientation at 50Hz you can do so with
 * the following Intent:
 * <p>
 * Intent i = new Intent(Recorder.RECORD_ACTION);
 * i.putString('-i', ['accelerometer', 'orientation']);
 * i.putFloat('-r', 50.0);
 * sendBroadcast(i);
 * <p>
 * or from the adb shell:
 * <p>
 * adb shell am broadcast -a senserec -e -i accelerometer
 * <p>
 * Supported arguments are:
 * <p>
 * -i [String or list of String]
 * sensors to record, providing an unknown one will print a list on logcat
 * <p>
 * -r [int/float or list of int/float]
 * recording rate of each input, or if only a single value is given the rate for all sensors
 * <p>
 * -o [String]
 * output directory under /sdcard/DCIM under which the recordings are stored
 * <p>
 * -f [single string or list of strings]
 * list of string specifying the sensor format for each input, null to use the default,
 * currently only the video sensor has any specs, which is the recording size given as
 * widthxheight, e.g. 1280x720.
 * <p>
 * A Broadcast Intent is sent once the recording is started or canceled. The latest recording
 * can be canceled with the senserec_cancel broadcast action, e.g.:
 * <p>
 * adb shell am broadcast -a senserec_cancel
 * <p>
 * it is also possible to cancel a specific recording by supplying its id:
 * <p>
 * adb shell am broadcast -a senserec_cancel -r <id>
 * <p>
 * Created by phil on 2/22/16.
 */
public class Recorder extends IntentService {
    static final String TAG = Recorder.class.getName();

    /* designate the sensor you want to record, can either be a single String or a list thereof,
     * possible values are the String identifier of Android sensors, visible here:
      * https://developer.android.com/reference/android/hardware/Sensor.html
      *
      * Accelerometer for example is android.sensor.accelerometer, you can leave the android.sensor
      * prefix out. */
    public static final String RECORDER_INPUT = "-i";

    /* the rate at which to record, a single one will apply to all input. If multiple sensors are
     * given, multiple rates can be applied to each input. Default rate is 50Hz */
    public static final String RECORDER_RATE = "-r";

    /* the duration of the recording, given in seconds, default is 10 seconds. */
    public static final String RECORDER_DURATION = "-d";

    /* the optional output path */
    public static final String RECORDER_OUTPUT = "-o";

    /* optional format specifier for each sensor */
    public static final String RECORDER_FORMAT = "-f";

    /* the main action for recording */
    public static final String RECORD_ACTION = ForwardedUtils.RECORD_ACTION;

    public static final String READY_ACTION = ForwardedUtils.READY_ACTION;
    public static final String STEADY_ACTION = ForwardedUtils.STEADY_ACTION;
    public static final String DISCOVERY_ACTION = "senserec_discovery";
    public static final String DISCOVERY_RESPONSE_ACTION = "senserec_discovery_response";

    public static final String SHOWUI_ACTION = "senserec_showui";

    /* for handing over the cancel action from a notification */
    public static final String CANCEL_ACTION = "senserec_cancel";

    /* whether we are currently recording */
    public static boolean mIsRecording = false;
    public static long mRecordingSince = -1;

    public static final long DEFAULT_STEADY_TIME = 3000;

    public static int SEMAPHORE = 0;
    public static boolean isMaster;

    private AutoDiscovery mAutoDiscovery;

    /* members when a recording is ongoing, stored here for cleanup from
     * onDestroy */
    private List<SensorProcess> sensorProcesses;
    private PowerManager.WakeLock mWl;
    private RecorderStatus status;
    private FFMpegProcess ffmpeg;
    private String output;

    public Recorder() {
        super(Recorder.class.getName());
    }

    private SensorProcess newSensorProcess(String sensor, String format, double rate,
                                           double dur, OutputStream os) throws Exception {
        Context c = this.getApplicationContext();

        /** one thread for each sensor to make sure that they can block on write on their
         * respective delivery outputstream without interacting with any other sensorprocesses */
        HandlerThread h = new HandlerThread("sensorprocess " + sensor);
        h.start();

        if (sensor.contains("video") || sensor.contains("audio"))
            return new BlockSensorProcess(c, sensor, rate, format, dur, os,
                    new Handler(h.getLooper()));
        else
            return new NonBlockSensorProcess(c, sensor, rate, format, dur, os,
                    new Handler(h.getLooper()));

    }


    /**
     * just start a single recording until the flag is toggled or the duration has been hit.
     *
     * @param intent recording specification
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        if (!RECORD_ACTION.equals(intent.getAction())) {
            Log.d(TAG, String.format(
                    "not a %s action, not doing anything", RECORD_ACTION));
            return;
        }

        isMaster = !isIntentForwarded(intent);
        Log.e("MASTER????", isMaster + "");

//        if(isMaster) {
//            SEMAPHORE = mAutoDiscovery.getConnectedNodes();
//        } else {
        SEMAPHORE = 0; // use semaphore for steady command
//        }

        try {
            intent = RecorderCommands.parseRecorderIntent(intent);

            output = intent.getStringExtra(RECORDER_OUTPUT);
            String[] sensors = intent.getStringArrayExtra(RECORDER_INPUT);
            String[] formats = intent.getStringArrayExtra(RECORDER_FORMAT);
            double[] rates = intent.getDoubleArrayExtra(RECORDER_RATE);
            final double duration = intent.getDoubleExtra(RECORDER_DURATION, -1);

            /** create an ffmpeg process that will demux all sensor recordings into
             * a single file on one time axis. */
            FFMpegProcess.Builder fp = new FFMpegProcess.Builder();
            fp.setOutput(output, "matroska")
                    .setCodec("a", "wavpack")
                    .setCodec("v", "libx264")
                    .addOutputArgument("-preset", "ultrafast")
                    .setLoglevel("debug");

            /** create a SensorProcess for each input and wire it to ffmpeg
             * accordingly */
            for (int j = 0; j < sensors.length; j++) {
                if (SensorProcess.getMatchingSensor(this, sensors[j]) instanceof VideoSensor) {
                    VideoSensor.CameraSize size = VideoSensor.getCameraSize(formats[j]);

                    fp
                            .addVideo(size.width, size.height, rates[j], "rawvideo", "nv21")
                            .setStreamTag("name", "Android Default Cam");
                } else if (sensors[j].contains("audio")) {
                    Log.i(TAG, "Endianess " + ByteOrder.nativeOrder());
                    fp
                            .addAudio(ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? "s16le" : "s16be", AudioSensor.getAudioSampleRate(), 1) // native endian!
                            .setStreamTag("name", sensors[j]);
                } else
                    fp
                            .addAudio("f32be", rates[j], SensorProcess.getSampleSize(this, sensors[j]))
                            .setStreamTag("name", sensors[j]);
            }

            sensorProcesses = new LinkedList<>();
            ffmpeg = fp.build(this);

            /** create sensorprocess for each input and wire it to the ffmpeg process */
            for (int j = 0; j < sensors.length; j++)
                sensorProcesses.add(newSensorProcess(
                        sensors[j], formats[j], rates[j], duration, ffmpeg.getOutputStream(j)));

            /** notify the system that a new recording was started, and make
             * sure that the service does not get called when an activity is
             * destroyed by using the startForeground method. If not doing so,
             * the service is also killed when an accompanying Activity is
             * destroyed (wtf). */
            status = new RecorderStatus(getApplicationContext(), sensorProcesses, duration);
            startForeground(status.NOTIFICATION_ID, status.mNotification.build());

            /** acquire a wake lock to avoid the sensor data generators to suspend */
            mWl = ((PowerManager) getSystemService(POWER_SERVICE))
                  .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "sensorlock");
            mWl.acquire();

            /** wait for all local sensors */
            for(boolean ready=false; !ready; ready=true)
              for (SensorProcess process : sensorProcesses)
                ready &= process.getSensor().isPrepared();


            mIsRecording = true;

            if (isMaster) { // wait for everyone to send prepared
                while (SEMAPHORE > 0 && mIsRecording)
                    Thread.sleep(500); // wait till everyone's ready
                Log.e(TAG, "all nodes are ready");
                if (mIsRecording) {
                    status.steady(System.currentTimeMillis() + DEFAULT_STEADY_TIME);
                    Thread.sleep(DEFAULT_STEADY_TIME);
                }
            } else {
                status.ready(sensors);
                while (SEMAPHORE > 0 && mIsRecording)
                    Thread.sleep(100); // wait till steady and our time has come ;)
            }

            Log.e(TAG, "RECORDING");

            ((Vibrator) getApplicationContext().getSystemService(VIBRATOR_SERVICE)).vibrate(100);

            for (SensorProcess process : sensorProcesses)
                process.startRecording();

            /** now wait until the recording is stopped or a timeout has occurred, give
             * 1 seconds extra, as the sensorprocesses should stop themselves,
             * but need additional time to transport the data */
            for (mRecordingSince = System.currentTimeMillis();
                 mIsRecording && (duration <= 0 ||
                         System.currentTimeMillis() - mRecordingSince < (long) duration * 1000 + 1000);
                 Thread.sleep(500))
                status.recording(System.currentTimeMillis() - mRecordingSince, (long) duration * 1000 * 1000);

            /** close down the ffmpeg process and all sensorprocesses */
            onDestroy();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "unable to start recording");
            status.error(e);
            return;
        }
    }

    private boolean isIntentForwarded(Intent intent) {
        return intent.getExtras().keySet().contains("forwarded");
    }

    public static void stopCurrentRecording() {
        mIsRecording = false; SEMAPHORE = Integer.MIN_VALUE;
    }

    public void onDestroy() {
       if (sensorProcesses == null)
           return;

       /** close all streams to notify each process that we're done */
       for (SensorProcess p : sensorProcesses)
           p.terminate();

       /** wait for ffmpeg to finish */
       try { ffmpeg.terminate(); }
       catch (InterruptedException e) {};

       /** release the wakelock again */
       mWl.release();

       /** notify the system that a sensor recording is finished, stopForeground
        * to remove the service-bound notification and display the finished one */
       stopForeground(true);
       status.finished(output);

       sensorProcesses = null;
       ffmpeg = null;
       status = null;
       mWl = null;
    }
}
