package es.uni_freiburg.de.cmotion.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import de.uni_freiburg.es.intentforwarder.ForwardedUtils;

/** Just ask for the permission and restart the Recorder, now with hopefully
 * enabled permissions.
 *
 * Created by phil on 2/28/16.
 */
public class PermissionDialog extends Activity
    implements  ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int PERMISSION_REQUEST = 11;

    @Override
    protected void onResume() {
        super.onResume();
        Intent start = getIntent();

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CAMERA},
                PERMISSION_REQUEST);
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, String[] perms, int[] grantResults) {
        if (requestCode != PERMISSION_REQUEST)
            return;

        finish();

        /*
         * Location is optional, only check if we can write.
         */
        for (int i=0; i<perms.length; i++) {
            String p = perms[i];
            int resu = grantResults[i];

            if (p.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) &&
                resu == PackageManager.PERMISSION_GRANTED) {

                Intent ii = new Intent(ForwardedUtils.RECORD_ACTION);
                if (getIntent().getExtras() != null)
                    ii.putExtras(getIntent().getExtras());
                sendBroadcast(ii);
            }
        }
    }

    public static boolean externalStorage(Context c) {
        return ContextCompat.checkSelfPermission(c, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean location(Context c) {
        return ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean camera(Context c) {
        return ContextCompat.checkSelfPermission(c, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean audio(Context c) {
        return ContextCompat.checkSelfPermission(c, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean needToAskForPermission(Context context) {
        return !externalStorage(context) || !location(context) || !camera(context) || !audio(context);
    }
}
