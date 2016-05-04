package es.uni_freiburg.de.cmotion;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import de.uni_freiburg.es.intentforwarder.IntentForwarder;
import de.uni_freiburg.es.sensorrecordingtool.Recorder;

/** Simply start a new recording with the default settings.
 *
 * Created by phil on 5/3/16.
 */
public class CMotionStartRecording extends Service {

    private static final String TAG = CMotionStartRecording.class.getSimpleName();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent start = new Intent(Recorder.RECORD_ACTION);
        start.putExtra("-i", "acc gyr mag video".split(" "));
        start.putExtra("-r", new int[] {50,50,50,15});
        start.putExtra("-d", 20*60.);
        sendBroadcast(start);

        return super.onStartCommand(intent, flags, startId);
    }
}
