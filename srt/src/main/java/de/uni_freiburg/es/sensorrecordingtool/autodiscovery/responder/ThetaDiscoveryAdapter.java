package de.uni_freiburg.es.sensorrecordingtool.autodiscovery.responder;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import de.uni_freiburg.es.sensorrecordingtool.Recorder;
import de.uni_freiburg.es.sensorrecordingtool.RecorderStatus;
import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.ConnectionTechnology;

/**
 * Discovery Adapter for Theta Devices. Will use WiFi Scanning to find Theta etworks. Will also check already connected networks.
 */
public class ThetaDiscoveryAdapter extends DiscoveryResponseAdapter {

    private static final String TAG = ThetaDiscoveryAdapter.class.getSimpleName();
    private static final long SCANTIME = 5000;
    private static ThetaDiscoveryAdapter mInstance = null;
    private WifiManager mWifiManager;
    private Handler mHandler = new Handler();

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {
                onDiscoveryResults();
            }
        }
    };

    private ThetaDiscoveryAdapter(Context context) {
        super(context);
        mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    public static ThetaDiscoveryAdapter getInstance(Context context) {
        if (mInstance == null)
            mInstance = new ThetaDiscoveryAdapter(context);
        return mInstance;
    }

    public void discover() {
        if (!mWifiManager.isWifiEnabled())
            return; // abort, wifi not enabled

        if (isOSCNetwork(mWifiManager)) { // we are connected to OSC
            int ipAddress = mWifiManager.getConnectionInfo().getIpAddress();
            @SuppressLint("DefaultLocale") String ip = String.format("%d.%d.%d.%d",
                    (ipAddress & 0xff),
                    (ipAddress >> 8 & 0xff),
                    (ipAddress >> 16 & 0xff),
                    (ipAddress >> 24 & 0xff));
            respond(ip);
            return; // no need to scan
        }

        startScanning();
    }

    /**
     * called when new wifi scan results are there. Checks whether there is an OSC network,
     * i.e. a network which hosts a spherical camera.
     */
    private void onDiscoveryResults() {
        ScanResult cam = null;

        for (ScanResult r : mWifiManager.getScanResults())
            if (r.SSID.contains("OSC") && getConf(r.SSID) != null) {
                cam = r;
                break;
            }

        if (cam == null) {
            Log.d(TAG, "no OSC camera found (or not authenticated)");
        } else
            respond(null);

    }

    /**
     * Starts WiFi scanning for a scheduled duration (defined by {@link #SCANTIME}). Will register
     * the relevant BroadcastReceiver and unregistered it after the scan timeout is reached.
     */
    private void startScanning() {
        mHandler.removeCallbacksAndMessages(null);
        try {
            context.getApplicationContext().registerReceiver(mReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        } catch (Exception e) {
            e.printStackTrace();
        }

        mWifiManager.startScan();

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    context.getApplicationContext().unregisterReceiver(mReceiver);
                } catch (Exception ignored) {
                }
            }
        }, SCANTIME);

    }

    /**
     * Sends the reponse Intent, containing only one video sensor for the theta platform.
     *
     * @param ip
     */
    private void respond(String ip) {
        Intent response = new Intent();
        response.setAction(Recorder.DISCOVERY_RESPONSE_ACTION);
        ArrayList<String> sensorNameList = new ArrayList<>();
        sensorNameList.add("android.hardware.video_front");
        response.putExtra(RecorderStatus.SENSORS, sensorNameList.toArray(new String[sensorNameList.size()]));
        String[] conArray = new String[1];
        String[] conIdArray = new String[1];
        conArray[0] = ConnectionTechnology.Type.TCP_OVER_WIFI.name();
        conIdArray[0] = ip;
        response.putExtra(RecorderStatus.CONNECTIONTECH, conArray);
        response.putExtra(RecorderStatus.CONNECTIONTECH_ID, conIdArray);
        response.putExtra(RecorderStatus.ANDROID_ID, "__theta__");
        response.putExtra(RecorderStatus.PLATFORM, "theta");
        response.putExtra(RecorderStatus.AUTONOMOUS, true);
        context.sendBroadcast(response);
    }

    /**
     * Will iterate over all configured networks and match the searched network by its SSID.
     *
     * @param ssid ssid of the network
     * @return WifiConfiguration of the desired network or null
     */
    private WifiConfiguration getConf(String ssid) {
        if (ssid == null)
            return null;

        List<WifiConfiguration> mConfs = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration c : mConfs)
            if (c.SSID != null && c.SSID.contains(ssid))
                return c;

        return null;
    }

    /**
     * checks if the currently connected WiFi network hosts an OSC camera
     */
    public static boolean isOSCNetwork(WifiManager wifi) {
        return wifi.getDhcpInfo() != null &&
                wifi.getConnectionInfo().getSSID() != null &&
                wifi.getDhcpInfo().gateway != 0 && // disconnected
                wifi.getConnectionInfo().getSSID().contains("OSC");
    }
}
