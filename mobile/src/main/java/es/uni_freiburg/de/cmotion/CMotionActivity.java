package es.uni_freiburg.de.cmotion;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import de.uni_freiburg.es.intentforwarder.IntentForwarderService;
import de.uni_freiburg.es.sensorrecordingtool.Recorder;


public class CMotionActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cmotion);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        /** just make sure that the IntentForwarderService is eventually started */
        Intent i = new Intent(this, IntentForwarderService.class);
        startService(i);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_cmotion, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        startService(new Intent(this, LocalSensorService.class));
        startService(new Intent(this, WearService.class));
        super.onStart();
    }

    @Override
    protected void onPause() {
        stopService(new Intent(this, LocalSensorService.class));
        stopService(new Intent(this, WearService.class));
        super.onPause();
    }
}
