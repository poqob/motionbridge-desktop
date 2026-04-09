package com.motionbridge.core.os.system;

import com.motionbridge.core.models.SystemEvent;

import java.io.IOException;

public class WindowsSystemStrategy implements SystemStrategy {
    @Override
    public void handleSystemEvent(SystemEvent event) {
        if ("LOCK".equals(event.getAction())) {
            try {
                Runtime.getRuntime().exec("rundll32.exe user32.dll,LockWorkStation");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
