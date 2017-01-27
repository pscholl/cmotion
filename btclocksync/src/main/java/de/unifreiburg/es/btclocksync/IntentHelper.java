package de.unifreiburg.es.btclocksync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.concurrent.CountDownLatch;


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

    public Intent waitForBroadcast(Context context, String action) throws InterruptedException {
        latch = new CountDownLatch(1);
        context.registerReceiver(mReceiver, new IntentFilter(action));
        latch.await();
        context.unregisterReceiver(mReceiver);
        return result;
    }
}
