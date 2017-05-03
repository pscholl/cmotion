package de.unifreiburg.es.btclocksync;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;

/**
 * Bluetooth Server thread that handles incoming clock sync requests. Will respond with the current
 * timestamp ({@link System#currentTimeMillis()}). Links are closed after the timestamp is sent. Will loop
 * forever waiting for new clients, unless interrupted.
 */
public class ClockSyncServerThread extends Thread {

    private static final String TAG = ClockSyncServerThread.class.getSimpleName();
    private static final long WAIT_TIME = 0;
    private final BluetoothAdapter mBtAdapter;
    BluetoothServerSocket serverSocket = null;

    /**
     * Create a Clock Sync Master.
     *
     * @throws Exception if BT is not supported or enabled.
     */
    public ClockSyncServerThread() throws Exception {

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        // Phone does not support Bluetooth so let the user know and exit.
        if (mBtAdapter == null) {
            throw new Exception("BT not available on this device");
        }

        if (!mBtAdapter.isEnabled())
            throw new Exception("BT not enabled");

        createServerSocket();
    }

    private void createServerSocket() throws IOException {
        serverSocket = mBtAdapter.listenUsingRfcommWithServiceRecord("ESE-Clock-Sync",
                ClockSyncManager.RECORD_UUID);
    }

    public void closePFD(BluetoothServerSocket closeMe) throws Exception
    {
        Field mSocketFld = closeMe.getClass().getDeclaredField("mSocket");
        mSocketFld.setAccessible(true);
        BluetoothSocket btsock = (BluetoothSocket)mSocketFld.get(closeMe);
        Field mPfdFld = btsock.getClass().getDeclaredField("mPfd");
        mPfdFld.setAccessible(true);
        ParcelFileDescriptor pfd = (ParcelFileDescriptor)mPfdFld.get(btsock);
        pfd.close();
    }

    @Override
    public void interrupt() {
        super.interrupt();
        if(serverSocket != null)
            try {
                serverSocket.close();
            } catch (IOException e) {

            }
    }

    /**
     * Loops forever waiting for clients, if a client connects, we will send it the current
     * timestamp and close the link. We are waiting for {@link #WAIT_TIME} ms after each connection before sending
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


                BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                br.readLine();
                long clientTime = Long.valueOf(br.readLine());

                Log.e(TAG, (System.currentTimeMillis() - clientTime)+"ms offset found");

                BufferedWriter bw = new BufferedWriter(
                        new OutputStreamWriter(clientSocket.getOutputStream()));
                bw.write("\n");
                bw.write(System.currentTimeMillis()+"\n");
                bw.flush();
                bw.close();
                br.close();
                clientSocket.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            serverSocket.close();
//            closePFD(serverSocket);
            serverSocket = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
