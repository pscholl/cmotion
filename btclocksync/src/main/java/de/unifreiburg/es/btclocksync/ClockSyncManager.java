package de.unifreiburg.es.btclocksync;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * A manager class to determine drift against bonded Bluetooth Classic nodes. It is assumed that the
 * calling device supports Bluetooth, and Bluetooth is enabled.
 * We also assume a low latency BT Link.
 */
public class ClockSyncManager {


    private final int MAX_ATTEMPTS = 3;
    private final long MAX_ATTEMPT_MILLIS = 15 * 1000L;

    private static final String TAG = ClockSyncManager.class.getSimpleName();
    /**
     * custom clock sync uuid
     **/
    public static final UUID RECORD_UUID = UUID.fromString("abcd1234-0000-1000-8000-00805f9b34fb");

    private final BluetoothAdapter mBtAdapter;
    private final Context context;


    /**
     * Constructs a manger.
     * @param context
     * @throws Exception in case Bluetooth is not supported or enabled.
     */
    public ClockSyncManager(Context context) throws Exception {
        this.context = context;
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        // Phone does not support Bluetooth so let the user know and exit.
        if (mBtAdapter == null) {
            throw new Exception("BT not available on this device");
        }

        if (!mBtAdapter.isEnabled())
            throw new Exception("BT not enabled");
    }

    /**
     * Attempts to determine drift against the first bonded device that supports the correct UUID.
     * Will return the drift or throw an exception.
     * @return drift in ms
     * @throws Exception
     */
    public long getDrift() throws Exception {
        for (BluetoothDevice device : mBtAdapter.getBondedDevices()) {
            Log.i(TAG, "bonded device " + device.getName() + " " + device.getAddress());
            BluetoothSocket socket = tryDevice(device);
            if (socket != null)
                return geDriftFromDevice(socket);
        }
        throw new Exception("no master node found");
    }

    /**
     * Tries to determine the drift against a selected master. Will perform  {@link #MAX_ATTEMPTS}
     * successful connections or timeout after {@link #MAX_ATTEMPT_MILLIS} milliseconds and throw
     * a TimeoutException
     * @return measured minimal drift
     * @throws TimeoutException
     */
    public long getDriftSafe() throws TimeoutException {
        int tries = 0;

        long minDrift = Long.MAX_VALUE;
        long startTime = System.currentTimeMillis();

        while(tries < MAX_ATTEMPTS && (System.currentTimeMillis() - startTime) < MAX_ATTEMPT_MILLIS) {
            try {
                long drift = getDrift();
                if(drift < minDrift)
                    minDrift = drift;
                tries ++;
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        if(minDrift < MAX_ATTEMPTS)
            throw new TimeoutException(String.format("not able to sync %d times in %d millis, succeeded syncs=%d",MAX_ATTEMPTS,MAX_ATTEMPT_MILLIS,tries));

        return minDrift;

    }

    /**
     * Will return the correct a bluetooth socket, by trying to connect against SDP, since a SDP query
     * is not reliable. (WTF?)
     * @param device
     * @return
     * @throws IOException
     */
    private BluetoothSocket tryDevice(BluetoothDevice device) throws IOException {
        BluetoothSocket bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(RECORD_UUID);
        return bluetoothSocket;
    }


    /**
     * Calculates the drift against a BT device.
     * Will return the drift or throw an exception.
     * @param bluetoothSocket BT Socket connected to the correct UUID.
     * @return determiend drift, contains the RTT, but is assumed to be low latency.
     * @throws Exception
     */
    public long geDriftFromDevice(BluetoothSocket bluetoothSocket) throws Exception {

        bluetoothSocket.connect(); // will block until a connection is established or failed
        try {
            if (bluetoothSocket.isConnected()) {
                BufferedReader br = new BufferedReader(new InputStreamReader(bluetoothSocket.getInputStream()));
                String line = br.readLine();
                long remoteTime = Long.parseLong(line);
                long drift = System.currentTimeMillis() - remoteTime;
                Log.e(TAG, "Drift=" + drift);
                br.close();
                bluetoothSocket.close();
                return drift;
            } else throw new Exception("link not created");
        } catch (Exception e) {
            e.printStackTrace();
            bluetoothSocket.close();
        }
        throw new Exception("no drift calculated");
    }


}
