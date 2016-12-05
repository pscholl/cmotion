package de.uni_freiburg.es.sensorrecordingtool;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.ArrayList;

/**
 * A class for requesting permissions.
 */
public class PermissionHelperActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int PERMISSION_REQUEST = 11;
    private static final String TAG = PermissionHelperActivity.class.getSimpleName();
    private static PermissionRunnable permissionsGrantedRunnable, notGrantedRunnable;


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

        finish(); // Kill our useless activity

        ArrayList<String> grantedList = new ArrayList<>();
        ArrayList<String> notGrantedList = new ArrayList<>();

        for (int i = 0; i < permissions.length; i++) {
            String p = permissions[i];
            int resu = grantResults[i];
            if (resu != PackageManager.PERMISSION_GRANTED) {
                notGrantedList.add(p);
                Log.e(TAG, String.format("Permission %s has not been granted", p));
            } else grantedList.add(p);
        }

        permissionsGrantedRunnable.setResults(grantedList.toArray(new String[grantedList.size()]),
                notGrantedList.toArray(new String[notGrantedList.size()]));
        notGrantedRunnable.setResults(grantedList.toArray(new String[grantedList.size()]),
                notGrantedList.toArray(new String[notGrantedList.size()]));

        (notGrantedList.size() == 0 ? permissionsGrantedRunnable : notGrantedRunnable).run();
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
