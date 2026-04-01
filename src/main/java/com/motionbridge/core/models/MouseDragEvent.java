package com.motionbridge.core.models;

public class MouseDragEvent extends MBEvent {
    private double x;
    private double y;

    public MouseDragEvent() {
        this.t = "DRAG";
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
}
