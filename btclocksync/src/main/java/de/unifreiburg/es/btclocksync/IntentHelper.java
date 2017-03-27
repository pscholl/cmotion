package de.unifreiburg.es.btclocksync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * A class to decrease complexity, by implementing synchronous methods for asynchronous intents.
 */
public class IntentHelper {

    public Intent result;
    private CountDownLatch latch;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            result = intent;
            latch.countDown();
        }
    };

    /**
     * Will return after the Broadcast arrived or timeout.
     *
     * @param context   Context registering the {@link BroadcastReceiver}
     * @param action    Broadcast action to listen for
     * @param timeoutMs timeout in milliseconds or -1 for forever
     * @return the received {@Intent}
     * @throws InterruptedException Intent hasn't arrived in time.
     */
    public Intent waitForBroadcast(Context context, String action, long timeoutMs) throws InterruptedException {
        latch = new CountDownLatch(1);
        context.registerReceiver(mReceiver, new IntentFilter(action));
        if (timeoutMs != -1)
            latch.await(timeoutMs, TimeUnit.MILLISECONDS);
        else
            latch.await();
        context.unregisterReceiver(mReceiver);
        return result;
    }

    /**
     * Will return after the Broadcast arrived. Will wait forever.
     *
     * @param context Context registering the {@link BroadcastReceiver}
     * @param action  Broadcast action to listen for
     * @return the received {@Intent}
     * @throws InterruptedException
     */
    public Intent waitForBroadcast(Context context, String action) throws InterruptedException {
        return waitForBroadcast(context, action, -1);
    }

}
