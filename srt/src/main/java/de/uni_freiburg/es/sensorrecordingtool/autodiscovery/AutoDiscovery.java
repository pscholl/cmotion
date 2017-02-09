package de.uni_freiburg.es.sensorrecordingtool.autodiscovery;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
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
    private static AutoDiscovery sInstance;
    private final Context mContext;

    private ArrayList<Node> mDiscoveredList = new ArrayList<>();
    private List<OnNodeSensorsDiscoveredListener> mListeners = new ArrayList<>();

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
        String aid = intent.getStringExtra(RecorderStatus.ANDROID_ID);
        String[] sensors = intent.getStringArrayExtra(RecorderStatus.SENSORS);

        Node node = new Node(platform, aid);

        Log.e(TAG, String.format("Device %s has following sensors: %s", platform, sensors != null ? Arrays.toString(sensors) : "[]"));

        if (!mDiscoveredList.contains(node))
            mDiscoveredList.add(node);
        else node = mDiscoveredList.get(mDiscoveredList.indexOf(node));

        if (sensors != null) {
            node.setAvailableSensors(sensors);
        }

        for (OnNodeSensorsDiscoveredListener listener : mListeners)
            listener.onNodeSensorsDiscovered(node, sensors);
    }


    public static AutoDiscovery getInstance(Context context) {
        if (sInstance == null)
            sInstance = new AutoDiscovery(context);
        return sInstance;
    }

    private void bind() {
        mContext.registerReceiver(mMasterReceiver, new IntentFilter(Recorder.DISCOVERY_RESPONSE_ACTION));
    }

    private AutoDiscovery(Context context) {
        mContext = context;
        bind();
    }

    /**
     * Starts the autodiscovery asynchronously. Will remove all previously discovered devices.
     */
    public void discover() {
//        mDiscoveredMap = new HashMap<>();
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
        try {
            mContext.unregisterReceiver(mMasterReceiver);
        } catch (IllegalArgumentException e) {}
    }

    /**
     * Sets the OnNodeSensorsDiscoveredListener.
     *
     * @param listener
     */
    public void setListener(OnNodeSensorsDiscoveredListener listener) {
        this.mListeners.add(listener);
    }


    public ArrayList<Node> getDiscoveredSensors() {
        return mDiscoveredList;
    }

    public int getConnectedNodes() {
        return mDiscoveredList.size();
    }
}
