package com.motionbridge.core.models;

public class DictationEvent extends MBEvent {
    public String text;
    public boolean is_final;

    public DictationEvent() {
        this.t = "DICT";
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isFinal() {
        return is_final;
    }

    public void setFinal(boolean is_final) {
        this.is_final = is_final;
    }
}