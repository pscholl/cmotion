package de.uni_freiburg.es.cmotiontheta;

import android.app.IntentService;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import de.uni_freiburg.es.sensorrecordingtool.Recorder;
import de.uni_freiburg.es.sensorrecordingtool.RecorderCommands;

/** Sequencializes request for forwarding the recording commands to the Theta or any compatible
 * OSC camera. This involves several steps:
 *
 *  1. connect to an OSC wifi, if not already
 *  2. open a session with the camera, if there is no valid one
 *  3. send the start or stop capture command
 *  4. on stop capture also close the session
 *
 *  An IntentService is used, to avoid async programming the http request to the camera.
 *
 * Created by phil on 9/29/16.
 */
public class ThetaService extends IntentService {
    protected static final String TAG = ThetaService.class.getSimpleName();
    protected static WifiConfiguration mSavedWifiConf;
    protected static Intent mPendingAction;
    protected static ThetaSession mSession;
    protected WifiManager mWifi;

    public ThetaService() {
        super(ThetaService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        mWifi = (WifiManager) getSystemService(WIFI_SERVICE);

        if (action == null)
            return;

        /** only a single request can be pending at each time, either a recording
         * with video is started or the current is canceled.        */
        if (isVideoRecordingIntent(intent) ||
            action.equals(Recorder.CANCEL_ACTION))
            mPendingAction = intent;

        if (mPendingAction == null)
            return;

        /** one of four states:
         *  1. connected to OSC network -> forward intent
         *  2. OSC network found        -> connect
         *  3. OSC network not found    -> error
         *  4. no ScanResults           -> start scan */
        if (ThetaSession.isOSCNetwork(mWifi)) {
            mSession = onConnectedToOSC();
            mPendingAction = null;
        }

        else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action))
            mPendingAction = onDiscoveryResults();

        else
            mWifi.startScan();
    }

    private boolean isVideoRecordingIntent(Intent intent) {
        boolean hasVideo = false;
        for (String input : RecorderCommands.getStringOrArray(intent, Recorder.RECORDER_INPUT))
            hasVideo |= input.contains("vid");

        return Recorder.RECORD_ACTION.equals(intent.getAction()) && hasVideo;
    }


    /** called when connected to an OSC wifi network.
     *
     * Creates or closes a camera session depending on the currently pendingAction. Also
     * reconnects to the previous network in case of an error or if the session has been closed.
     *
     * @return null on error or session end, a thetasession object otherwise
     */
    protected ThetaSession onConnectedToOSC() {
        if (Recorder.CANCEL_ACTION.equals(mPendingAction.getAction())) {
            if (mSession != null) {
                mSession.stopCapture();
                mSession.closeSession();
                //mSession = null;
            }

            /*
            if (mSavedWifiConf != null) {
                mWifi.disconnect();
                mWifi.enableNetwork(mSavedWifiConf.networkId, true);
                mWifi.reconnect();
                mSavedWifiConf = null;
            }
            */

            return null;
        }

        if (Recorder.RECORD_ACTION.equals(mPendingAction.getAction())) {
            ThetaSession session = mSession != null && mSession.isValid() ?
                                   mSession : new ThetaSession(mWifi).startSession();

            JSONObject options = new JSONObject();
            try {
                options.put("captureMode", "_video");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            try {
                return session  .setOptions(options)
                                .startCapture();
            } catch (Exception e) {
                return null;
            }
        }

        return null;
    }

    /** called when new wifi scan results are there. Checks whether there is an OSC network,
     * i.e. a network which hosts a spherical camera.
     *
     * Connects to the network if it has been found.
     *
     * @return null if not found, pendingAction if the camera is there
     */
    private Intent onDiscoveryResults() {
        ScanResult cam = null;

        for (ScanResult r : mWifi.getScanResults())
            if (r.SSID.indexOf("OSC") != -1 && getConf(r.SSID) != null) {
                cam = r;
                break;
            }

        if (cam == null) {
            Log.d(TAG, "no OSC camera found (or not authenticated), not forwarding");
            return null;
        }

        mSavedWifiConf = getConf(mWifi.getConnectionInfo().getSSID());

        mWifi.disconnect();
        mWifi.enableNetwork(getConf(cam.SSID).networkId, true);
        mWifi.reconnect();

        return mPendingAction;
    }

    private WifiConfiguration getConf(String ssid) {
        if (ssid == null)
            return null;

        List<WifiConfiguration> mConfs = mWifi.getConfiguredNetworks();
        for (WifiConfiguration c : mConfs)
            if (c.SSID != null && c.SSID.contains(ssid))
                return c;

        return null;
    }
}
