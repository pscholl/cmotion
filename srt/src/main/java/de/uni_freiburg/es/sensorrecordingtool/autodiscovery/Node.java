package de.uni_freiburg.es.sensorrecordingtool.autodiscovery;

import java.util.Arrays;

public class Node implements Comparable<Node> {
    private String aid, platform;
    private NodeStatus nodeStatus = NodeStatus.UNKNOWN;
    private String[] readySensors = new String[0];
    private String[] availableSensors = new String[0];
    private ConnectionTechnology[] connectionTechnologies = new ConnectionTechnology[0];
    private long drift = Long.MIN_VALUE;

    public Node(String platform, String aid) {
        this.platform = platform;
        this.aid = aid;
    }

    public String[] getReadySensors() {
        return readySensors;
    }

    public void setReadySensors(String[] readySensors) {
        this.readySensors = readySensors;
    }

    public NodeStatus getNodeStatus() {
        return nodeStatus;
    }

    public void setNodeStatus(NodeStatus nodeStatus) {
        this.nodeStatus = nodeStatus;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getAid() {
        return aid;
    }

    public void setAid(String aid) {
        this.aid = aid;
    }

    @Override
    public int compareTo(Node another) {
        return getAid().compareTo(another.getAid());
    }

    @Override
    public String toString() {
        return String.format("%s[%s] status=%s drift=%sms readySense=%s", platform, aid, nodeStatus.toString(), drift, Arrays.toString(readySensors));
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof Node))
            return false;
        else return ((Node) o).getAid().equals(getAid());
    }

    public long getDrift() {
        return drift;
    }

    public void setDrift(long drift) {
        this.drift = drift;
    }

    public String[] getAvailableSensors() {
        return availableSensors;
    }

    public void setAvailableSensors(String[] availableSensors) {
        this.availableSensors = availableSensors;
    }

    public ConnectionTechnology[] getConnectionTechnologies() {
        return connectionTechnologies;
    }

    public void setConnectionTechnologies(ConnectionTechnology[] connectionTechnologies) {
        this.connectionTechnologies = connectionTechnologies;
    }
}
