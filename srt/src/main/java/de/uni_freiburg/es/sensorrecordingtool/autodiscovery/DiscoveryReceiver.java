package de.uni_freiburg.es.sensorrecordingtool.autodiscovery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;

import de.uni_freiburg.es.sensorrecordingtool.Recorder;
import de.uni_freiburg.es.sensorrecordingtool.RecorderStatus;

/**
 * Created by paulgavrikov on 19/11/2016.
 */

public class DiscoveryReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e("DiscoveryReceiver", intent.getAction());
        if (intent.getAction().equals(Recorder.DISCOVERY_ACTION)) {
            respond(context);
        }
    }


    /**
     * Respond to a incoming discovery intent. Will include Sensors, Android_ID, Platform and send a DISCOVERY_RESPONSE_ACTION Broadcast.
     */
    private void respond(Context context) {
        Intent response = new Intent();
        response.setAction(Recorder.DISCOVERY_RESPONSE_ACTION);

        ArrayList<String> sensorNameList = new ArrayList<>();
        for (de.uni_freiburg.es.sensorrecordingtool.sensors.Sensor sensor : de.uni_freiburg.es.sensorrecordingtool.sensors.Sensor.getAvailableSensors(context)) {
            String name = sensor.getStringType();
            if(TextUtils.isEmpty(name))
                name = "to.unknown."+sensor.getStringName().toLowerCase().replace(" ","_")+"_unknown";
            sensorNameList.add(name);
        }

        response.putExtra(RecorderStatus.SENSORS, sensorNameList.toArray(new String[sensorNameList.size()]));

        response.putExtra(RecorderStatus.ANDROID_ID, Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID));
        response.putExtra(RecorderStatus.PLATFORM, Build.BOARD);
        context.sendBroadcast(response);
    }
}
