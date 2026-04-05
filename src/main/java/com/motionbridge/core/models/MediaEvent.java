package com.motionbridge.core.models;

public class MediaEvent extends MBEvent {
    private String action; // PLAY, PAUSE, NEXT, PREV

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
