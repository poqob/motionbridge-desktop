package com.motionbridge.core.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.motionbridge.core.models.*;
import com.motionbridge.core.parser.EventParser;
import com.motionbridge.core.processor.EventProcessor;
import com.motionbridge.core.registry.DeviceRegistry;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.Collections;

public class WebSocketEventServer extends WebSocketServer {
    private DeviceListener deviceListener;
    private final EventProcessor eventProcessor;
    private final DeviceRegistry deviceRegistry;
    private final Set<WebSocket> authenticatedSessions = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final ConcurrentHashMap<String, WebSocket> activeSessions = new ConcurrentHashMap<>();

    public int getAuthenticatedSessionsCount() {
        return authenticatedSessions.size();
    }

    public void disconnectDevice(String deviceId) {
        if (deviceId == null)
            return;
        WebSocket conn = activeSessions.remove(deviceId);
        DeviceRegistration device = deviceRegistry.getRegisteredDevice(deviceId);
        if (conn != null) {
            authenticatedSessions.remove(conn);
            if (conn.isOpen()) {
                conn.close(1000, "Unpaired by desktop");
            }
        }
        if (device != null) {
            deviceRegistry.removeRegisteredDevice(device);
        }
        System.out.println("Device " + deviceId + " unpaired and connection closed.");
    }

    private final ConcurrentHashMap<String, WebSocket> pendingSessions = new ConcurrentHashMap<>();

    public void setDeviceListener(DeviceListener deviceListener) {
        this.deviceListener = deviceListener;
    }

