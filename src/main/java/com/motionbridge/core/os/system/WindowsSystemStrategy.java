package com.motionbridge.core.os.system;

import com.motionbridge.core.models.SystemEvent;

import java.io.IOException;

public class WindowsSystemStrategy implements SystemStrategy {
    @Override
    public void handleSystemEvent(SystemEvent event) {
        String action = event.getAction();
        if ("LOCK".equals(action)) {
            try {
                Runtime.getRuntime().exec("rundll32.exe user32.dll,LockWorkStation");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if ("POWEROFF".equals(action)) {
            try {
                Runtime.getRuntime().exec("shutdown.exe -s -t 0");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if ("REBOOT".equals(action)) {
            try {
                Runtime.getRuntime().exec("shutdown.exe -r -t 0");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
