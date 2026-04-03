package com.motionbridge.core.os.audio;

public interface AudioStrategy {
    void setVolume(double volume); // 0.0 to 1.0

    void setMute(boolean mute);
}