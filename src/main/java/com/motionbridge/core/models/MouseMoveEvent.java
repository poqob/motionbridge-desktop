package com.motionbridge.core.models;

public class MouseMoveEvent extends MBEvent {
    private double x;
    private double y;

    public MouseMoveEvent() {
        this.t = "M";
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
}
