package de.unifreiburg.es.btclocksync;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class FunctionalityTest {
    @Test
    public void test() throws Exception {

        new ClockSyncServerThread().start();
        while(true) Thread.yield();
    }
}
