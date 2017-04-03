package de.uni_freiburg.es.sensorrecordingtool.merger.provider;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;

import de.uni_freiburg.es.sensorrecordingtool.RecorderStatus;
import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.ConnectionTechnology;
import de.uni_freiburg.es.sensorrecordingtool.merger.IOUtils;

public class BTDataProvider extends DataProvider {

    private final String TAG = BTDataProvider.class.getSimpleName();
    public static final String ACTION_PROVIDER_READY = "recorder_provider_ready";

    private BluetoothAdapter mBtAdapter;
    private BluetoothServerSocket mServerSocket;

    public BTDataProvider(Context context) {
        super(context);
    }

    @Override
    public void serve(String recordingUUID, File file) {

        assert !file.isDirectory() && file.canRead();

        BluetoothServerSocket serverSocket = null;
        BluetoothSocket clientSocket = null;

        try {
            serverSocket = BluetoothAdapter.getDefaultAdapter().listenUsingInsecureRfcommWithServiceRecord("Merge-Sync",
                    UUID.fromString(recordingUUID));
            sendProviderReadyIntent(recordingUUID); // notify the master about our MAC
            clientSocket = serverSocket.accept();

            IOUtils.transport(new FileInputStream(file), clientSocket.getOutputStream());
            Log.i(TAG, "Wrote " + file.length() + "bytes over Bluetooth");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            if(clientSocket != null)
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            if(serverSocket != null)
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    private void sendProviderReadyIntent(String recordingUUID) {
        Intent intent = new Intent(ACTION_PROVIDER_READY);
        String btAddr = Settings.Secure.getString(mContext.getContentResolver(), "bluetooth_address");
        intent.putExtra(RecorderStatus.CONNECTIONTECH_ID, btAddr);
        intent.putExtra(RecorderStatus.CONNECTIONTECH, ConnectionTechnology.Type.BT_CLASSIC.name());
        intent.putExtra(RecorderStatus.RECORDING_UUID, recordingUUID);
        intent.putExtra(RecorderStatus.ANDROID_ID, Settings.Secure.getString(mContext.getContentResolver(),
                Settings.Secure.ANDROID_ID));
        mContext.sendBroadcast(intent);
    }


}
