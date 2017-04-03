package de.uni_freiburg.es.sensorrecordingtool.merger;

public class MergeConst {

    public static final String KEY_DATA = "data";
    public static final String KEY_OFFSET = "offset";
    public static final String KEY_TOTAL = "total";

    public static final int CHUNK_SIZE = 50000;

    public static String buildWearPath(String nodeId, String recordingUUID) {
        return "/" + nodeId + "/" + recordingUUID;
    }

}
