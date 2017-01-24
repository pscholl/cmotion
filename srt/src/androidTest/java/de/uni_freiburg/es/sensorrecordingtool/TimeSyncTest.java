package de.uni_freiburg.es.sensorrecordingtool;

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.MediumTest;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.uni_freiburg.es.sensorrecordingtool.clock.TimeSync;

/**
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class TimeSyncTest extends BroadcastingTest {




    @Test public void doSync() throws Exception {
        TimeSync sync = TimeSync.getInstance();
        sync.getDrift();
        sync.getDrift();
        sync.getDrift();

        Assert.assertTrue("Has Drift", sync.isDriftCalculated());
    }


}
