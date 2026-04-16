package com.motionbridge.core.models;

public class AMMoveEvent extends MBEvent {
    private double x;
    private double y;
    private double z;

    public AMMoveEvent() {
        this.t = "AM_M";
    }

    public AMMoveEvent(double x, double y, double z) {
        this.t = "AM_M";
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }
}
