package es.uni_freiburg.de.cmotion;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

/**
 * This service picks up messages from the Wear network, augments them with an ID and timestamp
 * and hands them over to the UDPTransport.
 *
 * Created by phil on 1/5/16.
 */
public class WearSensorService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{
    private static final String TAG = WearSensorService.class.getName();
    private GoogleApiClient mApiClient;
    private static final String MESSAGE_API_PATH = "ROTATION_VECTOR_MESSAGE";
    private ArrayList<String> mWearableIdentifcations = new ArrayList<String>();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        /*
         * initialize a wearable connection
         */
        mApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();
        mApiClient.connect();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        mApiClient.disconnect();
        super.onDestroy();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "connected to GoogleApi");
        Wearable.MessageApi.addListener(mApiClient, new MessageApi.MessageListener() {
            @Override
            public void onMessageReceived(MessageEvent messageEvent) {
                String p = messageEvent.getPath();

                if (!p.equalsIgnoreCase(MESSAGE_API_PATH))
                    return;

                String id = messageEvent.getSourceNodeId();
                if (!mWearableIdentifcations.contains(id))
                    mWearableIdentifcations.add(id);

                /*
                 * wrap the quaternion rx'ed from the wearable in an id and a timestamp
                 */
                UDPTransport.getInstance().send(
                        ByteBuffer.allocate(6 * 4).order(ByteOrder.LITTLE_ENDIAN)
                                .putInt(mWearableIdentifcations.indexOf(id) + 1)
                                .put(messageEvent.getData())
                                .array());
            }
        });

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "connection GoogleApi failed: " + connectionResult);
    }
}
