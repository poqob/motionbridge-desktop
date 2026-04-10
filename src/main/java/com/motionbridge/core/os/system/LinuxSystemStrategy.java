package com.motionbridge.core.os.system;

import com.motionbridge.core.models.SystemEvent;

import java.io.IOException;

public class LinuxSystemStrategy implements SystemStrategy {
    @Override
    public void handleSystemEvent(SystemEvent event) {
        String action = event.getAction();
        if ("LOCK".equals(action)) {
            try {
                // The prompt asked to use loginctl with Super_L+L
                new ProcessBuilder("loginctl", "lock-session").start();
            } catch (IOException e) {
                System.err.println(
                        "Failed to execute loginctl to lock screen. ");
                e.printStackTrace();
            }
        } else if ("POWEROFF".equals(action)) {
            try {
                new ProcessBuilder("systemctl", "poweroff").start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if ("REBOOT".equals(action)) {
            try {
                new ProcessBuilder("systemctl", "reboot").start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
