package de.uni_freiburg.es.sensorrecordingtool;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

import de.uni_freiburg.es.intentforwarder.IntentForwarder;
import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.DiscoveryReceiver;

import static android.support.test.InstrumentationRegistry.getInstrumentation;

/**
 * An abstract class to be implemented by all tests. Will enable / disable BroadcastReceivers on
 * demand and prevent the real up from interacting with test receivers.
 */
@RunWith(AndroidJUnit4.class)
public abstract class BroadcastingTest {


    private final Class[] mReceivers = new Class[]{RecorderCommands.class, DiscoveryReceiver.class, IntentForwarder.class};

    @Before
    public void startAllReceivers() {
        for (Class receiver : mReceivers)
            enableBroadcastReceiver(receiver, true);
    }

    @After
    public void destroyAllReceivers() {
        for (Class receiver : mReceivers)
            enableBroadcastReceiver(receiver, false);
    }

    private void enableBroadcastReceiver( Class<? extends BroadcastReceiver> receiver, boolean on) {
        Context context = getInstrumentation().getTargetContext();
        ComponentName component = new ComponentName(context, receiver);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(component,
                (on ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED),
                PackageManager.DONT_KILL_APP);
    }

}
