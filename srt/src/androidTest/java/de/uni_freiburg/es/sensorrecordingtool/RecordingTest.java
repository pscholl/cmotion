package de.uni_freiburg.es.sensorrecordingtool;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.MediumTest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Created by phil on 2/23/16.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class RecordingTest {
    private Context c;
    private Intent i;
    private String o;
    private static int count = 0;

    @Before public void setup() {
        c = InstrumentationRegistry.getTargetContext();
        i = new Intent();
        o = RecorderCommands.getDefaultOutputPath() + Integer.toString(count++);
        i.putExtra("-o", o);
        i.setAction(Recorder.RECORD_ACTION);
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
        //try { delete(new File(o));
        //} catch (FileNotFoundException e) {}
    }


    @Test public void recordingNoInputSupplied() throws InterruptedException {
        String result = callForError(i);
        Assert.assertTrue("no answer from Service", result != null);
        Assert.assertEquals("error msg", "no input supplied", result);
    }

    @Test public void doARecordingWithRates() throws Exception {
        i.putExtra("-i", "accelerometer");
        i.putExtra("-r", 100.);
        i.putExtra("-d", 15.0);
        String result = callForResult(i);
        Assert.assertNotNull("timeout before completion", result);

        assertRecording(result, "accelerometer", 100 * (3) * 4 * 5);
    }

     @Test public void doMultipleSensors() throws Exception {
        i.putExtra(Recorder.RECORDER_INPUT, new String[]{
                "acc",
                "gyr",
                "mag",
                "rot"
        });
        i.putExtra("-d", 36.0f);
        i.putExtra("-r", 40);

        String x = Build.MODEL;

        String result = callForResult(i);
        Assert.assertNotNull("timeout", result);

        assertRecording(result, "acc", 40 * (3) * 4 * 6);
        assertRecording(result, "gyr", 40 * (3) * 4 * 6);
        if (!onChargingGradle())
            assertRecording(result, "mag", 40 * (3) * 4 * 6);
        assertRecording(result, "rot", 40 * (5) * 4 * 6);
    }

    @Test public void doMultipleSensorsAndRates() throws Exception {
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
        if (!onChargingGradle())
            assertRecording(result, "mag", 40 * (3) * 4 * 6);
        assertRecording(result, "rot", 100*(5)*4*5);
    }

    @Test public void doLocationTest() throws Exception {
        i.putExtra(Recorder.RECORDER_INPUT, "location");
        i.putExtra("-d", 5.0);
        String result = callForResult(i);
        Assert.assertNotNull("timed out", result);
        assertRecording(result, "location", 50*(4)*4*5);
    }

    @Test public void doInfiniteRecordingTest() throws Exception {
        i.putExtra("-d", -1);
        i.putExtra("-i", "acc");

        /* send a cancel request after five seconds */
        Handler h = new Handler(c.getMainLooper());
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent cancel = new Intent(Recorder.CANCEL_ACTION);
                c.sendBroadcast(cancel);
            }
        }, 15500);

        String result = callForResult(i);
        Assert.assertNotNull("timed out", result);

        assertRecording(result, "acc", 50*3*4*5, true);
    }

    /* we assume that some models are residing on their magnetized charging gradle while plugged
     * in. The magnetometer will not return any data in this case, which is why we have this corner-
     * case.     */
    private boolean onChargingGradle() {
        Intent intent = c.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        boolean in  = plugged == BatteryManager.BATTERY_PLUGGED_AC ||
                      plugged == BatteryManager.BATTERY_PLUGGED_USB;

        return !(in && Build.MODEL.equalsIgnoreCase("G Watch"));
    }

    public void assertRecording(String f, String sensor, int size) throws Exception {
        assertRecording(f,sensor,size,false);
    }

    private void assertRecording(String f, String sensor, int size, boolean b) throws Exception {
        byte buf[] = new byte[size];
        FFMpegProcess p = new FFMpegProcess.Builder()
                .addInputArgument("-i", "file:"+f)
                .addInputArgument("-map", String.format("m:name:%s",sensor))
                .setOutput("-", "f32le")
                .build(c);
        p.waitFor();
        if (b) {
            int n = p.getInputStream().read(buf);
            Assert.assertTrue(String.format("%d >= %d", n, size), n >= size);
        } else
            Assert.assertEquals(size, p.getInputStream().read(buf));
    }


    private String callForError(Intent i) throws InterruptedException {
        return callForResult(i, 25000, Recorder.ERROR_ACTION, Recorder.ERROR_REASON);
    }

    private String callForResult(Intent i) throws InterruptedException {
        return callForResult(i, 25000, Recorder.FINISH_ACTION, Recorder.FINISH_PATH);
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
        c.sendBroadcast(i);
        return lock.await(ms, TimeUnit.MILLISECONDS) ? result[0] : null;
    }
}
