package de.uni_freiburg.es.cmotiontheta;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;

import java.util.LinkedList;
import java.util.Queue;


/**
 * Created by phil on 9/29/16.
 */
public class AskForPermission extends Activity {
    private Queue<Intent> mIntentQ = new LinkedList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.askforpermission);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        if ( ActivityCompat.shouldShowRequestPermissionRationale(this,
             Manifest.permission.ACCESS_COARSE_LOCATION))
            ;

        ActivityCompat.requestPermissions(this,
            new String[] {Manifest.permission.ACCESS_COARSE_LOCATION},0);
        mIntentQ.add(getIntent());

        super.onStart();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        /** if permission has been granted, forward all intent in the current queue */
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            for (Intent i : mIntentQ) {
                i.setClass(this, ThetaService.class);
                startService(i);
            }

            mIntentQ.clear();
        }

        finish();
    }
}
