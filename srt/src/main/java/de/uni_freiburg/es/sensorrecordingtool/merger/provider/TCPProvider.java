package de.uni_freiburg.es.sensorrecordingtool.merger.provider;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import de.uni_freiburg.es.sensorrecordingtool.RecorderStatus;
import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.ConnectionTechnology;
import de.uni_freiburg.es.sensorrecordingtool.merger.IOUtils;

import static de.uni_freiburg.es.sensorrecordingtool.merger.provider.BTDataProvider.ACTION_PROVIDER_READY;


public class TCPProvider extends DataProvider {


    private static final String TAG = TCPProvider.class.getSimpleName();

    public TCPProvider(Context context) {
        super(context);
    }

    @Override
    public void serve(String recordingUUID, File file) {

        try {
            ServerSocket serverSocket = new ServerSocket(0); // assign any free port
            sendProviderReadyIntent(recordingUUID, serverSocket.getLocalPort());
            Socket clientSocket = serverSocket.accept();
            Log.i(TAG, clientSocket.getInetAddress().toString() + " connected!");
            IOUtils.transport(new FileInputStream(file), clientSocket.getOutputStream());
            clientSocket.close();
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void sendProviderReadyIntent(String recordingUUID, int port) {
        Intent intent = new Intent(ACTION_PROVIDER_READY);

        String addr = ConnectionTechnology.getLocalWifiAddress(mContext) + ":" + port;

        intent.putExtra(RecorderStatus.CONNECTIONTECH_ID, addr);
        intent.putExtra(RecorderStatus.CONNECTIONTECH, ConnectionTechnology.Type.TCP_OVER_WIFI.name());
        intent.putExtra(RecorderStatus.RECORDING_UUID, recordingUUID);
        intent.putExtra(RecorderStatus.ANDROID_ID, Settings.Secure.getString(mContext.getContentResolver(),
                Settings.Secure.ANDROID_ID));
        Log.i(TAG, addr + " listening");
        mContext.sendBroadcast(intent);
    }
}
