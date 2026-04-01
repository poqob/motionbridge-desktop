package com.motionbridge.core.models;

public class DiscoveryAck {
    private String type;

    public DiscoveryAck(String hostName, int dataPort, int wsPort) {
        this.type = "discovery_ack";
    }

    public String getType() {
        return type;
    }
}
