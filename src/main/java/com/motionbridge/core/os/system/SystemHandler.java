package com.motionbridge.core.os.system;

import com.motionbridge.core.models.SystemEvent;

public class SystemHandler {
    private final SystemStrategy strategy;

    public SystemHandler() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            strategy = new WindowsSystemStrategy();
        } else if (osName.contains("mac")) {
            strategy = new MacSystemStrategy();
        } else {
            strategy = new LinuxSystemStrategy();
        }
    }

    public void handleSystemEvent(SystemEvent event) {
        strategy.handleSystemEvent(event);
    }
}
