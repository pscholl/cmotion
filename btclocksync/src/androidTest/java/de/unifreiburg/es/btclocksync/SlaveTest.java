package de.unifreiburg.es.btclocksync;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class SlaveTest {
    @Test
    public void test() throws Exception {
        ClockSyncManager mgr = new ClockSyncManager(InstrumentationRegistry.getContext());

        long start = System.currentTimeMillis();
        int max = 100, fails = 0;

        for (int i = 0; i < max; i++)
            try {
                mgr.getTime();
            } catch (Exception e) {
                fails++;
            }

        Log.e("RESULT", ((System.currentTimeMillis() - start) / 1000f) + "s");
        Log.e("RESULT", fails + " " + max);

        Assert.assertTrue("connections: " + (fails / (float) max) * max + "% error is too much", fails < max / 2f);

    }
}
