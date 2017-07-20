package de.uni_freiburg.es.sensorrecordingtool.merger.retriever;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import de.uni_freiburg.es.sensorrecordingtool.RecorderStatus;
import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.ConnectionTechnology;
import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.Node;
import de.uni_freiburg.es.sensorrecordingtool.merger.IOUtils;

import static de.uni_freiburg.es.sensorrecordingtool.merger.provider.BTDataProvider.ACTION_PROVIDER_READY;

public class BTDataRetriever extends DataRetriever {

    private static final String TAG = BTDataRetriever.class.getSimpleName();
    private CountDownLatch latch = new CountDownLatch(1);
    private boolean isBroadcastReceiverRegistered = false;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (!intent.getStringExtra(RecorderStatus.RECORDING_UUID).equals(mRecordingUUID)
                    || !intent.getStringExtra(RecorderStatus.ANDROID_ID).equals(mNode.getAid())
                    || !intent.getStringExtra(RecorderStatus.CONNECTIONTECH).equals(ConnectionTechnology.Type.BT_CLASSIC.name()))
                return;

            isBroadcastReceiverRegistered = false;
            mContext.unregisterReceiver(this);
            final String mac = intent.getStringExtra(RecorderStatus.CONNECTIONTECH_ID);
            Log.i(TAG, "connecting to " + mac);
            new Thread() {
                @Override
                public void run() {
                    super.run();
                    doBluetoothExchange(mac);
                }
            }.start();
        }
    };

    private void doBluetoothExchange(String mac) {
        BluetoothSocket bluetoothSocket = null;
        try {
            BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mac);
            bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString(mRecordingUUID));
            bluetoothSocket.connect();
            Log.i(TAG, "connected to " + mac);

            IOUtils.transport(bluetoothSocket.getInputStream(), new FileOutputStream(getDestinationFile()));

        } catch (IOException e) {
            e.printStackTrace();
        } finally {


            if (bluetoothSocket != null)
                try {
                    bluetoothSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }


            latch.countDown();
        }
    }

    public BTDataRetriever(Context context, Node node, String recordingUUID) {
        super(context, node, recordingUUID);
        mContext.registerReceiver(mReceiver, new IntentFilter(ACTION_PROVIDER_READY));
        isBroadcastReceiverRegistered = true;
    }


    @Override
    public void destroy() {
        if (mReceiver != null && isBroadcastReceiverRegistered)
            mContext.unregisterReceiver(mReceiver);
    }

    @Override
    public File getFile() throws InterruptedException {
        latch.await();
        setProgress(1);
        return getDestinationFile();
    }


}
