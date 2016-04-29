package de.uni_freiburg.es.sensorrecordingtool.commands;

import android.content.Context;
import android.content.Intent;

import de.uni_freiburg.es.sensorrecordingtool.Recorder;

/**
 * A Broadcast receiver that calls the correct services.
 *
 * Created by phil on 2/29/16.
 */
public class startRecording extends android.content.BroadcastReceiver {
    public static final String ACTION = "android.intent.action.SENSOR_RECORD";
    public static final String WEAR_ACTION = "android.intent.action.SENSOR_RECORD_WEAR";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null)
            return;

        /*
         * create a service intent
         */
        Intent service = new Intent();
        service.setAction(startRecording.ACTION);

        if (intent.getExtras() != null)
            service.putExtras(intent.getExtras());

        /*
         * start local service
         */
        service.setClass(context, Recorder.class);
        context.startService(service);

        /*
         * forward to Wear network, to not bind to the WearForward Service, which needs to be in a
         * different module, so we can target API 19 here, we just sent a broadcast intent.
         */
        service.setAction(startRecording.WEAR_ACTION);
        context.sendBroadcast(service);
    }
}
