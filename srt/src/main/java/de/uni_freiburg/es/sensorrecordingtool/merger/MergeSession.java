package de.uni_freiburg.es.sensorrecordingtool.merger;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import de.uni_freiburg.es.sensorrecordingtool.FFMpegCopyProcess;
import de.uni_freiburg.es.sensorrecordingtool.RSyncProcess;
import de.uni_freiburg.es.sensorrecordingtool.RecorderStatus;
import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.ConnectionTechnology;
import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.Node;
import de.uni_freiburg.es.sensorrecordingtool.merger.retriever.BTDataRetriever;
import de.uni_freiburg.es.sensorrecordingtool.merger.retriever.DataRetriever;
import de.uni_freiburg.es.sensorrecordingtool.merger.retriever.LocalDataRetriever;
import de.uni_freiburg.es.sensorrecordingtool.merger.retriever.ProgressChangedListener;
import de.uni_freiburg.es.sensorrecordingtool.merger.retriever.TCPRetriever;
import de.uni_freiburg.es.sensorrecordingtool.merger.retriever.WearDataRetriever;

public class MergeSession {

    private static final boolean CLEANUP = false;
    private final ArrayList<RetrieverThread> mThreadPool = new ArrayList<>();
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

//            if (!ACTION_MERGE_CANCEL.equals(intent.getAction()) || !RecorderStatus.ERROR_ACTION.equals(intent.getAction()))
//                return;

            unregisterReceiver(this);

            for (Thread t : mThreadPool)
                t.interrupt();
            mTimeoutHandler.removeCallbacksAndMessages(null); // remove all scheduled runnables
            mMergeStatus.error(new InterruptedException("cancelled by user"));
            mIsFinished = true;
        }
    };

    private MergeStatus mMergeStatus;
    private String outputPath;

    public MergeSession(Context context, String recordingUUID, ArrayList<Node> nodes) {
        this.mContext = context;
        this.mNodeDataCount = nodes.size();
        this.mRecordingUUID = recordingUUID;
        this.mMergeStatus = new MergeStatus(context, recordingUUID, nodes.size());

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RecorderStatus.ERROR_ACTION);
        intentFilter.addAction(ACTION_MERGE_CANCEL);

        registerReceiver(mBroadcastReceiver, intentFilter);

        launchRetrievers(nodes);

    }

    /**
     * Starts RetrieverThreads for every given Node. All threads are cached in {@link #mThreadPool}
     * @param nodes
     */
    private void launchRetrievers(ArrayList<Node> nodes) {
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

    /**
     * Checks whether all Threads in the pool have finished.
     * @return true if one or more threads are still running.
     */
    public boolean isThreadPoolFinished() {
        boolean b = true;
        for (Thread t : mThreadPool)
            if (t.isAlive())
                b = false;
        return b;
    }

    /**
     * Merges all device recordings to one big mkv container using FFMPEG.
     * Deletes all device recordings if {@link #CLEANUP} is set and will RSync if allowed by the user.
     * This method will block until all recordings were merged.
     */
    private void mergeAllRecordings() {
        Log.i(TAG, "merging all node recordings");

        int i = 0;
        ArrayList<String> input = new ArrayList<>();
        for (File file : mFiles)
            if (file != null)
                input.add(file.getAbsolutePath());

        try {
            unregisterReceiver(mBroadcastReceiver);
            String output = getOutputPath() + "/" + mRecordingUUID + ".merged.mkv";

            FFMpegCopyProcess copyProcess = new FFMpegCopyProcess.Builder()
                    .setInput(input.toArray(new String[input.size()]))
                    .setOutput(output)
                    .build(mContext);
            copyProcess.waitFor();

            if (new File(output).exists()) {
                Log.i(TAG, "merged to: " + output);
                mMergeStatus.finished(output);

                rSyncIfNecessary(output);
            } else
                mMergeStatus.error(new FileNotFoundException("file not written"));

            if(CLEANUP) {
                for (File file : mFiles) // cleanup
                    if (file != null && file.exists())
                        file.delete();
            }

            mIsFinished = true;
        } catch (Exception e) {
            mMergeStatus.error(e);
            e.printStackTrace();
        }
    }

    /**
     * Will check whether rSync is activated and kick off the process if it is.
     * @param file
     */
    private void rSyncIfNecessary(String file) {
        if(!isRSync())
            return;

        try {
            new RSyncProcess.Builder()
                    .setInput(file)
                    .setOutput(getRSyncOutputPath())
                    .showProgress()
                    .showStats()
                    .build(mContext);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getOutputPath() {
        return PreferenceManager.getDefaultSharedPreferences(mContext).getString("output_directory",
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString());

    }

    private boolean isRSync() {
        return PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("rsync", false);

    }

    private String getRSyncOutputPath() {
        return PreferenceManager.getDefaultSharedPreferences(mContext).getString("rsync_out",
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString());

    }


    class RetrieverThread extends Thread {

        private DataRetriever retriever;
        private Node node;

        public DataRetriever getRetriever() {
            return retriever;
        }

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

            this.retriever.setProgressChangedListener(new ProgressChangedListener() {
                @Override
                public void progressChanged(DataRetriever retriever) {
                    System.out.println(retriever+" -> "+retriever.getProgress());
                    mMergeStatus.setProgress(calculateTotalProgress());
                }
            });

            File file = null;
            try {
                file = retriever.getFile();
                Log.i(TAG, node.toString()+" provided "+file.toString());
                mFiles.add(file);
//                mMergeStatus.incrementProgress();
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

    /**
     * Calculates the total progress of all retrievers.
     * @return a number between 0 to 1, representing a percentage
     */
    private float calculateTotalProgress() {
        float f = 0;

        for(RetrieverThread thread : mThreadPool) {
            f += thread.getRetriever().getProgress();
        }

        return f / mThreadPool.size();
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
