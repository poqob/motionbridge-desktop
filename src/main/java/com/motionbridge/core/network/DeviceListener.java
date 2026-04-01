package com.motionbridge.core.network;

import com.motionbridge.core.models.DeviceRegistration;

public interface DeviceListener {
    void onDeviceDiscovered(DeviceRegistration device);
}
