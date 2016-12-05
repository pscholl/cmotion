package es.uni_freiburg.de.cmotion;
import de.uni_freiburg.es.sensorrecordingtool.RecorderStatus;

class RecordingIntentFilter extends android.content.IntentFilter {

    RecordingIntentFilter() {
        addAction(RecorderStatus.STATUS_ACTION);
        addAction(RecorderStatus.FINISH_ACTION);
        addAction(RecorderStatus.ERROR_ACTION);
    }
}

