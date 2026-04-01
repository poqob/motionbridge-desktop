package com.motionbridge.core.models;

public class MouseClickEvent extends MBEvent {
    private int b;

    public MouseClickEvent() {
        this.t = "C";
    }

    public int getButton() {
        return b;
    }
}
