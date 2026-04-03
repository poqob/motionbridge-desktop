package com.motionbridge.core.os.audio;

import com.motionbridge.core.models.MuteEvent;
import com.motionbridge.core.models.VolumeEvent;

public class AudioHandler {
    private final AudioStrategy strategy;

    public AudioHandler() {
        String osItem = System.getProperty("os.name").toLowerCase();
        if (osItem.contains("win")) {
            strategy = new WindowsAudioStrategy();
        } else if (osItem.contains("mac")) {
            strategy = new MacAudioStrategy();
        } else {
            strategy = new LinuxAudioStrategy();
        }
    }

    public void handleVolume(VolumeEvent event) {
        if (event == null)
            return;
        strategy.setVolume(event.getVolume());
    }

    public void handleMute(MuteEvent event) {
        if (event == null)
            return;
        strategy.setMute(event.isMuted());
    }
}