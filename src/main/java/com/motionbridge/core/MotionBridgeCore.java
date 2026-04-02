package com.motionbridge.core;

import com.motionbridge.core.models.DeviceRegistration;
import com.motionbridge.core.models.AppConfig;
import com.motionbridge.core.network.DeviceListener;
import com.motionbridge.core.network.UdpDataServer;
import com.motionbridge.core.network.WebSocketEventServer;
import com.motionbridge.core.network.HostBroadcaster;
import com.motionbridge.core.os.RobotMouseHandler;
import com.motionbridge.core.os.brightness.BrightnessHandler;
import com.motionbridge.core.processor.EventProcessor;
import com.motionbridge.core.registry.DeviceRegistry;

public class MotionBridgeCore {
    private UdpDataServer udpDataServer;
    private WebSocketEventServer webSocketServer;
    private HostBroadcaster hostBroadcaster;
    private Thread udpThread;
    private Thread processorThread;
    private Thread broadcasterThread;

    private final RobotMouseHandler mouseHandler;
    private final BrightnessHandler brightnessHandler;
    private final EventProcessor eventProcessor;
    private final DeviceRegistry deviceRegistry;
    private final AppConfig appConfig;

    private DeviceListener uiDeviceListener;

    public MotionBridgeCore() {
        this.appConfig = AppConfig.load();
        this.mouseHandler = new RobotMouseHandler(this.appConfig);
        this.brightnessHandler = new BrightnessHandler();
        this.eventProcessor = new EventProcessor(mouseHandler, brightnessHandler);
        this.deviceRegistry = new DeviceRegistry();
    }

    public void setDeviceListener(DeviceListener listener) {
        this.uiDeviceListener = listener;
        if (webSocketServer != null) {
            webSocketServer.setDeviceListener(listener);
        }
    }

    public void start() {
        System.out.println("Starting MotionBridge Core Services...");

        processorThread = new Thread(eventProcessor);
        processorThread.start();

        udpDataServer = new UdpDataServer(device -> {
            boolean approved = false;
            if (deviceRegistry.isDeviceRegistered(device.getId())) {
                deviceRegistry.registerDevice(device); // Rekayit icin (aktif ip listesine al)
                approved = true;
            } else if (!deviceRegistry.isIpBlocked(device.getIp()) && !deviceRegistry.isDevicePending(device.getId())) {
                System.out.println("New device discovered (pending approval): " + device);
                deviceRegistry.addPendingDevice(device);
                if (uiDeviceListener != null) {
                    uiDeviceListener.onDeviceDiscovered(device);
                }
            }
        }, eventProcessor, deviceRegistry);

        udpThread = new Thread(udpDataServer);
        udpThread.start();

        webSocketServer = new WebSocketEventServer(44445, eventProcessor, deviceRegistry);
        if (uiDeviceListener != null) {
            webSocketServer.setDeviceListener(uiDeviceListener);
        }
        webSocketServer.start();

        hostBroadcaster = new HostBroadcaster(44444, webSocketServer);
        broadcasterThread = new Thread(hostBroadcaster);
        broadcasterThread.start();

        System.out.println("Core services are running.");
    }

    public void stop() {
        System.out.println("Stopping MotionBridge Core Services...");
        if (hostBroadcaster != null) {
            hostBroadcaster.stop();
        }
        if (udpDataServer != null) {
            udpDataServer.stop();
        }
        if (webSocketServer != null) {
            try {
                webSocketServer.stop();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (eventProcessor != null) {
            eventProcessor.stop();
        }
        if (processorThread != null) {
            processorThread.interrupt();
        }
        if (udpThread != null) {
            udpThread.interrupt();
        }
        if (broadcasterThread != null) {
            broadcasterThread.interrupt();
        }
    }

    public DeviceRegistry getDeviceRegistry() {
        return deviceRegistry;
    }

    public AppConfig getAppConfig() {
        return appConfig;
    }

    public void sendDiscoveryAck(DeviceRegistration device) {
        if (udpDataServer != null) {
            udpDataServer.sendAckPacket(device);
        }
    }

    public void acceptConnectionRequest(String deviceId, String pairingCode) {
        if (webSocketServer != null) {
            webSocketServer.acceptConnectionRequest(deviceId, pairingCode);
        }
    }

    public void unpairDevice(DeviceRegistration device) {
        if (device != null) {
            String id = device.getId();
            if (webSocketServer != null) {
                webSocketServer.disconnectDevice(id);
            } else {
                deviceRegistry.removeRegisteredDevice(device);
            }
        }
    }

    public void rejectConnectionRequest(String deviceId, String reason) {
        if (webSocketServer != null) {
            webSocketServer.rejectConnectionRequest(deviceId, reason);
        }
    }

}
