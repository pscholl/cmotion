package de.uni_freiburg.es.sensorrecordingtool;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.MediumTest;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import de.uni_freiburg.es.sensorrecordingtool.sensors.AudioSensor;

/** Test the ability to record videos from one of the cameras connected to the Android
 * System.
 *
 * Created by phil on 2/23/16.
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class AudioTest extends BroadcastingTest {
    private Context c;
    private Intent i;
    private String o;
    private static int count = 0;

    @Before
    public void setup() {
        c = InstrumentationRegistry.getTargetContext();
        i = new Intent(c, Recorder.class);
        o = RecorderCommands.getDefaultOutputPath(c) + Integer.toString(count++);
        i.setAction(Recorder.RECORD_ACTION);
        i.putExtra("-o", o);
    }

    public void delete(File f) throws FileNotFoundException {
        if (f.isDirectory()) {
            for (File c : f.listFiles())
               delete(c);
        }
        if (!f.delete())
            throw new FileNotFoundException("Failed to delete file: " + f);
    }

    @After
    public void teardown() {
        // not every test generates a directory.
        try { delete(new File(o));
        } catch (FileNotFoundException e) {}
    }


    @Test
    public void doAudioRecording() throws InterruptedException, IOException, JSONException {
        double recDuration = 5;
        i.putExtra("-i", "audio");
        i.putExtra("-d", recDuration);
        i.putExtra("-r", 8000);
        String result = callForResult(i);
        Assert.assertNotNull("timeout before completion", result);
        assertRecording(result, "00:00:05.000000000");
    }

//    public void doVideoAndOtherSensor() throws InterruptedException {
//        i.putExtra("-i", "video accelerometer".split(" "));
//        i.putExtra("-r", new double[] {15., 50.});
//        i.putExtra("-d", 5.0);
//        String result = callForResult(i);
//        Assert.assertNotNull("timeout before completion", result);
//        assertRecording(result, "video", (int) (15*5 * (1920*1080*1.5)));
//    }


    /* we assume that some models are residing on their magnetized charging gradle while plugged
     * in. The magnetometer will not return any data in this case, which is why we have this corner-
     * case.     */
    private int zeroWhenOnGradle(int i) {
        Intent intent = c.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        boolean in  = plugged == BatteryManager.BATTERY_PLUGGED_AC ||
                      plugged == BatteryManager.BATTERY_PLUGGED_USB;

        if (in && Build.MODEL.equalsIgnoreCase("G Watch"))
            return 0;

        return i;
    }

    public void assertRecording(String f, String duration) throws IOException, JSONException, InterruptedException {
        File path = new File(f);
        Assert.assertTrue("no output file " + path.toString(), path.exists());

        FFProbeProcess ffp = new FFProbeProcess.Builder()
                .addInput(f)
                .addShowOption("streams")
                .build(c);

        String dur = ffp
                .getJSONResult()
                .getJSONArray("streams")
                .getJSONObject(0)
                .getJSONObject("tags")
                .getString("DURATION");

        Assert.assertEquals("wrong duration",duration, dur);
    }

    private String callForError(Intent i) throws InterruptedException {
        return callForResult(i, 15000 * 1000, RecorderStatus.ERROR_ACTION, RecorderStatus.ERROR_REASON);
    }

    private String callForResult(Intent i) throws InterruptedException {
        return callForResult(i, 15000 * 1000, RecorderStatus.FINISH_ACTION, RecorderStatus.FINISH_PATH);
    }

    private String callForResult(Intent i, int ms, String action, final String extra)
            throws InterruptedException {
        final CountDownLatch lock = new CountDownLatch(1);
        final String[] result = new String[] {null};
        c.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                result[0] = intent.getStringExtra(extra);
                lock.countDown();
            }
        }, new IntentFilter(action));
        c.startService(i);
        return lock.await(ms, TimeUnit.MILLISECONDS) ? result[0] : null;
    }
}
