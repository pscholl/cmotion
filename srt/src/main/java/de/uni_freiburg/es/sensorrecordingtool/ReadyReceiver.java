package de.uni_freiburg.es.sensorrecordingtool;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

public class ReadyReceiver extends BroadcastReceiver {

    private final String TAG = ReadyReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (Recorder.READY_ACTION.equals(action) && Recorder.isMaster) {
            Log.e(TAG, String.format("node %s[%s] is ready", intent.getStringExtra(RecorderStatus.ANDROID_ID), intent.getStringExtra(RecorderStatus.PLATFORM)));
            Recorder.SEMAPHORE--;
        } else if (Recorder.STEADY_ACTION.equals(action) && !Recorder.isMaster) {
            long startTime = (long) intent.getDoubleExtra(RecorderStatus.START_TIME, -1);
            Log.e(TAG, "Steady, starting recording at " + startTime);
            long diff = Math.min(startTime - System.currentTimeMillis(), Recorder.DEFAULT_STEADY_TIME); // Due to clock drift
            Log.e(TAG, "Waiting for " + diff + "ms");

            if (diff > 0)
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Recorder.SEMAPHORE--;
                    }
                }, diff);
            else // time was either not set or connection took longer then the wait period was // TODO add error?
                Recorder.SEMAPHORE--;
        }
// else
//            Log.e(TAG, "unknown intent: " + intent.getAction());
    }
}
