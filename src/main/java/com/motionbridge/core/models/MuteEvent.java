package com.motionbridge.core.models;

public class MuteEvent extends MBEvent {
    public boolean v;

    public boolean isMuted() {
        return v;
    }

    public void setMuted(boolean v) {
        this.v = v;
    }
}