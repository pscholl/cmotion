package es.uni_freiburg.de.cmotion;

import android.content.Context;
import android.content.Intent;

import es.uni_freiburg.de.cmotion.ui.PermissionDialog;

/** A Broadcast receiver that asks for permission when starting a recording on MarshMallow.
 *
 * Created by phil on 2/29/16.
 */
public class RecorderCommands extends android.content.BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null)
            return;

        if (PermissionDialog.needToAskForPermission(context)) {
            intent.setClass(context, PermissionDialog.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
}
