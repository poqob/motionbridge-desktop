package com.motionbridge.core.models;

public class AMModeEvent extends MBEvent {
    private boolean enabled;

    public AMModeEvent() {
        this.t = "AM_MODE";
    }

    public AMModeEvent(boolean enabled) {
        this.t = "AM_MODE";
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
