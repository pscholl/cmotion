package es.uni_freiburg.de.cmotion;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import de.uni_freiburg.es.sensorrecordingtool.Recorder;

/**
 * A transparent {@link Activity} displaying a "Stop" options menu to remove the {@link LiveCard}.
 */
public class LiveCardMenuActivity extends Activity {

    private static final String TAG = LiveCardMenuActivity.class.getSimpleName();

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "fufufu "+ getIntent().getIntExtra(Recorder.RECORDING_ID, -1));
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        // Open the options menu right away.
        openOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.cmotion_card, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_stop:
                // Stop the service which will unpublish the live card.
                Intent cancel_intent = new Intent(Recorder.CANCEL_ACTION);
                cancel_intent.putExtra(Recorder.RECORDING_ID,
                        getIntent().getIntExtra(Recorder.RECORDING_ID, -1));
                sendBroadcast(cancel_intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        super.onOptionsMenuClosed(menu);
        // Nothing else to do, finish the Activity.
        finish();
    }
}
