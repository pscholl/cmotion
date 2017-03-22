package de.unifreiburg.es.btclocksync;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * A manager class to determine offset against bonded Bluetooth Classic nodes. It is assumed that the
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
     *
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
     * Attempts to determine offset against the first bonded device that supports the correct UUID.
     * Will return the drift or throw an exception.
     *
     * @return offset in ms
     * @throws Exception
     */
    public long getOffset() throws Exception {
        for (BluetoothDevice device : mBtAdapter.getBondedDevices()) {
            Log.i(TAG, "bonded device " + device.getName() + " " + device.getAddress());
            BluetoothSocket socket = tryDevice(device);
            if (socket != null)
                return geDriftFromDevice(socket);
        }
        throw new Exception("no master node found");
    }

    /**
     * Tries to determine the offset against a selected master. Will perform  {@link #MAX_ATTEMPTS}
     * successful connections or timeout after {@link #MAX_ATTEMPT_MILLIS} milliseconds and throw
     * a TimeoutException
     *
     * @return measured minimal offset
     * @throws TimeoutException
     * @throws InterruptedException
     */
    public long getDriftSafe() throws TimeoutException, InterruptedException {
        int tries = 0;

        long minOffset = Long.MAX_VALUE;
        long startTime = System.currentTimeMillis();

        while (tries < MAX_ATTEMPTS && (System.currentTimeMillis() - startTime) < MAX_ATTEMPT_MILLIS) {
            if(Thread.currentThread().isInterrupted())
                throw new InterruptedException();
            try {
                long offset = getOffset();
                if (Math.abs(offset) < Math.abs(minOffset))
                    minOffset = offset;
                tries++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (tries != MAX_ATTEMPTS) // we didnt make enough attempts in time
            throw new TimeoutException(String.format("not able to sync %d times in %d millis, succeeded syncs=%d", MAX_ATTEMPTS, MAX_ATTEMPT_MILLIS, tries));

        return minOffset;
    }

    /**
     * Will return the correct bluetooth socket, by trying to connect against SDP, since a SDP query
     * is not reliable. (WTF?)
     *
     * @param device
     * @return
     * @throws IOException
     */
    private BluetoothSocket tryDevice(BluetoothDevice device) throws IOException {
        BluetoothSocket bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(RECORD_UUID);
        return bluetoothSocket;
    }


    /**
     * Calculates the offset against a BT device.
     * Will return the offset or throw an exception.
     *
     * @param bluetoothSocket BT Socket connected to the correct UUID.
     * @return determined offset, contains the link latency, but is assumed to be low latency.
     * @throws Exception
     */
    public long geDriftFromDevice(BluetoothSocket bluetoothSocket) throws Exception {

        bluetoothSocket.connect(); // will block until a connection is established or failed
        BufferedReader br = null;
        BufferedWriter bw = null;
        try {
            if (bluetoothSocket.isConnected()) {

                bw = new BufferedWriter(new OutputStreamWriter(bluetoothSocket.getOutputStream()));
                bw.write("\n");
                bw.write(System.currentTimeMillis()+"\n");
                bw.flush();

                br = new BufferedReader(new InputStreamReader(bluetoothSocket.getInputStream()));
                br.readLine();
                String line = br.readLine();
                long remoteTime = Long.parseLong(line);
                long offset = System.currentTimeMillis() - remoteTime;
                Log.e(TAG, "Offset=" + offset);
                return offset;
            } else throw new Exception("link not created");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (br != null) br.close();
            if (bw != null) bw.close();
            if (bluetoothSocket != null)
                bluetoothSocket.close();
        }
        throw new Exception("no offset calculated");
    }


}
