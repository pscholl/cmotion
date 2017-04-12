package de.uni_freiburg.es.sensorrecordingtool.autodiscovery.responder;


import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import java.util.ArrayList;

import de.uni_freiburg.es.sensorrecordingtool.Recorder;
import de.uni_freiburg.es.sensorrecordingtool.RecorderStatus;
import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.ConnectionTechnology;
import de.uni_freiburg.es.sensorrecordingtool.sensors.Sensor;

public class SRTDiscoveryAdapter extends DiscoveryResponseAdapter {

    private static SRTDiscoveryAdapter sInstance;

    SRTDiscoveryAdapter(Context context) {
        super(context);
    }

    @Override
    public void discover() {

        /**
         * Respond to a incoming discovery intent. Will include Sensors, Android_ID, Platform and send a DISCOVERY_RESPONSE_ACTION Broadcast.
         */

        Intent response = new Intent();
        response.setAction(Recorder.DISCOVERY_RESPONSE_ACTION);

        ArrayList<String> sensorNameList = new ArrayList<>();
        for (Sensor sensor : Sensor.getAvailableSensors(context)) {
            String name = sensor.getStringType();
            if (TextUtils.isEmpty(name))
                name = "to.unknown." + sensor.getStringName().toLowerCase().replace(" ", "_") + "_unknown";
            sensorNameList.add(name);
        }
        response.putExtra(RecorderStatus.SENSORS, sensorNameList.toArray(new String[sensorNameList.size()]));

        ArrayList<ConnectionTechnology> connectionTechnologies = ConnectionTechnology.gatherConnectionList(context);
        String[] conArray = new String[connectionTechnologies.size()];
        String[] conIdArray = new String[connectionTechnologies.size()];

        for (int i = 0; i < connectionTechnologies.size(); i++) {
            conArray[i] = connectionTechnologies.get(i).getType().name();
            conIdArray[i] = connectionTechnologies.get(i).getIdentifier();
        }

        response.putExtra(RecorderStatus.CONNECTIONTECH, conArray);
        response.putExtra(RecorderStatus.CONNECTIONTECH_ID, conIdArray);


        response.putExtra(RecorderStatus.ANDROID_ID, Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID));
        response.putExtra(RecorderStatus.PLATFORM, Build.BOARD);
        context.sendBroadcast(response);
    }

    public static DiscoveryResponseAdapter getInstance(Context context) {
        if(sInstance == null)
            sInstance = new SRTDiscoveryAdapter(context);
        return sInstance;
    }
}
