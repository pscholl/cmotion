package de.uni_freiburg.es.wearforwarder;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import org.json.JSONException;

import java.util.LinkedList;

import de.uni_freiburg.es.intentforwarder.ForwardedUtils;

/**
 * A Service which is responsible for forwarding recording Intents to the Services running on
 * the Wear Device.
 * <p>
 * Created by phil on 2/24/16.
 */
public class WearForwarderService extends WearableListenerService
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String WEAR_FORWARD_PATH = "/senserec_wear";
    private static final String TAG = WearForwarderService.class.getName();
    private GoogleApiClient mGoogleApiClient;
    private LinkedList<Intent> mQ = new LinkedList<Intent>();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        /**
         * only forward the ones that have not yet been forwarded.
         */
        if (intent == null ||
                intent.getAction() == null ||
                intent.getAction().contains("BOOT"))
            return Service.START_NOT_STICKY;

        intent.putExtra("forwarded", true); // flag it as forwarded intent

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

        return Service.START_NOT_STICKY;
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (!messageEvent.getPath().equalsIgnoreCase(WEAR_FORWARD_PATH))
            return;

        try {
            Intent i = ForwardedUtils.fromJson(messageEvent.getData());
            i.putExtra(WearForwarder.EXTRA_DOWEARFORWARD, false);
            sendBroadcast(i);
            Log.d(TAG, "forwarded intent " + i);
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

        Intent tofw = mQ.pollLast();
        tofw.putExtra(WearForwarder.EXTRA_DOWEARFORWARD, false);

        final byte[] msg = ForwardedUtils.toJson(tofw).toString().getBytes();

        /*
         * define a callback for the ack of sending the message.
         */
        final ResultCallback<MessageApi.SendMessageResult> txack =
                new CustomResultCallBack(tofw) {

                    @Override
                    public void onResult(MessageApi.SendMessageResult result) {
                        if (result.getRequestId() == MessageApi.UNKNOWN_REQUEST_ID) {
                            Log.d(TAG, "messaging failed " + result.getStatus());
                            mQ.add(this.currentmsg);
                            return; // TODO messaging failed.
                        }
                        Log.d(TAG, "message sent " + result.getStatus());


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

        if(mQ.size() > 0)
            forwardNextIntent();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.e(TAG, "Connected");
        forwardNextIntent();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "Suspended, reason: " + i);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "Connection Failed, reason: " + connectionResult.describeContents());
    }

    abstract class CustomResultCallBack implements ResultCallback<MessageApi.SendMessageResult> {

        protected Intent currentmsg;

        CustomResultCallBack(Intent currentmsg) {
            this.currentmsg = currentmsg;
        }

    };
}
