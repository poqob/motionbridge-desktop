package com.motionbridge.core.network;

import com.motionbridge.core.models.DeviceRegistration;

public interface DeviceListener {
    void onDeviceDiscovered(DeviceRegistration device);

    default void onDeviceRegistered(DeviceRegistration device) {
    }

    default void onDeviceUnpaired(DeviceRegistration device) {
    }
}
