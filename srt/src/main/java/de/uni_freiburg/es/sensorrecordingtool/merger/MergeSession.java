package de.uni_freiburg.es.sensorrecordingtool.merger;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import de.uni_freiburg.es.sensorrecordingtool.FFMpegCopyProcess;
import de.uni_freiburg.es.sensorrecordingtool.RecorderStatus;
import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.ConnectionTechnology;
import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.Node;
import de.uni_freiburg.es.sensorrecordingtool.merger.retriever.BTDataRetriever;
import de.uni_freiburg.es.sensorrecordingtool.merger.retriever.DataRetriever;
import de.uni_freiburg.es.sensorrecordingtool.merger.retriever.LocalDataRetriever;
import de.uni_freiburg.es.sensorrecordingtool.merger.retriever.TCPRetriever;
import de.uni_freiburg.es.sensorrecordingtool.merger.retriever.WearDataRetriever;

public class MergeSession {

    private final ArrayList<Thread> mThreadPool = new ArrayList<>();
    private String mRecordingUUID;
    private ArrayList<File> mFiles = new ArrayList<>();
    private Context mContext;
    private final String TAG = MergeSession.class.getSimpleName();
    private int mNodeDataCount = 0;
    private Handler mTimeoutHandler = new Handler();
    public static final long TIMEOUT_AFTER_LAST_FILE_MS = 120 * 1000;
    private boolean mIsFinished = false;
    public static final String ACTION_MERGE_CANCEL = "merge_cancel";
    private boolean isRegistered = false;

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (!ACTION_MERGE_CANCEL.equals(intent.getAction()) || !RecorderStatus.ERROR_ACTION.equals(intent.getAction()))
                return;

            unregisterReceiver(this);

            for (Thread t : mThreadPool)
                t.interrupt();
            mTimeoutHandler.removeCallbacksAndMessages(null); // remove all scheduled runnables
            mMergeStatus.error(new InterruptedException("cancelled by user"));
            mIsFinished = true;
        }
    };

    private MergeStatus mMergeStatus;

    public MergeSession(Context context, String recordingUUID, ArrayList<Node> nodes) {
        this.mContext = context;
        this.mNodeDataCount = nodes.size();
        this.mRecordingUUID = recordingUUID;
        this.mMergeStatus = new MergeStatus(context, recordingUUID, nodes.size());

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RecorderStatus.ERROR_ACTION);
        intentFilter.addAction(ACTION_MERGE_CANCEL);

        registerReceiver(mBroadcastReceiver, intentFilter);

        for (Node node : nodes) {
            final String aid = node.getAid();
            Log.e(TAG, "setting up retriever for " + aid);
            RetrieverThread thread = new RetrieverThread(node);
            mThreadPool.add(thread);
            thread.start();
        }

    }

    private void registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        mContext.getApplicationContext().registerReceiver(mBroadcastReceiver, filter);
        isRegistered = true;
    }

    private void unregisterReceiver(BroadcastReceiver receiver) {
        if (isRegistered)
            try {
                mContext.getApplicationContext().unregisterReceiver(mBroadcastReceiver);
            } catch (Exception e) {
            }
        isRegistered = false;
    }

    public boolean isFinished() {
        return mIsFinished;
    }

    public boolean isThreadPoolFinished() {
        boolean b = true;
        for (Thread t : mThreadPool)
            if (t.isAlive())
                b = false;
        return b;
    }

    private void mergeAllRecordings() {
        Log.i(TAG, "merging all node recordings");

        int i = 0;
        ArrayList<String> input = new ArrayList<>();
        for (File file : mFiles)
            if (file != null)
                input.add(file.getAbsolutePath());

        try {
            unregisterReceiver(mBroadcastReceiver);
            String output = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                    .getAbsolutePath() + "/" + mRecordingUUID + ".merged.mkv";

            FFMpegCopyProcess copyProcess = new FFMpegCopyProcess.Builder()
                    .setInput(input.toArray(new String[input.size()]))
                    .setOutput(output)
                    .build(mContext);
            copyProcess.waitFor();

            if(new File(output).exists()) {
                Log.i(TAG, "merged to: " + output);
                mMergeStatus.finished(output);
            } else
                mMergeStatus.error(new FileNotFoundException("file not written"));

            for (File file : mFiles) // cleanup
                if (file != null && file.exists())
                    file.delete();

            mIsFinished = true;
        } catch (Exception e) {
            mMergeStatus.error(e);
            e.printStackTrace();
        }
    }


    class RetrieverThread extends Thread {

        private DataRetriever retriever;
        private Node node;

        public RetrieverThread(Node node) {
            super(RetrieverThread.class.getSimpleName());
            this.node = node;
        }

        /**
         * Builds a compatible Retriever for a given Node.
         *
         * @param node
         * @return
         */
        private DataRetriever pickRetriever(Node node) {

            ArrayList<ConnectionTechnology.Type> list = new ArrayList<>();
            for (ConnectionTechnology tech : node.getConnectionTechnologies())
                list.add(tech.getType());

            if (list.contains(ConnectionTechnology.Type.LOCAL))
                return new LocalDataRetriever(mContext, node, mRecordingUUID);
            else if (list.contains(ConnectionTechnology.Type.WEAR))
                return new WearDataRetriever(mContext, node, mRecordingUUID);
            else if (list.contains(ConnectionTechnology.Type.TCP_OVER_WIFI))
                return new TCPRetriever(mContext, node, mRecordingUUID);
            else if (list.contains(ConnectionTechnology.Type.BT_CLASSIC))
                return new BTDataRetriever(mContext, node, mRecordingUUID);

            else return null; // TODO
        }

        @Override
        public void run() {
            this.retriever = pickRetriever(node);
            File file = null;
            try {
                file = retriever.getFile();
                Log.i(TAG, node.toString()+" provided "+file.toString());
                mFiles.add(file);
                mMergeStatus.incrementProgress();
                mTimeoutHandler.removeCallbacksAndMessages(null); // remove all scheduled runanbles
                mNodeDataCount--;
                if (!isInterrupted()) {
                    if (mNodeDataCount == 0) {
                        mergeAllRecordings();
                    } else
                        startTimeoutTimer();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            retriever.destroy();
        }

    }

    private void startTimeoutTimer() {
        mTimeoutHandler.removeCallbacksAndMessages(null);
        mTimeoutHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.w(TAG, "MergeSession timeout, doing early merge");
                for (Thread t : mThreadPool)
                    t.interrupt();
                mergeAllRecordings();
            }
        }, TIMEOUT_AFTER_LAST_FILE_MS);
    }

}
