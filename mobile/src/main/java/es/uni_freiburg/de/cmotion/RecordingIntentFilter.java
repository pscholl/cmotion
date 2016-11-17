package es.uni_freiburg.de.cmotion;
import de.uni_freiburg.es.sensorrecordingtool.RecorderStatus;
import de.uni_freiburg.es.sensorrecordingtool.Recorder;

public class RecordingIntentFilter extends android.content.IntentFilter {
    public RecordingIntentFilter() {
        addAction(Recorder.RECORD_ACTION);
        addAction(Recorder.CANCEL_ACTION);
        addAction(RecorderStatus.FINISH_ACTION);
        addAction(RecorderStatus.ERROR_ACTION);
    }
}

