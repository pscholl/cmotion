package de.uni_freiburg.es.sensorrecordingtool.merger.retriever;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import de.uni_freiburg.es.sensorrecordingtool.RecorderStatus;
import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.Node;


public class LocalDataRetriever extends DataRetriever {

    private CountDownLatch latch = new CountDownLatch(1);
    private File mFile = null;
    private boolean mReceiverRegistered = false;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent == null || !intent.getStringExtra(RecorderStatus.RECORDING_UUID).equals(mRecordingUUID))
                return;
            String path = intent.getStringExtra(RecorderStatus.FINISH_PATH);
            if(path == null)
                return;
            mFile = new File(path);
            mReceiverRegistered = false;
            context.unregisterReceiver(this);
            latch.countDown();
        }
    };


    public LocalDataRetriever(Context context, Node node, String recordingUUID) {
        super(context, node, recordingUUID);

        IntentFilter filter = new IntentFilter(RecorderStatus.FINISH_ACTION);
        context.registerReceiver(mReceiver, filter);
        mReceiverRegistered = true;
    }

    @Override
    public void destroy() {
        if(mReceiverRegistered && mReceiver != null)
            super.mContext.unregisterReceiver(mReceiver);
    }

    @Override
    public File getFile() {
        try {
            latch.await();
            return mFile;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }
}
