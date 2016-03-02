package de.uni_freiburg.es.sensorrecordingtool;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Node;

import java.util.Iterator;
import java.util.LinkedList;

/** A Service which is responsible for forwarding recording Intents to the Services running on
 * the Wear Device.
 *
 * Created by phil on 2/24/16.
 */
public class WearForwarder extends WearableListenerService
    implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    static final String RECORD_ACTION_FORWARDED = "RECORD_ACTION_FORWARDED";
    private static final String WEAR_FORWARD_PATH = "/senserec_wear";
    private static final String TAG = WearForwarder.class.getName();
    private GoogleApiClient mGoogleApiClient;
    private LinkedList<Intent> mQ = new LinkedList<>();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        /**
         * only forward the ones that have not yet been forwarded.
         */
        if (intent == null)
            return START_NOT_STICKY;

        /**
         * add up all intent into a queue, and whenever we have a connection to the google
         * API client, we start sending out the Q until it is empty again.
         */
        mQ.add(intent);

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            mGoogleApiClient.connect();
        } else if (!mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();
        else
            forwardNextIntent();

        return START_NOT_STICKY;
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (!messageEvent.getPath().equalsIgnoreCase(WEAR_FORWARD_PATH))
            return;

        try {
            JSONObject o = new JSONObject(new String(messageEvent.getData()));
            Bundle bundle = fromJson(o);
            Log.d(TAG, "rx'ed msg " + o.toString());

            Intent omgwtf = new Intent(this, Recorder.class);
            omgwtf.setAction(RECORD_ACTION_FORWARDED);
            omgwtf.putExtras(bundle);
            startService(omgwtf);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void forwardNextIntent() {
        /*
         * check if there is something and serialize.
         */
        Log.d(TAG, "forwarding messages " + mQ.size());

        if (mQ.size() <= 0) return;
        final byte[] msg = toJson(mQ.peekFirst().getExtras()).toString().getBytes();

        /*
         * define a callback for the ack of sending the message.
         */
        final ResultCallback<MessageApi.SendMessageResult> txack =
        new ResultCallback<MessageApi.SendMessageResult>() {
            private Intent currentmsg = mQ.peekFirst();

            @Override
            public void onResult(MessageApi.SendMessageResult result) {
                if (result.getRequestId() == MessageApi.UNKNOWN_REQUEST_ID) {
                    Log.d(TAG, "messaging failed " + result.getStatus());
                    return; // TODO messaging failed.
                }
                Log.d(TAG, "message sent " + result.getStatus());
                mQ.remove(currentmsg); // message sent succesfully, remove from the Q
            }
        };

        /*
         * define a callback for getting a list of all nodes currently connected:
         *  forward the intent to all connected nodes
         */
        final ResultCallback<NodeApi.GetConnectedNodesResult> tx =
        new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult result) {
                for (com.google.android.gms.wearable.Node n : result.getNodes()) {
                    PendingResult<MessageApi.SendMessageResult> msgresult =
                    Wearable.MessageApi.sendMessage(mGoogleApiClient,
                            n.getId(), WEAR_FORWARD_PATH, msg);
                    Log.d(TAG, "send message to " + n.getId());
                    msgresult.setResultCallback(txack);
                }
            }
        };

        /*
         * now finally, get all connected nodes and do something.
         */
        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(tx);
    }

    public static Bundle fromJson(JSONObject s) {
        Bundle bundle = new Bundle();

        for (Iterator<String> it = s.keys(); it.hasNext(); ) {
            String key = it.next();
            JSONArray arr = s.optJSONArray(key);
            Double num = s.optDouble(key);
            String str = s.optString(key);

            if (arr != null && arr.length() <= 0)
                bundle.putStringArray(key, new String[]{});

            else if (arr != null && !Double.isNaN(arr.optDouble(0))) {
                double[] newarr = new double[arr.length()];
                for (int i=0; i<arr.length(); i++)
                    newarr[i] = arr.optDouble(i);
                bundle.putDoubleArray(key, newarr);
            }

            else if (arr != null && arr.optString(0) != null) {
                String[] newarr = new String[arr.length()];
                for (int i=0; i<arr.length(); i++)
                    newarr[i] = arr.optString(i);
                bundle.putStringArray(key, newarr);
            }

            else if (!num.isNaN())
                bundle.putDouble(key, num);

            else if (str != null)
                bundle.putString(key, str);

            else
                System.err.println("unable to transform json to bundle " + key);
        }

        return bundle;
    }

    public static JSONObject toJson(Bundle bundle) {
        JSONObject json = new JSONObject();

        if (bundle == null)
            return json;

        for (String key : bundle.keySet())
            try {
                json.put(key, JSONObject.wrap(bundle.get(key)));
            } catch (JSONException e) {
                e.printStackTrace();
            }

        return json;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) { forwardNextIntent(); }

    @Override
    public void onConnectionSuspended(int i) { }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        System.err.println(connectionResult.describeContents());
    }
}
