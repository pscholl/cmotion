package de.uni_freiburg.es.sensorrecordingtool.autodiscovery.responder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import de.uni_freiburg.es.sensorrecordingtool.Recorder;

/**
 * {@link BroadcastReceiver} for incoming {@link Recorder#DISCOVERY_ACTION} Intents. Will launch all
 * known {@link DiscoveryResponseAdapter} to repsond.
 */
public class DiscoveryResponderReceiver extends BroadcastReceiver {
    private static final String TAG = DiscoveryResponderReceiver.class.getSimpleName().substring(0, 16);

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(TAG, intent.getAction());
        if (intent.getAction().equals(Recorder.DISCOVERY_ACTION)) {
            SRTDiscoveryAdapter.getInstance(context).discover();
            ThetaDiscoveryAdapter.getInstance(context).discover();
        }
    }
}