    public WebSocketEventServer(int port, EventProcessor eventProcessor, DeviceRegistry deviceRegistry) {
        super(new InetSocketAddress(port));
        this.eventProcessor = eventProcessor;
        this.deviceRegistry = deviceRegistry;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        String ip = conn.getRemoteSocketAddress().getAddress().getHostAddress();
        System.out.println("New WebSocket connection: " + ip);
        System.out.println("Waiting for auth/connection packet from: " + ip);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Closed WebSocket connection: " + conn.getRemoteSocketAddress() + " with exit code " + code
                + " additional info: " + reason);
        authenticatedSessions.remove(conn);
        activeSessions.values().remove(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            if (!authenticatedSessions.contains(conn)) {
                JsonObject jsonObject = JsonParser.parseString(message).getAsJsonObject();
                if (jsonObject.has("type")) {
                    String type = jsonObject.get("type").getAsString();
                    if ("auth".equals(type)) {
                        String id = jsonObject.has("id") ? jsonObject.get("id").getAsString() : null;
                        if (id != null && deviceRegistry.isDeviceRegistered(id)) {
                            String ip = conn.getRemoteSocketAddress().getAddress().getHostAddress();
                            deviceRegistry.updateRegisteredDeviceIp(id, ip);
                            authenticatedSessions.add(conn);
                            activeSessions.put(id, conn);
                            System.out.println("WebSocket authenticated for device ID: " + id);
                        } else {
                            System.out.println("WebSocket auth failed or unrecognised ID.");
                            conn.close(1008, "Unauthorized");
                        }
                    } else if ("connection_request".equals(type)) {
                        String deviceId = jsonObject.has("device_id") ? jsonObject.get("device_id").getAsString()
                                : null;
                        String deviceName = jsonObject.has("device_name") ? jsonObject.get("device_name").getAsString()
                                : null;

                        if (deviceId != null && deviceName != null) {
                            String ip = conn.getRemoteSocketAddress().getAddress().getHostAddress();
                            if (deviceRegistry.isDeviceRegistered(deviceId)) {
                                // Auto-accept registered device
                                deviceRegistry.updateRegisteredDeviceIp(deviceId, ip);
                                authenticatedSessions.add(conn);
                                activeSessions.put(deviceId, conn);
                                JsonObject successJson = new JsonObject();
                                successJson.addProperty("type", "pairing_success");
                                conn.send(successJson.toString());
                                System.out
                                        .println("Device auto-accepted as it was previously registered: " + deviceName);
                                return;
                            }

                            DeviceRegistration pendingDevice = new DeviceRegistration();
                            pendingDevice.setId(deviceId);
                            pendingDevice.setName(deviceName);
                            pendingDevice.setIp(ip);

                            deviceRegistry.addPendingDevice(pendingDevice);
                            pendingSessions.put(deviceId, conn);
                            if (deviceListener != null) {
                                deviceListener.onDeviceDiscovered(pendingDevice);
                            }
                            System.out.println("New device requested connection: " + deviceName);
                        }
                    } else if ("pairing_request".equals(type)) {
                        String deviceId = jsonObject.has("device_id") ? jsonObject.get("device_id").getAsString()
                                : null;
                        String pairingCode = jsonObject.has("pairing_code")
                                ? jsonObject.get("pairing_code").getAsString()
                                : null;

                        if (deviceId != null && pairingCode != null) {
                            DeviceRegistration pendingDevice = deviceRegistry.getPendingDevice(deviceId);
                            if (pendingDevice != null && pairingCode.equals(pendingDevice.getPairingCode())) {
                                // Eşleşme başarılı!
                                deviceRegistry.registerDevice(pendingDevice);
                                deviceRegistry.removePendingDevice(deviceId);
                                authenticatedSessions.add(conn);
                                activeSessions.put(deviceId, conn);
                                pendingSessions.remove(deviceId);

                                if (deviceListener != null) {
                                    deviceListener.onDeviceRegistered(pendingDevice);
                                }

                                JsonObject successJson = new JsonObject();
                                successJson.addProperty("type", "pairing_success");
                                conn.send(successJson.toString());

                                System.out.println("Pairing successful for device: " + pendingDevice.getName());
                            } else {
                                JsonObject failJson = new JsonObject();
                                failJson.addProperty("type", "pairing_failed");
                                failJson.addProperty("reason", "Invalid pairing code or device not found");
                                conn.send(failJson.toString());
                            }
                        }
                    } else {
                        conn.close(1008, "Auth required");
                    }
                } else {
                    conn.close(1008, "Auth required");
                }
                return;
            }

            if (message.contains("\"unpair_request\"")) {
                try {
                    JsonObject jsonObject = JsonParser.parseString(message).getAsJsonObject();
                    if (jsonObject.has("type") && "unpair_request".equals(jsonObject.get("type").getAsString())) {
                        String deviceId = jsonObject.has("device_id") ? jsonObject.get("device_id").getAsString()
                                : null;
                        if (deviceId != null) {
                            DeviceRegistration dev = deviceRegistry.getRegisteredDevice(deviceId);
                            disconnectDevice(deviceId);
                            if (deviceListener != null && dev != null) {
                                deviceListener.onDeviceUnpaired(dev);
                            }
                        }
                        return;
                    }
                } catch (Exception ignored) {
                }
            }

            MBEvent event = EventParser.parse(message);
            if (event != null) {
                // System.out.println("Received event from WebSocket: " +
                // event.getClass().getSimpleName());
                eventProcessor.enqueueEvent(event);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void acceptConnectionRequest(String deviceId, String pairingCode) {
        WebSocket conn = pendingSessions.get(deviceId);
        if (conn != null && conn.isOpen()) {
            JsonObject json = new JsonObject();
            json.addProperty("type", "connection_accepted");
            json.addProperty("pairing_code", pairingCode);
            conn.send(json.toString());
        }
    }

    public void rejectConnectionRequest(String deviceId, String reason) {
        WebSocket conn = pendingSessions.get(deviceId);
        if (conn != null && conn.isOpen()) {
            JsonObject json = new JsonObject();
            json.addProperty("type", "connection_rejected");
            json.addProperty("reason", reason);
            conn.send(json.toString());
            conn.close(1008, reason);
        }
        pendingSessions.remove(deviceId);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println(
                "WebSocket Error on connection " + (conn != null ? conn.getRemoteSocketAddress() : "null") + ":" + ex);
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("WebSocket Server started on port: " + getPort());
    }
}
