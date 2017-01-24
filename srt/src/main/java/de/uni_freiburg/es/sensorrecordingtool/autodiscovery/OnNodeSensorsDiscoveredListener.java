package de.uni_freiburg.es.sensorrecordingtool.autodiscovery;

/**
 * A listener that listens for discovered nodes
 */
public interface OnNodeSensorsDiscoveredListener {

    /**
     * Triggered when a node has been discovered. Once for each node in a discovery cycle.
     * @param node The node
     * @param availableSensors All available Sensor names
     */
    void onNodeSensorsDiscovered(Node node, String[] availableSensors);
}