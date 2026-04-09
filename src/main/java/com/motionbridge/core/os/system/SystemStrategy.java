package com.motionbridge.core.os.system;

import com.motionbridge.core.models.SystemEvent;

public interface SystemStrategy {
    void handleSystemEvent(SystemEvent event);
}
