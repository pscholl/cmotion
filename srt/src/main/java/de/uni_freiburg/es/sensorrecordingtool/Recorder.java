package de.uni_freiburg.es.sensorrecordingtool;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;

import java.io.File;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import de.uni_freiburg.es.intentforwarder.ForwardedUtils;
import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.AutoDiscovery;
import de.uni_freiburg.es.sensorrecordingtool.merger.MergeService;
import de.uni_freiburg.es.sensorrecordingtool.merger.provider.MergeProviderSession;
import de.uni_freiburg.es.sensorrecordingtool.sensors.AudioSensor;
import de.uni_freiburg.es.sensorrecordingtool.sensors.BlockSensorProcess;
import de.uni_freiburg.es.sensorrecordingtool.sensors.NonBlockSensorProcess;
import de.uni_freiburg.es.sensorrecordingtool.sensors.Sensor;
import de.uni_freiburg.es.sensorrecordingtool.sensors.SensorProcess;
import de.uni_freiburg.es.sensorrecordingtool.sensors.VideoSensor;
import de.unifreiburg.es.btclocksync.ClockSyncManager;
import de.unifreiburg.es.btclocksync.ClockSyncServerThread;

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
public class Recorder extends InfiniteIntentService {
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
    private static final long STEADY_TIMEOUT = 10000;

    /* whether we are currently recording */
    public static boolean mIsRecording = false;
    public static long mRecordingSince = -1;

    public static final long DEFAULT_STEADY_TIME = 3000;

    public static CountDownLatch SEMAPHORE = new CountDownLatch(1);
    public static boolean isMaster;
    public static boolean isReady = false;

    private AutoDiscovery mAutoDiscovery;

    private ClockSyncServerThread mClockSyncServerThread = null;

    /* members when a recording is ongoing, stored here for cleanup from
     * onDestroy */
    private List<SensorProcess> sensorProcesses;
    private PowerManager.WakeLock mWl = null;
    private RecorderStatus status;
    private FFMpegProcess ffmpeg;
    private String output;
    public static long OFFSET;
    private double duration;
    private boolean error = false;


    public static String mRecordUUID;

    public static ArrayList<String> mReadyNodes = new ArrayList<>();

    public Recorder() {
        super(Recorder.class.getName());
    }

    private SensorProcess newSensorProcess(String sensor, String format, double rate,
                                           double dur, FFMpegProcess p, int j) throws Exception {
        Context c = this.getApplicationContext();

        /** one thread for each sensor to make sure that they can block on write on their
         * respective delivery outputstream without interacting with any other sensorprocesses */
        HandlerThread h = new HandlerThread("sensorprocess " + sensor);
        h.start();

        SensorProcess process;

        if (sensor.contains("video") || sensor.contains("audio"))
            process =  new BlockSensorProcess(c, sensor, rate, format, dur, p, j,
                    new Handler(h.getLooper()));
        else
            process = new NonBlockSensorProcess(c, sensor, rate, format, dur, p, j,
                    new Handler(h.getLooper()));

        process.setHandlerThread(h);
        return process;
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

        mIsRecording = true;
        mReadyNodes.clear(); // remove all old ready-flagged nodes

        isMaster = !isIntentForwarded(intent);
        Log.e(TAG, "We are " + (isMaster ? "Master" : "Slave"));


        try {

            intent = RecorderCommands.parseRecorderIntent(this, intent);
            output = intent.getStringExtra(RECORDER_OUTPUT);
            String[] sensors = intent.getStringArrayExtra(RECORDER_INPUT);
            String[] formats = intent.getStringArrayExtra(RECORDER_FORMAT);
            double[] rates = intent.getDoubleArrayExtra(RECORDER_RATE);
            duration = intent.getDoubleExtra(RECORDER_DURATION, -1);
            isReady = false;


            if (isMaster)
                mRecordUUID = UUID.randomUUID().toString();
            status = new RecorderStatus(getApplicationContext(), sensors.length, duration, mRecordUUID);

            initSynchronization(isMaster);

            ffmpeg = buildFFMPEG(this, sensors, formats, rates);
            sensorProcesses = new LinkedList<>();

            /** create sensorprocess for each input and wire it to the ffmpeg process */
            for (int j = 0; j < sensors.length; j++)
                sensorProcesses.add(newSensorProcess(
                        sensors[j], formats[j], rates[j], duration, ffmpeg, j));

            /** notify the system that a new recording was started, and make
             * sure that the service does not get called when an activity is
             * destroyed by using the startForeground method. If not doing so,
             * the service is also killed when an accompanying Activity is
             * destroyed (wtf). */
            startForeground(status.NOTIFICATION_ID, status.mNotification.build());

            /** make sure the screen is turning on for some time, otherwise sensors won't start.
             * They keep running though, once the screen turns off again. */
            mWl = ((PowerManager) getSystemService(POWER_SERVICE))
                    .newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK |
                                 PowerManager.ACQUIRE_CAUSES_WAKEUP,
                                 "sensorlock");
            mWl.acquire();

            /** wait for all local sensors */
            for (boolean ready = false; !ready; ready = true) {
                for (SensorProcess process : sensorProcesses)
                    ready &= process.getSensor().isPrepared();
            }

            if (Thread.currentThread().interrupted())
                throw new InterruptedIOException();

            boolean driftCalculated = true;
            if (!isMaster) {
                try {
                    OFFSET = new ClockSyncManager(this).getOffsetSafe();
                } catch (TimeoutException e) {
                    // TODO come up with something clever here.
                    e.printStackTrace();
                    driftCalculated = false;
                    OFFSET = 0;
                }

            } else OFFSET = 0; // since we provide the time in this case ...

            Log.e(TAG, String.format("Clock drift: %s ms - valid computation: %s", OFFSET, driftCalculated));

            if (Thread.currentThread().interrupted())
                throw new InterruptedIOException();

            readySteady(isMaster, sensors, OFFSET, driftCalculated);

            if (isMaster) {
                startMergeService();
            }

            for (SensorProcess process : sensorProcesses)
                process.startRecording();
            mRecordingSince = System.currentTimeMillis();
            ((Vibrator) getApplicationContext().getSystemService(VIBRATOR_SERVICE)).vibrate(100);
            Log.e(TAG, "RECORDING");

            waitUntilEnd();
        } catch (InterruptedException ie) {
            error = true;
            Log.e(TAG, "recording aborted during semaphore phase");
            if (status != null)
                status.error(new Exception("Recording terminated"));
        } catch (Exception e) {
            // TODO needs to be more explicit
            error = true;
            e.printStackTrace();
            Log.d(TAG, "unable to start recording");
            if (status != null)
                status.error(e);
        }

