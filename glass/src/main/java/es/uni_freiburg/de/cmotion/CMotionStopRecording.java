package es.uni_freiburg.de.cmotion;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import de.uni_freiburg.es.sensorrecordingtool.Recorder;

/** Simply start a new recording with the default settings.
 *
 * Created by phil on 5/3/16.
 */
public class CMotionStopRecording extends Service {

    private static final String TAG = CMotionStopRecording.class.getSimpleName();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent start = new Intent(Recorder.CANCEL_ACTION);
        sendBroadcast(start);

        return super.onStartCommand(intent, flags, startId);
    }
}
