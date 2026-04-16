package com.motionbridge.core.models;

public class AMSensEvent extends MBEvent {
    private double value;

    public AMSensEvent() {
        this.t = "AM_SENS";
    }

    public AMSensEvent(double value) {
        this.t = "AM_SENS";
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}
