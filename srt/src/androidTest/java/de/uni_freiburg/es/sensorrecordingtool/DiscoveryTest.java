package de.uni_freiburg.es.sensorrecordingtool;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.MediumTest;
import android.text.TextUtils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.AutoDiscovery;
import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.Node;
import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.OnNodeSensorsDiscoveredListener;

/**
 * Test the autodiscovery;
 * <p>
 * Created by phil on 2/23/16.
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class DiscoveryTest extends BroadcastingTest {
    private Context c;
    private Intent i;

    @Before
    public void setup() {
        c = InstrumentationRegistry.getTargetContext();
        i = new Intent();
        i.setAction(Recorder.DISCOVERY_ACTION);
    }


    @Test
    public void testRawIntent() throws InterruptedException {
        Bundle bundle = callForResult(i, 5000, Recorder.DISCOVERY_RESPONSE_ACTION);
        Assert.assertTrue("Timeout", bundle != null);
        Assert.assertTrue("Sensors >= 4", bundle.getStringArray(RecorderStatus.SENSORS).length >= 4);
        Assert.assertEquals("Android-ID == 16", bundle.getString(RecorderStatus.ANDROID_ID).length(), 16);
        Assert.assertTrue("Platform set", !TextUtils.isEmpty(bundle.getString(RecorderStatus.PLATFORM)));
    }

    @Test
    public void testImplementation() throws InterruptedException {
        AutoDiscovery discovery = AutoDiscovery.getInstance(c);
        final CountDownLatch latch = new CountDownLatch(1);
        discovery.setListener(new OnNodeSensorsDiscoveredListener() {
            @Override
            public void onNodeSensorsDiscovered(Node node, String[] availableSensors) {
                latch.countDown();
            }
        });
        discovery.discover();
        latch.await(5000, TimeUnit.MILLISECONDS);
        Assert.assertTrue("Connected nodes >= 1", discovery.getConnectedNodes() > 0);
        Assert.assertTrue("Sensors >= 4", discovery.getDiscoveredSensors().get(0).getAvailableSensors().length >= 4);
        discovery.close();
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
