package es.uni_freiburg.de.cmotion;
import de.uni_freiburg.es.sensorrecordingtool.RecorderStatus;

class StatusIntentFilter extends android.content.IntentFilter {

    StatusIntentFilter() {
        addAction(RecorderStatus.STATUS_ACTION);
    }
}

