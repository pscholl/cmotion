package de.uni_freiburg.es.sensorrecordingtool.autodiscovery;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import de.uni_freiburg.es.sensorrecordingtool.Recorder;
import de.uni_freiburg.es.sensorrecordingtool.RecorderStatus;

/**
 * A class for Discovery of available Sensors. Intents send by this object shall be forwarded to all connected nodes.
 * Discovered Devices will include the host.
 */
public class AutoDiscovery {

    private static final String TAG = "AutoDiscovery";
    private static final long DISCOVERY_TIMEOUT = 50000;
    private final Context mContext;

    private HashMap<String, List<String>> mDiscoveredMap = new HashMap<>();
    private Handler mTimingHandler = new Handler();

    private OnNodeSensorsDiscoveredListener mListener;

    /**
     * Listens for Responses.
     */
    private BroadcastReceiver mMasterReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Recorder.DISCOVERY_RESPONSE_ACTION)) {
                interpreteDiscovery(intent);
            }
        }
    };


    /**
     * Interpretes an incoming DISCOVERY_RESPONSE_ACTION Intent and adds values to discovered devices.
     *
     * @param intent A valid and populated DISCOVERY_RESPONSE_ACTION intent
     */
    private void interpreteDiscovery(Intent intent) {
        String platform = intent.getStringExtra(RecorderStatus.PLATFORM);
        String[] sensors = intent.getStringArrayExtra(RecorderStatus.SENSORS);

        Log.e(TAG, String.format("Device %s has following sensors: %s", platform, sensors != null ? Arrays.toString(sensors) : "[]"));

        if (!mDiscoveredMap.containsKey(platform))
            mDiscoveredMap.put(platform, new ArrayList<String>());
        if (sensors != null) {
            for(String sensor : sensors)
            mDiscoveredMap.get(platform).add(sensor);
        }

        if(mListener != null)
            mListener.onNodeSensorsDiscovered(platform, sensors);
    }


    public AutoDiscovery(Context context) {
        mContext = context;
        mContext.registerReceiver(mMasterReceiver, new IntentFilter(Recorder.DISCOVERY_RESPONSE_ACTION));
    }

    /**
     * Starts the autodiscovery asynchronously. Will remove all previously discovered devices.
     */
    public void discover() {
        mDiscoveredMap = new HashMap<>();
//        mTimingHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                close();
//            }
//        }, DISCOVERY_TIMEOUT);

        Intent intent = new Intent();
        intent.setAction(Recorder.DISCOVERY_ACTION);
        mContext.sendBroadcast(intent);
        Log.e(TAG, "send discover action");
    }

    /**
     * Close the discovery by unregistering the BroadcastReceivers. Must be called on Recorder end.
     */
    public void close() {
        mContext.unregisterReceiver(mMasterReceiver);
    }

    /**
     * Sets the OnNodeSensorsDiscoveredListener.
     * @param listener
     */
    public void setListener(OnNodeSensorsDiscoveredListener listener) {
        this.mListener = listener;
    }


    public HashMap<String, List<String>> getDiscoveredSensors() {
        return mDiscoveredMap;
    }

    public int getConnectedNodes() {
        return 1; // TODO
    }
}
