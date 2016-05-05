package es.uni_freiburg.de.cmotion;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.view.WindowUtils;

import de.uni_freiburg.es.sensorrecordingtool.Recorder;

/**
 * A transparent {@link Activity} displaying a "Stop" options menu to remove the {@link LiveCard}.
 */
public class CMotionCardMenuActivity extends Activity {

    private static final String TAG = CMotionCardMenuActivity.class.getSimpleName();
    private boolean mFromLiveCardVoice = false;
    private boolean mWasOpened = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);

        mFromLiveCardVoice = getIntent().getBooleanExtra(LiveCard.EXTRA_FROM_LIVECARD_VOICE, false);
        if (mFromLiveCardVoice) {
            // When activated by voice from a live card, enable voice commands. The menu
            // will automatically "jump" ahead to the items (skipping the guard phrase
            // that was already said at the live card).
            getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);
        }
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        if (isMyMenu(featureId)) {
            getMenuInflater().inflate(R.menu.cmotion_card, menu);
            return true;
        }
        return super.onCreatePanelMenu(featureId, menu);
    }

    @Override
    public boolean onPreparePanel(int featureId, View view, Menu menu) {
        // do not reopen the voice menu
        if (isMyMenu(featureId))
            return !mWasOpened;
        return super.onPreparePanel(featureId, view, menu);
    }

    @Override
    public void onPanelClosed(int featureId, Menu menu) {
        if (isMyMenu(featureId))
            mWasOpened = true;
        super.onPanelClosed(featureId, menu);

        finish();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!mFromLiveCardVoice)
            openOptionsMenu();
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        Log.d(TAG, "selected " + item.getItemId());
        switch (item.getItemId()) {
            case R.id.action_stop:
                // Stop the service which will unpublish the live card.
                Intent cancel_intent = new Intent(Recorder.CANCEL_ACTION);
                cancel_intent.putExtra(Recorder.RECORDING_ID,
                        getIntent().getIntExtra(Recorder.RECORDING_ID, -1));
                sendBroadcast(cancel_intent);
                return true;
            default:
                return super.onMenuItemSelected(featureId, item);
        }
    }

    /**
     * Returns {@code true} when the {@code featureId} belongs to the options menu or voice
     * menu that are controlled by this menu activity.
     */
    private boolean isMyMenu(int featureId) {
        return featureId == Window.FEATURE_OPTIONS_PANEL ||
                featureId == WindowUtils.FEATURE_VOICE_COMMANDS;
    }
}
