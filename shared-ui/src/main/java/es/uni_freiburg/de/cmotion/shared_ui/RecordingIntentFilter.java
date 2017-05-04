package es.uni_freiburg.de.cmotion.shared_ui;
import de.uni_freiburg.es.sensorrecordingtool.RecorderStatus;
import de.uni_freiburg.es.sensorrecordingtool.merger.MergeStatus;

public class RecordingIntentFilter extends android.content.IntentFilter {

    public RecordingIntentFilter() {
        addAction(RecorderStatus.STATUS_ACTION);
        addAction(RecorderStatus.FINISH_ACTION);
        addAction(MergeStatus.FINISH_ACTION);
        addAction(RecorderStatus.ERROR_ACTION);
    }
}

