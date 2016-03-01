package de.uni_freiburg.es.sensorrecordingtool;

import android.content.Context;
import android.content.Intent;

/**
 * This class simply forwards all Intents to the recorder service.
 *
 * Created by phil on 2/29/16.
 */
public class StartRecordingReceiver extends android.content.BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null)
            return;

        Intent service = new Intent(context, Recorder.class);
        service.setAction(Recorder.RECORD_ACTION);

        if (intent.getExtras() != null)
            service.putExtras(intent.getExtras());

        context.startService(service);
    }
}
