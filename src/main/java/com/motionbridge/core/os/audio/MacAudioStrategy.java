package com.motionbridge.core.os.audio;

import java.io.IOException;

public class MacAudioStrategy implements AudioStrategy {
    @Override
    public void setVolume(double volume) {
        int percent = (int) (volume * 100);
        try {
            Runtime.getRuntime().exec(new String[] { "osascript", "-e", "set volume output volume " + percent });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setMute(boolean mute) {
        try {
            String state = mute ? "true" : "false";
            Runtime.getRuntime().exec(new String[] { "osascript", "-e", "set volume output muted " + state });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}