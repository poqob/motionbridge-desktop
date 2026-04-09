package com.motionbridge.core.models;

public class ClipboardEvent extends MBEvent {
    private String text;

    public ClipboardEvent() {
        this.t = "CLIP";
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}