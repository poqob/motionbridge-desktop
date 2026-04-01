package com.motionbridge.core.models;

public class Swipe3Event extends MBEvent {
    private String dir;

    public Swipe3Event() {
        this.t = "SWIPE_3";
    }

    public String getDir() {
        return dir;
    }
}
