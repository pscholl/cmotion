package de.uni_freiburg.es.wearforwarder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Simply forward all broadcast to the WearForwarderService for Delivery.
 *
 * Created by phil on 5/5/16.
 */
public class WearForwarder extends BroadcastReceiver {
    public static final String EXTRA_DOWEARFORWARD = "doWearForward";

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean doforward = intent.getBooleanExtra(WearForwarder.EXTRA_DOWEARFORWARD, true);

        if (intent == null)
            return;
        else if (!doforward)
            return;

        intent.setClass(context, WearForwarderService.class);
        context.startService(intent);
    }
}
