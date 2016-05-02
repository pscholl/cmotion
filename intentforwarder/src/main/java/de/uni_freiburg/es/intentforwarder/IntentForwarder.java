package de.uni_freiburg.es.intentforwarder;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Activated on startrecording intent and when new Bluetooth devices are bonded with. Checks
 * if a new bluetooth is bonded, and starts the service or forwards the recording intent to
 * the GlassService directly.
 *
 * Created by phil on 4/29/16.
 */
public class IntentForwarder extends BroadcastReceiver {
    protected static final String TAG = IntentForwarder.class.getName();
    protected static final String EXTRA_DOBLUETOOTHFORWARD = "doBlForward";

    @Override
    public void onReceive(Context context, Intent intent) {
        BluetoothDevice dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        int state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, -1);
        boolean doforward = intent.getBooleanExtra(IntentForwarder.EXTRA_DOBLUETOOTHFORWARD, true);

        if (intent == null || intent.getAction() == null)
            return;
        else if (state != -1 && state != BluetoothAdapter.STATE_CONNECTED)
            return;
        else if (!doforward)
            return; // nothing to do if already forwarded

        /** try and start the service, this makes sure that we're actually listening or
         * forwarding the currently rx'ed action intent. */
        intent.setClass(context, IntentForwarderService.class);
        context.startService(intent);
    }
}
