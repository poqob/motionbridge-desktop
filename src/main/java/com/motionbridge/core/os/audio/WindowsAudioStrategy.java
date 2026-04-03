package com.motionbridge.core.os.audio;

import java.io.IOException;

public class WindowsAudioStrategy implements AudioStrategy {
    @Override
    public void setVolume(double volume) {
        // Without third-party libraries, setting exact volume on Windows is hard via
        // command line natively.
        // As a simple alternative, we can just print a warning. For full support, a
        // native JNI call or an external tool like nircmd is required.
        System.err.println(
                "Volume control is not natively supported on Windows Command Line. Please implement JNI or use nircmd.");
    }

    @Override
    public void setMute(boolean mute) {
        // We can simulate pressing the Mute key
        try {
            // Volume Mute Key is usually 173
            String command = "powershell.exe -Command \"(new-object -com wscript.shell).SendKeys([char]173)\"";
            Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}