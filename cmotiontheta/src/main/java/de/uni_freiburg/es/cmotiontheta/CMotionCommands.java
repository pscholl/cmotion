package de.uni_freiburg.es.cmotiontheta;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Build;

/**
 * Created by phil on 9/21/16.
 */
public class CMotionCommands extends BroadcastReceiver{
    private static final String TAG = CMotionCommands.class.getSimpleName();
    private static Intent mCMotionIntent;
    private WifiManager mWifi;

    @Override
    public void onReceive(Context context, Intent intent) {
        mWifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        if (intent == null)
            return;

        /** check permission crap */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
            intent.setClass(context, AskForPermission.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } else { /** forward intent to the service */
            intent.setClass(context, ThetaService.class);
            context.startService(intent);
        }
    }
}
