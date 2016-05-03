package es.uni_freiburg.de.cmotion;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/** Just forward the rx'ed intent to the CMotionCardService.
 *
 * Created by phil on 5/3/16.
 */
public class CMotionReceiver extends BroadcastReceiver {
    private static final String TAG = CMotionReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent==null)
            return;

        intent.setClass(context, CMotionCardService.class);
        context.startService(intent);
    }
}
