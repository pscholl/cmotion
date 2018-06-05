package de.senseable.wear_autorecord;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import de.uni_freiburg.es.sensorrecordingtool.RecorderStatus;
import de.uni_freiburg.es.sensorrecordingtool.autodiscovery.NodeStatus;

public class MainActivity extends WearableActivity {

    private Button mButton;
    private View.OnClickListener mStartRecording = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            startActivity(MainReceiver.mStartIntent);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButton = (Button) findViewById(R.id.button);

        // Enables Always-on
        setAmbientEnabled();

        mButton.setOnClickListener(mStartRecording);
    }
}
