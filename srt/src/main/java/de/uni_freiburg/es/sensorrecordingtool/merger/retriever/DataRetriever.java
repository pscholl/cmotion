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

    public DataRetriever(Context context, Node node, String recordingUUID) {
        mContext = context;
        mNode = node;
        mRecordingUUID = recordingUUID;
        fileName = node.getAid() + "_" + mRecordingUUID;
    }

    public abstract void destroy();

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
