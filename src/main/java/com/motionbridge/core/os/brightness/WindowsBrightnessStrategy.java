package com.motionbridge.core.os.brightness;

import java.io.IOException;

public class WindowsBrightnessStrategy implements BrightnessStrategy {
    @Override
    public void setBrightness(double value) {
        int percent = Math.max(0, Math.min(100, (int) (value * 100)));
        try {
            String command = String.format(
                    "powershell.exe -Command \"(Get-WmiObject -Namespace root/WMI -Class WmiMonitorBrightnessMethods).WmiSetBrightness(1,%d)\"",
                    percent);
            Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            System.err.println("Could not set Windows brightness: " + e.getMessage());
        }
    }
}
