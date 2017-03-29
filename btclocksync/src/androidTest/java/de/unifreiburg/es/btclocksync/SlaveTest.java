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

        long minTime = Long.MAX_VALUE, maxTime = Long.MIN_VALUE;


        for (int i = 0; i < max; i++)
            try {
                long drift = mgr.getOffset();

                if(drift > maxTime)
                    maxTime = drift;

                if(drift < minTime)
                    minTime = drift;

            } catch (Exception e) {
                fails++;
            }

        Log.e("RESULT", ((System.currentTimeMillis() - start) / 1000f) + "s");
        Log.e("RESULT", (fails / (float) max) * 100 + "% error rate, min="+minTime+" max="+maxTime);

        Assert.assertTrue("connections: " + (fails / (float) max) * max + "% error is too much", fails < max / 2f);

    }

    @Test
    public void testSafe() throws Exception {
        ClockSyncManager mgr = new ClockSyncManager(InstrumentationRegistry.getContext());

        long start = System.currentTimeMillis();
        int max = 10, fails = 0;

        long minTime = Long.MAX_VALUE, maxTime = Long.MIN_VALUE;


        for (int i = 0; i < max; i++)
            try {
                long drift = mgr.getOffsetSafe();

                if(drift > maxTime)
                    maxTime = drift;

                if(drift < minTime)
                    minTime = drift;

            } catch (Exception e) {
                fails++;
            }

        Log.e("RESULT", ((System.currentTimeMillis() - start) / 1000f) + "s");
        Log.e("RESULT", (fails / (float) max) * max + "% error rate, min="+minTime+" max="+maxTime);

        Assert.assertTrue("connections: " + (fails / (float) max) * 100 + "% error is too much", fails < max / 2f);
        Assert.assertTrue("distance too high: "+(maxTime-minTime), (maxTime - minTime) < 10); // error less then 10ms

    }
}
