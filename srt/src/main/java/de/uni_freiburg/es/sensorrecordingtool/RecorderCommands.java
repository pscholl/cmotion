package de.uni_freiburg.es.sensorrecordingtool;

import android.content.Context;
import android.content.Intent;

/** A Broadcast receiver that calls the correct services.
 *
 * Created by phil on 2/29/16.
 */
public class RecorderCommands extends android.content.BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null)
            return;

        /** simply forward this intent to the service */
        intent.setClass(context, Recorder.class);
        context.startService(intent);
    }
}
