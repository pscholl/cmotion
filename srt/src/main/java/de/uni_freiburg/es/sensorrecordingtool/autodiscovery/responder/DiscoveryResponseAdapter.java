package de.uni_freiburg.es.sensorrecordingtool.autodiscovery.responder;


import android.content.Context;

/**
 * Abstract class for Classes that will search for particular nodes and respond with a result intent.
 */
public abstract class DiscoveryResponseAdapter {
    final Context context;

    DiscoveryResponseAdapter(Context context) {
        this.context = context;
    }

    /**
     * Called after reeciving a discovery Intent
     */
    abstract void discover();
}
