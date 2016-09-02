package de.uni_freiburg.es.sensorrecordingtool;

import android.content.Context;
import android.hardware.Camera;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.TimeZone;

import de.uni_freiburg.es.sensorrecordingtool.sensors.SensorProcess;
import de.uni_freiburg.es.sensorrecordingtool.sensors.VideoSensor;

/** This class captures a recording of multiple sensors into a single recording.
 *
 * Created by phil on 9/1/16.
 */
public class RecordingProcess {

    public final FFMpegProcess ffmpeg;
    private final PowerManager mpm;
    private final PowerManager.WakeLock mwl;
    //private final Handler mTimeout;
    public LinkedList<SensorProcess> mInputList = new LinkedList<SensorProcess>();
    public final String   output;
    public final String[] sensors;
    public final String[] formats;
    public final double[] rates;
    public final double   duration;

    public RecordingProcess(Context c,
                            String output,
                            String[] sensors,
                            String[] formats,
                            double[] rates,
                            double duration) throws Exception
    {
        if (sensors == null)
            throw new Exception("need at least one sensor as input");

        if (formats == null)
            formats = new String[sensors.length];
        if (formats.length != sensors.length) {
            String[] fmts = new String[sensors.length];
            Arrays.fill(fmts, null);
            for (int j=0; j<formats.length; j++)
                fmts[j] = formats[j];
            formats = fmts;
        }

        /*
         * convert a single rate to an array, convert a single element array to a length
         * matching the sensor input array, check whether lengths are matching.
         */
        if (rates.length == 1) {
            rates = Arrays.copyOf(rates, sensors.length);
            Arrays.fill(rates, rates[0]);
        }

        if (rates.length != sensors.length)
            throw new Exception("either rates and sensors must of same length or a single rate must be given");

        for (double r : rates)
            if (r <= 0)
                throw new Exception("rate must be larger than zero, but was " + r);

        assert(rates.length==sensors.length);
        assert(formats.length==sensors.length);

        this.output   = output == null ? getDefaultOutputPath() : output;
        this.sensors  = sensors;
        this.formats  = formats;
        this.rates    = rates;
        this.duration = duration;

        /**
         * now start the ffmpeg process and the respective sensorprocesses
         */
        FFMpegProcess.Builder fp = new FFMpegProcess.Builder();
        fp.setOutput(output, "matroka");
        fp.setCodec("a", "wavpack");
        fp.setCodec("v", "libtheora");
        fp.addOutputSwitch("-qscale:v", "7");
        fp.setLoglevel("debug");

        for (int j=0; j<sensors.length; j++)
        {
            if (sensors[j].contains("video")) {
                Camera.Size size = VideoSensor.getCameraSize(formats[j]);
                fp.addVideo(size.width, size.height, rates[j], "rawvideo", "nv21")
                  .setStreamTag("name", "Android Default Cam");
            } else
                fp.addAudio("f32be", rates[j])
                  .setStreamTag("name", sensors[j]);
        }

        this.ffmpeg = fp.build(c);

        mpm = (PowerManager) c.getSystemService(c.POWER_SERVICE);
        mwl = mpm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Integer.toString(this.hashCode()));

        /* have a timeout after the duration to flush the sensor and terminate the process
         * afterwards. This is to avoid sensor that do not report data continuously. For example
         * in the case of a failure condition
        mTimeout = new Handler();
        if (duration > 0)
            mTimeout.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        RecordingProcess.this.terminate();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, (long) (duration * 1000) + 2000);
            */
    }

    /** utility function for ISO datetime path on public storage */
    public static String getDefaultOutputPath() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
        df.setTimeZone(tz);
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        return new File(path, df.format(new Date())).toString();
    }

    public OutputStream getOutputStream(int j) throws IOException, InterruptedException {
        return ffmpeg.getOutputStream(j);
    }

    public void terminate() throws IOException {
        for (SensorProcess p : mInputList)
            p.terminate();
    }
}