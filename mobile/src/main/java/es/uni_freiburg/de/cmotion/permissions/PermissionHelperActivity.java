package es.uni_freiburg.de.cmotion.permissions;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.util.ArrayList;

/**
 * A class for requesting permissions.
 */
public class PermissionHelperActivity extends Activity {

    private static final int PERMISSION_REQUEST = 11; // request code must be in the range of 0.. 65535
    private static final String TAG = PermissionHelperActivity.class.getSimpleName();
    private static PermissionRunnable permissionsGrantedRunnable, notGrantedRunnable;

    ArrayList<String> grantedList = new ArrayList<>();
    ArrayList<String> notGrantedList = new ArrayList<>();

    /**
     * Request permissions, will launch a new Activity (and Task) if one or more Permissions are not granted.
     *
     * @param context
     * @param permissionsGrantedRunnable Runnable to be executed after all permissions are granted
     * @param notGrantedRunnable         Runnable to be executed in case some permissions are not granted
     */
    public void runWithPermissions(Context context, PermissionRunnable permissionsGrantedRunnable, PermissionRunnable notGrantedRunnable) {
        this.permissionsGrantedRunnable = permissionsGrantedRunnable;
        this.notGrantedRunnable = notGrantedRunnable;

        if (needToAskForPermission(context)) { // some permissions not granted
            Intent intent = new Intent(context, PermissionHelperActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } else
            permissionsGrantedRunnable.run(); // already granted no need to start activity
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CAMERA},
                PERMISSION_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != PERMISSION_REQUEST)
            return;

        for (int i = 0; i < permissions.length; i++) {
            String p = permissions[i];
            int resu = grantResults[i];
            if (resu != PackageManager.PERMISSION_GRANTED) {
                notGrantedList.add(p);
                Log.e(TAG, String.format("Permission %s has not been granted", p));
            } else grantedList.add(p);
        }

        if(!overlay(this))
            askForOverlay(this);
        else end();

    }

    @Override
    public void onBackPressed() {
        // do nothing
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,  Intent data) {
        /** check if received result code
         is equal our requested code for draw permission  */
        if (requestCode == PERMISSION_REQUEST) {
            (!overlay(this) ? notGrantedList : grantedList).add("android.permission.SYSTEM_ALERT_WINDOW");
            end();
        }
    }

    private void end() {
        finish(); // Kill our useless activity
        permissionsGrantedRunnable.setResults(grantedList.toArray(new String[grantedList.size()]),
                notGrantedList.toArray(new String[notGrantedList.size()]));
        notGrantedRunnable.setResults(grantedList.toArray(new String[grantedList.size()]),
                notGrantedList.toArray(new String[notGrantedList.size()]));
        (notGrantedList.size() == 0 ? permissionsGrantedRunnable : notGrantedRunnable).run();
    }

    private void askForOverlay(Context c) {
        /** if not construct intent to request permission */
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        /** request permission via start activity for result */
        startActivityForResult(intent, PERMISSION_REQUEST);
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

    /**
     * Systemoverlay permissions are auto granted on SDK < 23 but auto denied on higher SDKs.
     *
     * @param c
     * @return
     */
    public static boolean overlay(Context c) {
        if (Build.VERSION.SDK_INT < 23)
            return true;
        return overlaySDK23(c);
    }

    @android.support.annotation.RequiresApi(api = Build.VERSION_CODES.M)
    private static boolean overlaySDK23(Context c) {
        return Settings.canDrawOverlays(c);
    }


    public static boolean needToAskForPermission(Context context) {
        return !externalStorage(context) || !location(context) || !camera(context) || !audio(context) || !overlay(context);
    }

}