        if (error)
        /** close down the ffmpeg process and all sensorprocesses */
            stopSelf();


    }

    private Handler handler = new Handler();

    private void waitUntilEnd() {
        if (mIsRecording && (duration <= 0 ||
                System.currentTimeMillis() - mRecordingSince < (long) duration * 1000 + 1000)) {
            Log.d(TAG, "recording status");
            status.recording((System.currentTimeMillis() - mRecordingSince) * 1000, (long) duration * 1000 * 1000);
            long timeLeft = Double.valueOf(duration).longValue() - (System.currentTimeMillis() - mRecordingSince);
            if (mWl.isHeld()) mWl.release();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    waitUntilEnd();
                }
            }, Math.min(2500, duration <= 0 ? Integer.MAX_VALUE : timeLeft));
        } else
            stopSelf();

    }

    private void readySteady(boolean isMaster, String[] sensors, long drift, boolean driftSet) throws InterruptedException {
        status.ready(sensors, drift, driftSet);
        isReady = true;

        if (!isMaster)
            SEMAPHORE.await(); // wait and die
        else
            SEMAPHORE.await(STEADY_TIMEOUT, TimeUnit.MILLISECONDS); // MASTER may have a timeout

        if (isMaster) { // wait for everyone to send prepared
            Log.e(TAG, "all nodes are ready - sending steady");

            if (mClockSyncServerThread != null) // we can stop BT timesync hosting
                mClockSyncServerThread.interrupt();

            long correctTime = System.currentTimeMillis() + Recorder.OFFSET;
            status.steady(correctTime + DEFAULT_STEADY_TIME);
            new CountDownLatch(1).await(DEFAULT_STEADY_TIME, TimeUnit.MILLISECONDS);

        }
    }

    private void initSynchronization(boolean isMaster) throws InterruptedException {
        if (isMaster) {

            try {
                mClockSyncServerThread = new ClockSyncServerThread();
                mClockSyncServerThread.start();
            } catch (Exception e) {
                Log.e(TAG, "Unable to start BT-Clock Server " + e);
                e.printStackTrace();
            }

            SEMAPHORE = new CountDownLatch(Integer.MAX_VALUE);
            mAutoDiscovery = AutoDiscovery.getInstance(this);
            if (mAutoDiscovery.getNonAutonomousConnectedNodes() <= 1 || true) {
                Log.e(TAG, "Running discovery to find nodes");
                mAutoDiscovery.discover();
                Thread.sleep(5000);
                Log.e(TAG, String.format("We have at least %s nodes (including us)", mAutoDiscovery.getNonAutonomousConnectedNodes()));
            }

            int readyNodes = Integer.MAX_VALUE - (int) (SEMAPHORE.getCount());
            if (readyNodes > 0)
                Log.e(TAG, readyNodes + " nodes are already ready");
            SEMAPHORE = new CountDownLatch(Math.max(0, mAutoDiscovery.getNonAutonomousConnectedNodes() - readyNodes)); // do not init Latch with negative number
            Log.e(TAG, "Latch at " + SEMAPHORE.getCount());

        } else
            SEMAPHORE = new CountDownLatch(1);
    }

    /**
     * Starts {@link MergeService} with the current recording-UUID and all nodes that have confirmed to be ready.
     */
    private void startMergeService() {
        assert isMaster;

        Intent startServiceIntent = new Intent(getApplicationContext(), MergeService.class);
        startServiceIntent.putExtra(RecorderStatus.RECORDING_UUID, mRecordUUID);
        startServiceIntent.putExtra(MergeService.RELEVANT_AIDS, mReadyNodes);
        getApplicationContext().startService(startServiceIntent);
    }

    private String getBuildDate() {
        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), 0);
            ZipFile zf = new ZipFile(ai.sourceDir);
            ZipEntry ze = zf.getEntry("classes.dex");
            long time = ze.getTime();
            String s = SimpleDateFormat.getInstance().format(new java.util.Date(time));
            zf.close();
            return s;
        } catch (Exception e) {
            return "unknown";
        }
    }

    private FFMpegProcess buildFFMPEG(Context context, String[] sensors, String[] formats, double[] rates) throws Exception {

        String platform = Build.BOARD + " " + Build.DEVICE + " " + Build.VERSION.SDK_INT;

        /** create an ffmpeg process that will demux all sensor recordings into
         * a single file on one time axis. */
        FFMpegProcess.Builder fp = new FFMpegProcess.Builder(context);

        fp.setOutput(output, "matroska")
                .setCodec("a", "wavpack")
                .setCodec("v", "libx264")
                .setTag("recorder", "cmotion v" + getBuildDate())
                .setTag("android_id", Settings.Secure.getString(getContentResolver(),
                        Settings.Secure.ANDROID_ID))
                .setTag("platform", platform)
                .setTag("fingerprint", Build.FINGERPRINT)
                .setTag("beginning", getCurrentDataAsIso())
                .addOutputArgument("-preset", "ultrafast")
                .setLoglevel("error");

        if (isMaster)
            fp.setTag("recording_id", mRecordUUID);

        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WATCH))
            fp.setTag("wear_location", WearPositionManager.getPosition(context).name());


        /** create a SensorProcess for each input and wire it to ffmpeg accordingly */
        for (int j = 0; j < sensors.length; j++) {
            Sensor matched = SensorProcess.getMatchingSensor(this, sensors[j]);

            if (matched instanceof VideoSensor) {
                VideoSensor.CameraSize size = VideoSensor.getCameraSize(formats[j]);

                fp
                        .addVideo(size.width, size.height, rates[j], "rawvideo", "nv21")
                        .setStreamTag("name", "Android Default Cam");
                ;
            } else if (matched instanceof AudioSensor) {
                fp
                        .addAudio(ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? "s16le" : "s16be",
                                rates[j],
                                ((AudioSensor) matched).getChannels()) // native endian!
//                        .setStreamTag("resolution", sensors[j].getResolution())
//                        .setStreamTag("unit", sensors[j].getUnit())
                        .setStreamTag("name", sensors[j])
                ;
            } else
                fp
                        .addAudio("f32be", rates[j], SensorProcess.getSampleSize(this, sensors[j]))
                        .setStreamTag("name", sensors[j]);

            fp.setStreamTag("platform", platform);
        }

        return fp.build();
    }

    private boolean isIntentForwarded(Intent intent) {
        return intent.getExtras().keySet().contains("forwarded");
    }

    public static void stopCurrentRecording() {
        mIsRecording = false;
        Log.e(TAG, "interrupting ... ");

        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.getName().contains(Recorder.class.getCanonicalName())) {
                t.interrupt();
                Log.e(TAG, t.getName() + " interrupted!");
            }
        }
    }

    public static String getCurrentDataAsIso() {
        /** from
         * https://stackoverflow.com/questions/3914404/how-to-get-current-moment-in-iso-8601-format
         * */
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);
        return df.format(new Date());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mIsRecording = false;

        if (handler != null)
            handler.removeCallbacksAndMessages(null);

        if (mClockSyncServerThread != null)
            mClockSyncServerThread.interrupt();
        if (!error)
            status.finishing();
        new Thread() {

            @Override
            public void run() {
                if (sensorProcesses != null) {

                    /** close all streams to notify each process that we're done */
                    for (SensorProcess p : sensorProcesses)
                        p.terminate();

                    /** wait for ffmpeg to finish */
                    try {
                        ffmpeg.terminate();
                    } catch (InterruptedException e) {
                    }

                    /** release the wakelock again */
                    if (mWl != null && mWl.isHeld())
                        mWl.release();

                    /** notify the system that a sensor recording is finished, stopForeground
                     * to remove the service-bound notification and display the finished one */
                    stopForeground(true);

                }

                if (!error) {
                    spawnProvider();
                    status.finished(output);
                }

                sensorProcesses = null;
                ffmpeg = null;
                status = null;
                mWl = null;
            }
        }.start();
    }

    private void spawnProvider() {
        if (!isMaster) // masters dont have providers
            new MergeProviderSession(Recorder.this, mRecordUUID, new File(output));
    }
}
