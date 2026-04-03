package com.motionbridge.core.models;

public class VolumeEvent extends MBEvent {
    public double v;

    public double getVolume() {
        return v / 100.0; // Convert from 0-100 to 0.0-1.0
    }

    public void setVolume(double v) {
        this.v = v;
    }
}