package de.uni_freiburg.es.sensorrecordingtool.merger.retriever;

import android.content.Context;
import android.os.Environment;

import java.io.File;

import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.Node;

public abstract class DataRetriever {

    protected final String mRecordingUUID;
    protected final Node mNode;
    protected Context mContext;
    private String fileName;
    private ProgressChangedListener mProgressChangedListener;

    private float mProgress = 0f;

    public DataRetriever(Context context, Node node, String recordingUUID) {
        mContext = context;
        mNode = node;
        mRecordingUUID = recordingUUID;
        fileName = node.getAid() + "_" + mRecordingUUID;
    }


    public ProgressChangedListener getProgressChangedListener() {
        return mProgressChangedListener;
    }

    public void setProgressChangedListener(ProgressChangedListener progressChangedListener) {
        this.mProgressChangedListener = progressChangedListener;
    }

    public abstract void destroy();

    public float getProgress() {
        return mProgress;
    }

    public void setProgress(float progress) {
        assert progress >= 0 && progress <= 1;
        this.mProgress = progress;
        if(mProgressChangedListener != null)
            mProgressChangedListener.progressChanged(this);
    }

    public abstract File getFile() throws InterruptedException;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    File getDestinationFile(int offset) {
        return getDestinationFile(offset, true);
    }

    File getDestinationFile() {
        return getDestinationFile(-1, false);
    }

    File getDestinationFile(int offset, boolean isPart) {
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                .getAbsolutePath() + "/" + (isPart ? mNode.getAid() + "_" + mRecordingUUID : fileName) + (isPart ? ".part" + offset : ""));
    }
}
