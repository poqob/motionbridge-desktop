package com.motionbridge.core.models;

public class DimmerEvent extends MBEvent {
    private double v;

    public DimmerEvent() {
        this.t = "D";
    }

    public double getValue() {
        return v;
    }
}
