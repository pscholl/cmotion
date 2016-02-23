package de.uni_freiburg.es.sensorrecordingtool;

import android.app.Instrumentation;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.content.LocalBroadcastManager;
import android.test.suitebuilder.annotation.MediumTest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Date;
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

    @Before public void setup() {
        c = InstrumentationRegistry.getTargetContext();
        i = new Intent(c, Recorder.class);
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
        try { delete(new File(Recorder.getDefaultOutputPath()));
        } catch (FileNotFoundException e) {}
    }

     public void recordingNoInputSupplied() throws InterruptedException {
        String result = callForError(i);
        Assert.assertTrue("no answer from Service", result != null);
        Assert.assertEquals("error msg", result, "no input supplied");
    }

     public void noRateSupplied() throws InterruptedException {
        i.putExtra("-i", "acc");
        String result = callForError(i);
        Assert.assertEquals(null, result);
    }

    @Test public void doARecordingWithRates() throws InterruptedException {
        i.putExtra("-i", "accelerometer");
        i.putExtra("-r", 100.);
        i.putExtra("-d", 5.0);
        String result = callForResult(i);
        Assert.assertNotNull(result);

        File path = new File(new File(result), "accelerometer");
        Assert.assertTrue("no output file " + path.toString(), path.exists());
        Assert.assertEquals("wrong size", 100*3*4*5, path.length());
    }

    private String callForError(Intent i) throws InterruptedException {
        return callForResult(i, 2000, Recorder.ERROR_ACTION, Recorder.ERROR_REASON);
    }

    private String callForResult(Intent i) throws InterruptedException {
        return callForResult(i, 10000, Recorder.FINISH_ACTION, Recorder.FINISH_PATH);
    }

    private String callForResult(Intent i, int ms, String action, final String extra) throws InterruptedException {
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
