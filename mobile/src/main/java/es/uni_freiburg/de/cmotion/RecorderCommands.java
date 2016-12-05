package es.uni_freiburg.de.cmotion;

import android.content.Context;
import android.content.Intent;

import de.uni_freiburg.es.sensorrecordingtool.Recorder;

/** A Broadcast receiver that asks for permission when starting a recording on MarshMallow.
 *
 * Created by phil on 2/29/16.
 */
public class RecorderCommands extends android.content.BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null)
            return;

//        if (PermissionDialog.needToAskForPermission(context)) {
//            intent.setClass(context, PermissionDialog.class);
//            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            context.startActivity(intent);
//        }

        if (Recorder.SHOWUI_ACTION.equals(intent.getAction()))
        {
            intent.setClass(context, CMotionActivity.class);
            context.startActivity(intent);
        }
    }
}
