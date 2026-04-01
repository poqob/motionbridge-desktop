package com.motionbridge.core.models;

public class ScrollEvent extends MBEvent {
    private double x;
    private double y;

    public ScrollEvent() {
        this.t = "S";
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
}
