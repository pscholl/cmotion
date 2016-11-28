package de.uni_freiburg.es.sensorrecordingtool.autodiscovery;

/**
 * A listener that listens for discovered nodes
 */
public interface OnNodeSensorsDiscoveredListener {

    /**
     * Triggered when a node has been discovered. Once for each node in a discovery cycle.
     * @param nodeName The node name, usually the model
     * @param availableSensors All available Sensor names
     */
    void onNodeSensorsDiscovered(String nodeName, String[] availableSensors);
}