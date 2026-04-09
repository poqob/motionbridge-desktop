package com.motionbridge.core.os.system;

import com.motionbridge.core.models.SystemEvent;

import java.io.IOException;

public class MacSystemStrategy implements SystemStrategy {
    @Override
    public void handleSystemEvent(SystemEvent event) {
        if ("LOCK".equals(event.getAction())) {
            try {
                // Command to lock mac
                Runtime.getRuntime().exec(new String[]{"pmset", "displaysleepnow"});
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
