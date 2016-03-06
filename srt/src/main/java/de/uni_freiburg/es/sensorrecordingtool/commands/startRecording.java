package de.uni_freiburg.es.sensorrecordingtool.commands;

import android.content.Context;
import android.content.Intent;

import de.uni_freiburg.es.sensorrecordingtool.Recorder;
import de.uni_freiburg.es.sensorrecordingtool.WearForwarder;

/**
 * A Broadcast receiver that calls the correct services.
 *
 * Created by phil on 2/29/16.
 */
public class startRecording extends android.content.BroadcastReceiver {
    public static final String ACTION = "android.intent.action.SENSOR_RECORD";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null)
            return;

        /*
         * create a service intent
         */
        Intent service = new Intent();
        intent.setAction(startRecording.ACTION);

        if (intent.getExtras() != null)
            service.putExtras(intent.getExtras());

        /*
         * forward to Wear network
         */
        service.setClass(context, WearForwarder.class);
        context.startService(service);

        /*
         * start local service
         */
        service.setClass(context, Recorder.class);
        context.startService(service);
    }
}
