package de.uni_freiburg.es.sensorrecordingtool;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

import de.uni_freiburg.es.intentforwarder.IntentForwarder;
import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.DiscoveryResponderReceiver;
import de.uni_freiburg.es.sensorrecordingtool.merger.MergeService;

import static android.support.test.InstrumentationRegistry.getInstrumentation;

/**
 * An abstract class to be implemented by all tests. Will enable / disable BroadcastReceivers / Services on
 * demand and prevent the real app from interacting with test components.
 */
@RunWith(AndroidJUnit4.class)
public abstract class BroadcastingTest {


    private final Class[] mComponents = new Class[]{RecorderCommands.class, DiscoveryResponderReceiver.class, IntentForwarder.class, Recorder.class, MergeService.class};

    @Before
    public void startAllComponents() {
        for (Class receiver : mComponents)
            enableComponent(receiver, true);
    }

    @After
    public void destroyAllComponents() {
        for (Class receiver : mComponents)
            enableComponent(receiver, false);
    }

    private void enableComponent(Class<?> cl, boolean on) {
        Context context = getInstrumentation().getTargetContext();
        ComponentName component = new ComponentName(context, cl);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(component,
                (on ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED),
                PackageManager.DONT_KILL_APP);
    }

}
