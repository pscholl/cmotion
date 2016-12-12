package de.uni_freiburg.es.sensorrecordingtool;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.MediumTest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test the autodiscovery;
 * <p>
 * Created by phil on 2/23/16.
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class ReadySteadyTest extends BroadcastingTest {
    private Context c;
    private Intent i;

    @Before
    public void setup() {
        c = InstrumentationRegistry.getTargetContext();
        i = new Intent();
        i.setAction(Recorder.RECORD_ACTION);
        i.putExtra(Recorder.RECORDER_INPUT, "gyro");
    }


    @Test
    public void testMaster() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        i.removeExtra("forwarded");
        c.sendBroadcast(i);


        final boolean[] gotIntent = {false};
        c.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                latch.countDown();
                gotIntent[0] = true;
            }
        }, new IntentFilter(Recorder.STEADY_ACTION));
        sendReady();
        latch.await(5000, TimeUnit.MILLISECONDS);
        Assert.assertTrue("Timeout", gotIntent[0]); // we got steady
        sendCancel();
    }

    @Test
    public void testSlave() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        i.putExtra("forwarded", true);
        c.sendBroadcast(i);


        final boolean[] gotIntent = {false};
        c.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                latch.countDown();
                gotIntent[0] = true;
            }
        }, new IntentFilter(Recorder.READY_ACTION));

        latch.await(5000, TimeUnit.MILLISECONDS);
        Assert.assertTrue("waiting for Ready caused timeout", gotIntent[0]); // we got steady


        final CountDownLatch statusLatch = new CountDownLatch(1);

        c.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                statusLatch.countDown();
                gotIntent[0] = true;
            }
        }, new IntentFilter(RecorderStatus.STATUS_ACTION));
        sendSteady();

        statusLatch.await(10000, TimeUnit.MILLISECONDS);
        Assert.assertTrue("waiting for Recording caused timeout", gotIntent[0]); // we got steady

        sendCancel();
    }

    private void sendReady() {
        Intent intent = new Intent();
        intent.setAction(Recorder.READY_ACTION);
        c.sendBroadcast(intent);
    }

    private void sendSteady() {
        Intent intent = new Intent();
        intent.setAction(Recorder.STEADY_ACTION);
        intent.putExtra(RecorderStatus.START_TIME, System.currentTimeMillis() * 1d);
        c.sendBroadcast(intent);
    }

    private void sendCancel() {
        Intent intent = new Intent();
        intent.setAction(Recorder.CANCEL_ACTION);
        c.sendBroadcast(intent);
    }

    private Bundle callForResult(Intent i, int timeout, String listenFor)
            throws InterruptedException {
        final CountDownLatch lock = new CountDownLatch(1);
        final Bundle[] result = new Bundle[1];
        c.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                result[0] = intent.getExtras();
                lock.countDown();
            }
        }, new IntentFilter(listenFor));
        c.sendBroadcast(i);
        return lock.await(timeout, TimeUnit.MILLISECONDS) ? result[0] : null;
    }
}
