package de.uni_freiburg.es.cmotiontheta;

import android.net.wifi.WifiManager;
import android.util.Log;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.io.IOException;

/**
 * Created by phil on 9/30/16.
 */
public class ThetaSession  {
    private static final String TAG = ThetaSession.class.getSimpleName();
    private final WifiManager mWifi;
    private JSONObject mSessionId;

    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");
    private OkHttpClient mClient = new OkHttpClient();

    public ThetaSession(WifiManager wifi) {
        mWifi = wifi;
        mSessionId = null;

        mClient.setConnectTimeout(5, TimeUnit.SECONDS);
        mClient.setReadTimeout(5, TimeUnit.SECONDS);
        mClient.setWriteTimeout(5, TimeUnit.SECONDS);
    }

    public ThetaSession startSession() throws IOException {
        try {
            JSONObject json = new JSONObject();
            json.put("name", "camera.startSession");
            json.put("parameters", new JSONObject());

            Request req =
                getOSCConnection(mWifi, "/osc/commands/execute")
                    .post(RequestBody.create(JSON, json.toString()))
                    .build();

            Response rsp = mClient.newCall(req).execute();
            String body  = rsp.body().string();
            rsp.body().close();

            if (!rsp.isSuccessful())
                return null;

            mSessionId = new JSONObject()
                    .put("sessionId", new JSONObject(body)
                    .getJSONObject("results")
                    .getString("sessionId"));

            System.err.printf("started Session %s", mSessionId.toString());

            return this;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public ThetaSession stopCapture() throws IOException {
        try {
            JSONObject json = new JSONObject();
            json.put("name", "camera._stopCapture");
            json.put("parameters", mSessionId);

            System.err.printf("stopping Session %s", mSessionId.toString());

            Request r =
                getOSCConnection(mWifi, "/osc/commands/execute")
                    .post(RequestBody.create(JSON, json.toString()))
                    .build();

            Response rsp = mClient.newCall(r).execute();
            rsp.body().close();
            return rsp.isSuccessful() ? this : null;
        } catch (JSONException e) {
          e.printStackTrace();
          return this;
        }
    }

    public ThetaSession startCapture() throws IOException {
        try {
            JSONObject json = new JSONObject();
            json.put("name", "camera._startCapture");
            json.put("parameters", mSessionId);

            Request r =
                    getOSCConnection(mWifi, "/osc/commands/execute")
                            .post(RequestBody.create(JSON, json.toString()))
                            .build();

            Response rsp = mClient.newCall(r).execute();
            rsp.body().close();
            return rsp.isSuccessful() ? this : null;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public ThetaSession closeSession() {
        try {
            JSONObject json = new JSONObject();
            json.put("name", "camera.closeSession");
            json.put("parameters", mSessionId);

            Request r =
                    getOSCConnection(mWifi, "/osc/commands/execute")
                            .post(RequestBody.create(JSON, json.toString()))
                            .build();

            Response rsp = mClient.newCall(r).execute();
            rsp.body().close();

            if (rsp.isSuccessful())
                mSessionId = null;

            return rsp.isSuccessful() ? this : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public ThetaSession setOptions(JSONObject options) {
        try {
            JSONObject json = new JSONObject();

            json.put("name", "camera.setOptions");
            json.put("parameters", new JSONObject()
                .put("sessionId", mSessionId.getString("sessionId"))
                .put("options", options));

            Request r =
                getOSCConnection(mWifi, "/osc/commands/execute")
                    .post(RequestBody.create(JSON, json.toString()))
                    .build();

            Response rsp = mClient.newCall(r).execute();
            rsp.body().close();
            return rsp.isSuccessful() ? this : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public ThetaSession getOptions(JSONObject options, String...names) {
        try {
            JSONObject json = new JSONObject(), opts = new JSONObject();
            opts.put("clientVersion", 2);

            json.put("name", "camera.getOptions");
            json.put("parameters", new JSONObject()
                    .put("sessionId", mSessionId.getString("sessionId"))
                    .put("optionNames", new JSONArray(names)));

            Request r =
                    getOSCConnection(mWifi, "/osc/commands/execute")
                            .post(RequestBody.create(JSON, json.toString()))
                            .build();

            Response rsp = mClient.newCall(r).execute();
            rsp.body().close();

            if (rsp.isSuccessful()) {
                json = new JSONObject(rsp.body().string()).getJSONObject("results").getJSONObject("options" +
                        "");
                for (Iterator<String> it = json.keys(); it.hasNext();) {
                    String key = it.next();
                    options.put(key, json.get(key));
                }
            }

            return rsp.isSuccessful() ? this : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Request.Builder getOSCConnection(WifiManager mWifi, String url) {
        if (isOSCNetwork(mWifi))
            return new Request.Builder()
                    .url("http://"+intToIp(mWifi.getDhcpInfo().gateway)+url);

        return null;
    }

    /** checks if the currently connected WiFi network hosts an OSC camera */
    public static boolean isOSCNetwork(WifiManager mWifi) {
        return mWifi.getDhcpInfo() != null &&
               mWifi.getConnectionInfo().getSSID() != null &&
               mWifi.getDhcpInfo().gateway != 0 && // disconnected
               mWifi.getConnectionInfo().getSSID().contains("OSC");
    }

    public static String intToIp(int i) {
        return ( i & 0xFF) + "." +
                ((i >> 8 ) & 0xFF) + "." +
                ((i >> 16 ) & 0xFF) + "." +
                ((i >> 24 ) & 0xFF )
                ;
    }

    public boolean isValid() {
        return mSessionId != null;
    }
}
