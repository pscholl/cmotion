package de.uni_freiburg.es.sensorrecordingtool;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.os.BatteryManager;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.MediumTest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by phil on 2/23/16.
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class RecordingTest {
    private Context c;
    private Intent i;
    private String o;
    private static int count = 0;

    @Before public void setup() {
        c = InstrumentationRegistry.getTargetContext();
        i = new Intent(c, Recorder.class);
        o = Recorder.getDefaultOutputPath() + Integer.toString(count++);
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

    @After public void teardown() {
        // not every test generates a directory.
        try { delete(new File(o));
        } catch (FileNotFoundException e) {}
    }


    @Test public void recordingNoInputSupplied() throws InterruptedException {
        String result = callForError(i);
        Assert.assertTrue("no answer from Service", result != null);
        Assert.assertEquals("error msg", "no input supplied", result);
    }

    @Test public void doARecordingWithRates() throws InterruptedException {
        i.putExtra("-i", "accelerometer");
        i.putExtra("-r", 100.);
        i.putExtra("-d", 5.0);
        String result = callForResult(i);
        Assert.assertNotNull("timeout before completion", result);

        assertRecording(result, "accelerometer", 100 * (3) * 4 * 5);
    }

     @Test public void doMultipleSensors() throws InterruptedException {
        i.putExtra(Recorder.RECORDER_INPUT, new String[]{
                "acc",
                "gyr",
                "mag",
                "rot"
        });
        i.putExtra("-d", 6.0f);
        i.putExtra("-r", 40);

        String x = Build.MODEL;

        String result = callForResult(i);
        Assert.assertNotNull("timeout", result);

        assertRecording(result, "acc", 40 * (3) * 4 * 6);
        assertRecording(result, "gyr", 40 * (3) * 4 * 6);
        assertRecording(result, "mag", zeroWhenOnGradle(40 * (3) * 4 * 6));
        assertRecording(result, "rot", 40 * (5) * 4 * 6);
    }

    @Test public void doMultipleSensorsAndRates() throws InterruptedException {
        i.putExtra(Recorder.RECORDER_INPUT, new String[]{
                "acc",
                "gyr",
                "mag",
                "rot"
        });
        i.putExtra("-d", 5.0);
        i.putExtra("-r", new double[]{25.0, 50.0, 75.0, 100.0});

        String result = callForResult(i);
        Assert.assertNotNull("timeout", result);

        assertRecording(result, "acc", 25 * (3) * 4 * 5);
        assertRecording(result, "gyr", 50*(3)*4*5);
        assertRecording(result, "mag", zeroWhenOnGradle(75*(3)*4*5));
        assertRecording(result, "rot", 100*(5)*4*5);
    }

    @Test public void doLocationTest() throws InterruptedException {
        i.putExtra(Recorder.RECORDER_INPUT, "location");
        i.putExtra("-d", 5.0);
        String result = callForResult(i);
        Assert.assertNotNull("timeout", result);

        assertRecording(result, "location", 50*(4)*4*5);
    }

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

    public void assertRecording(String f, String p, int size) {
        File path = new File(new File(f), p);
        Assert.assertTrue("no output file " + path.toString(), path.exists());
        Assert.assertEquals("wrong size", size, path.length());
    }

    private String callForError(Intent i) throws InterruptedException {
        return callForResult(i, 15000, Recorder.ERROR_ACTION, Recorder.ERROR_REASON);
    }

    private String callForResult(Intent i) throws InterruptedException {
        return callForResult(i, 15000, Recorder.FINISH_ACTION, Recorder.FINISH_PATH);
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
