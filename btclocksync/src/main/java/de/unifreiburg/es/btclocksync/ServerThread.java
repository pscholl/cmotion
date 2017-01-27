package de.unifreiburg.es.btclocksync;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * Bluetooth Server thread that handles incoming clock sync requests. Will respond with the current
 * timestamp (System.currentTimeMillis). Links are closed after the timestamp is sent. Will loop
 * forever waiting for new clients, unless interrupted.
 */
public class ServerThread extends Thread {

    private static final String TAG = ServerThread.class.getSimpleName();
    BluetoothServerSocket serverSocket = null;

    /**
     * Create a Clock Sync Master.
     * @throws Exception if BT is not supported or enabled.
     */
    public ServerThread() throws Exception {

        BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        // Phone does not support Bluetooth so let the user know and exit.
        if (mBtAdapter == null) {
            throw new Exception("BT not available on this device");
        }

        if (!mBtAdapter.isEnabled())
            throw new Exception("BT not enabled");
        serverSocket = mBtAdapter.listenUsingInsecureRfcommWithServiceRecord("ESE-Clock-Sync",
                ClockSyncManager.RECORD_UUID);
    }

    /**
     * Loops forever waiting for clients, if a client connects, we will send it the current
     * timestamp and close the link. We are waiting for 50ms after each connection before sending
     * data since the slave may otherwise have trouble to get data.
     */
    @Override
    public void run() {
        super.run();
        while (!isInterrupted()) {
            try {
                BluetoothSocket clientSocket = serverSocket.accept();
                Log.i(TAG, "slave: " + clientSocket.getRemoteDevice().getName() + " " +
                        clientSocket.getRemoteDevice().getAddress() + " connected!");


                try {
                    Thread.sleep(50); // if we remove this, slave will receive a bunch of errors,
                    // waiting for isConnected doesnt help either, cause fuck logic.
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                PrintWriter bw = new PrintWriter(
                        new OutputStreamWriter(clientSocket.getOutputStream()));
                bw.println(System.currentTimeMillis());
                bw.flush();
                bw.close();
                clientSocket.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
