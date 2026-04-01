package com.motionbridge.core.models;

public abstract class MBEvent {
    protected String t; // Type of the event

    public String getType() {
        return t;
    }
}
