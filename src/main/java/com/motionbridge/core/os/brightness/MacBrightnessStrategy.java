package com.motionbridge.core.os.brightness;

import java.io.IOException;

public class MacBrightnessStrategy implements BrightnessStrategy {
    @Override
    public void setBrightness(double value) {
        // Mac'te osascript (AppleScript) veya 'brightness' tool ile yapılabilir
        // 0.0 - 1.0 skalasında olan value'yu direkt kullanabilirsiniz
        double boundedValue = Math.max(0.0, Math.min(1.0, value));
        try {
            // "brightness" aracı brew ile yüklenen standart bir araçtır.
            // Alternatif bir applescript eklenebilir, fakat terminale düşmek en basitidir.
            String[] cmd = { "osascript", "-e",
                    "tell application \"System Events\" to tell module \"Displays\" to set brightness to "
                            + boundedValue };
            Runtime.getRuntime().exec(cmd);
            System.out.println("Mac brightness adjusted to " + boundedValue);
        } catch (IOException e) {
            System.err.println("Could not set Mac brightness: " + e.getMessage());
        }
    }
}
