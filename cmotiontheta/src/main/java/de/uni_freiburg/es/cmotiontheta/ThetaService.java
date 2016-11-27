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
            mSession = retryOnConnectedToOSC(15);
            mPendingAction = null;
        }

        else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action))
            mPendingAction = onDiscoveryResults();

        else
            mWifi.startScan();
    }

    private boolean isVideoRecordingIntent(Intent intent) {
        /* on every recording, try to start an OSC
        boolean hasVideo = false;
        for (String input : RecorderCommands.getStringOrArray(intent, Recorder.RECORDER_INPUT))
            hasVideo |= input.contains("vid");

        return Recorder.RECORD_ACTION.equals(intent.getAction()) && hasVideo;
        */
        return Recorder.RECORD_ACTION.equals(intent.getAction());
    }

    protected ThetaSession retryOnConnectedToOSC(int times) {
      for (int i=0; i<times; i++)
        try { return onConnectedToOSC(); }
        catch(Exception e) {
          System.err.printf("%s: connect to OSC failed (%d times) %s\n", TAG,
              i, i<times ? ", retrying..." : ", failed");
          e.printStackTrace();
          sleep(200);
      }
      return null;
    }

    protected void sleep(int ms) {
      try { Thread.sleep(ms); }
      catch(Exception e) {};
    }

    /** called when connected to an OSC wifi network.
     *
     * Creates or closes a camera session depending on the currently pendingAction. Also
     * reconnects to the previous network in case of an error or if the session has been closed.
     *
     * @return null on error or session end, a thetasession object otherwise
     */
    protected ThetaSession onConnectedToOSC() throws Exception {
        if (Recorder.CANCEL_ACTION.equals(mPendingAction.getAction())) {
            if (mSession != null) {
                mSession.stopCapture();
                mSession.closeSession();
                //mSession = null; can re-use session
            }

            if (mSavedWifiConf != null) {
                connectTo(mSavedWifiConf.networkId);
                mSavedWifiConf = null;
            }

            return null;
        }

        if (Recorder.RECORD_ACTION.equals(mPendingAction.getAction())) {
            ThetaSession session = mSession != null && mSession.isValid() ?
                                   mSession : new ThetaSession(mWifi).startSession();

            JSONObject options = new JSONObject();
            options.put("captureMode", "_video");
            return session
                   .setOptions(options)
                   .startCapture();
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
        connectTo( getConf(cam.SSID).networkId );
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

    private void connectTo(int nid) {
        try {
          mWifi.disconnect(); Thread.sleep(200);
          mWifi.enableNetwork(nid, true); Thread.sleep(200);
          mWifi.reconnect(); Thread.sleep(200);
        } catch (Exception e) {}
    }
}
