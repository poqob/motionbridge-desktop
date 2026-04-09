package com.motionbridge.core.os.system;

import com.motionbridge.core.models.SystemEvent;

import java.io.IOException;

public class LinuxSystemStrategy implements SystemStrategy {
    @Override
    public void handleSystemEvent(SystemEvent event) {
        if ("LOCK".equals(event.getAction())) {
            try {
                // The prompt asked to use loginctl with Super_L+L
                new ProcessBuilder("loginctl", "lock-session").start();
            } catch (IOException e) {
                System.err.println(
                        "Failed to execute loginctl to lock screen. ");
                e.printStackTrace();
            }
        }
    }
}
