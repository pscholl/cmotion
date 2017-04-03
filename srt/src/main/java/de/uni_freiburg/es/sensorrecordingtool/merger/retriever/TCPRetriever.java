package de.uni_freiburg.es.sensorrecordingtool.merger.retriever;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

import de.uni_freiburg.es.sensorrecordingtool.RecorderStatus;
import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.ConnectionTechnology;
import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.Node;
import de.uni_freiburg.es.sensorrecordingtool.merger.IOUtils;
import de.uni_freiburg.es.sensorrecordingtool.merger.provider.BTDataProvider;

public class TCPRetriever extends DataRetriever {

    private static final String TAG = TCPRetriever.class.getSimpleName();
    private CountDownLatch latch = new CountDownLatch(1);
    private boolean isBroadcastReceiverRegistered = false;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (!intent.getStringExtra(RecorderStatus.RECORDING_UUID).equals(mRecordingUUID)
                    || !intent.getStringExtra(RecorderStatus.ANDROID_ID).equals(mNode.getAid())
                    || !intent.getStringExtra(RecorderStatus.CONNECTIONTECH).equals(ConnectionTechnology.Type.TCP_OVER_WIFI.name()))
                return;

            isBroadcastReceiverRegistered = false;
            mContext.unregisterReceiver(this);
            final String ip = intent.getStringExtra(RecorderStatus.CONNECTIONTECH_ID);
            Log.i(TAG, "connecting to " + ip);
            new Thread() {
                @Override
                public void run() {
                    super.run();
                    String host = ip.split(":")[0];
                    int port = Integer.parseInt(ip.split(":")[1]);
                    try {
                        Socket socket = new Socket(host, port);
                        IOUtils.transport(socket.getInputStream(), new FileOutputStream(getDestinationFile()));
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    };


    public TCPRetriever(Context context, Node node, String recordingUUID) {
        super(context, node, recordingUUID);
        mContext.registerReceiver(mReceiver, new IntentFilter(BTDataProvider.ACTION_PROVIDER_READY));
        isBroadcastReceiverRegistered = true;
    }

    @Override
    public void destroy() {
        if (mReceiver != null && isBroadcastReceiverRegistered)
            mContext.unregisterReceiver(mReceiver);
    }

    @Override
    public File getFile() {
        try {
            latch.await();
            return getDestinationFile();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }
}
