package com.motionbridge.core.os.system;

import com.motionbridge.core.models.SystemEvent;

import java.io.IOException;

public class MacSystemStrategy implements SystemStrategy {
    @Override
    public void handleSystemEvent(SystemEvent event) {
        String action = event.getAction();
        if ("LOCK".equals(action)) {
            try {
                // Command to lock mac
                Runtime.getRuntime().exec(new String[] { "pmset", "displaysleepnow" });
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if ("POWEROFF".equals(action)) {
            try {
                Runtime.getRuntime()
                        .exec(new String[] { "osascript", "-e", "tell app \"System Events\" to shut down" });
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if ("REBOOT".equals(action)) {
            try {
                Runtime.getRuntime().exec(new String[] { "osascript", "-e", "tell app \"System Events\" to restart" });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
