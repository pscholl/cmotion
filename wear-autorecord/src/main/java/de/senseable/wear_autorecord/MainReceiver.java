package de.senseable.wear_autorecord;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.util.Log;

import de.uni_freiburg.es.sensorrecordingtool.Recorder;

/** This receiver starts and stops a sensor recording based on the POWER status of the device:
 *
 *  1. device is plugged off power or device is rebooted and not plugged in
 *  1. delay for 10-minutes and start a recording
 *  1. power is connected -> stop an ongoing recording
 *
 * Created by phil on 04.06.18.
 */

public class MainReceiver extends BroadcastReceiver {

    private static final long DELAY = 100; //10 * 60 * 60 * 1000;
    private static final String TAG =MainReceiver.class.getSimpleName();
    public static Handler mHandler = new Handler();
    public static Runnable mStart = null;

    public static Intent mStopIntent = new Intent(Recorder.CANCEL_ACTION),
                        mStartIntent = new Intent(Recorder.RECORD_ACTION);

    static {
        /*mStartIntent.putExtra(Recorder.RECORDER_INPUT, new String[]{
                "accelerometer", "gyroscope", "magnetometer" });
        mStartIntent.putExtra(Recorder.RECORDER_INPUT, new float[]{
                25.f, 25.f, 25.f});
                */

        mStartIntent.putExtra(Recorder.RECORDER_INPUT, "accelerometer");
        mStartIntent.putExtra(Recorder.RECORDER_RATE, 25.f);
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction();

        if (Intent.ACTION_POWER_CONNECTED == action) {
            // stop an ongoing recording, or a delayed start

            context.sendBroadcast(mStopIntent);
            Log.e(TAG, "power connected -> stopping");
        }
        else if (Intent.ACTION_POWER_DISCONNECTED == action  ||
                 Intent.ACTION_BOOT_COMPLETED == action && !isConnected(context)) {
            // start a new recording

            if (mStart != null)
                mHandler.removeCallbacks(mStart);
            
            mStart = new Runnable() {
                @Override
                public void run() {
                    context.sendBroadcast(mStartIntent);
                }
            };
            
            mHandler.postDelayed(mStart, DELAY);
            Log.e(TAG, "power disconnected -> starting");
        }
    }

    public static boolean isConnected(Context context) {
        Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        return plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB;
    }
}
