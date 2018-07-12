package de.uni_freiburg.es.sensorrecordingtool;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

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
@LargeTest
public class RecordingTest extends BroadcastingTest {
    private Context c;
    private Intent i;
    private String o;
    private static int count = 0;

    @Before public void setup() {
        c = InstrumentationRegistry.getTargetContext();
        i = new Intent();
        o = RecorderCommands.getDefaultOutputPath(c) + Integer.toString(count++);
        i.putExtra("-o", o);
        i.setAction(Recorder.RECORD_ACTION);
        try { // here to avoid bugs when restarting recording too quickly
            Thread.sleep(20*1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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

    @Test public void recordingInputNotFound() throws InterruptedException {
        i.putExtra(Recorder.RECORDER_INPUT, "accmememe");
        String result = callForError(i);
        Assert.assertTrue("no answer from Service", result != null);
        Assert.assertEquals("error msg", "sensor not found: accmememe", result);
    }

    @Test public void doARecordingWithRates() throws Exception {
        i.putExtra("-i", "accelerometer");
        i.putExtra("-r", 100.);
        i.putExtra("-d", 25.0);

        String result = callForResult(i);
        Assert.assertNotNull("timeout before completion", result);
        assertRecording(result, "0:a:0", 100 * (3) * 4 * 5);
    }

     @Test public void doMultipleSensors() throws Exception {
        i.putExtra(Recorder.RECORDER_INPUT, new String[]{
                "accelerometer",
                "gyr",
                "rot"
        });
        i.putExtra("-d", 6.0f);
        i.putExtra("-r", 40);

        String result = callForResult(i);
        Assert.assertNotNull("timeout", result);

        assertRecording(result, "0:a:0", 40 * (3) * 4 * 6);
        assertRecording(result, "0:a:1", 40 * (3) * 4 * 6);
        assertRecording(result, "0:a:2", 40 * (5) * 4 * 6);
    }

    @Test public void doMultipleSensorsAndRates() throws Exception {
        i.putExtra(Recorder.RECORDER_INPUT, new String[]{
                "accelerometer",
                "gyr",
                "rot"
        });
        i.putExtra("-d", 10.0);
        i.putExtra("-r", new double[]{25.0, 50.0, 100.0});

        String result = callForResult(i);
        Assert.assertNotNull("timeout", result);

        assertRecording(result, "0:a:0", 25*3*4*5);
        assertRecording(result, "0:a:1", 50*3*4*5);
        assertRecording(result, "0:a:2", 100*5*4*5);
    }

//    @Test public void doLocationTest() throws Exception {
//
////        if (c.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WATCH))
////          return;
//
//        i.putExtra(Recorder.RECORDER_INPUT, "location");
//        i.putExtra("-d", 5.0);
//        String result = callForResult(i);
//        Assert.assertNotNull("timed out", result);
//        assertRecording(result, "location", 50*(4)*4*5);
//    }

    @Test public void doInfiniteRecordingTest() throws Exception {
        i.putExtra("-d", -1);
        i.putExtra("-i", "accelerometer");
        i.putExtra("-r", 50);

        /* send a cancel request after 20 seconds */
        Handler h = new Handler(c.getMainLooper());
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent cancel = new Intent(Recorder.CANCEL_ACTION);
                c.sendBroadcast(cancel);
            }
        }, Recorder.DEFAULT_STEADY_TIME + 20*1000);

        String result = callForResult(i);
        Assert.assertNotNull("timed out", result);

        /** we only test for two seconds because of a possible startup delay */
        assertRecording(result, "0:a:0", (50*3*4*2), true);
    }

    public void assertRecording(String f, String stream, int size) throws Exception {
        assertRecording(f,stream,size,false);
    }

    private void assertRecording(String f, String stream, int size, boolean b) throws Exception {
        byte buf[] = new byte[size];
        FFMpegProcess p = new FFMpegProcess.Builder(c)
                .addInputArgument("-i", "file:"+f)
                .addInputArgument("-map", stream)
                .setOutput("-", "f32le")
                .build();
        p.waitFor();
        if (b) {
            int n = p.getInputStream().read(buf);
            Assert.assertTrue(String.format("%d >= %d", n, size), n >= size);
        } else
            Assert.assertEquals(size, p.getInputStream().read(buf));
    }


    private String callForError(Intent i) throws InterruptedException {
        return callForResult(i, 50 * 1000, RecorderStatus.ERROR_ACTION, RecorderStatus.ERROR_REASON);
    }

    private String callForResult(Intent i) throws InterruptedException {
        return callForResult(i, 50 * 1000, RecorderStatus.FINISH_ACTION, RecorderStatus.FINISH_PATH);
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