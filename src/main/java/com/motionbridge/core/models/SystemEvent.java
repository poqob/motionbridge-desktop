package com.motionbridge.core.models;

public class SystemEvent extends MBEvent {
    private String action;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
