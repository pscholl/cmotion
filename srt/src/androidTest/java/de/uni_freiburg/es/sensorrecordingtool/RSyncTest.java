package de.uni_freiburg.es.sensorrecordingtool;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import org.junit.Test;

/**
 * Created by paulgavrikov on 13.06.17.
 */

public class RSyncTest {

    @Test
    public void testRSync() throws Exception{
        Context c = InstrumentationRegistry.getTargetContext();
        RSyncProcess rSyncProcess = new RSyncProcess.Builder().build(c);
        rSyncProcess.waitFor();
    }

}
