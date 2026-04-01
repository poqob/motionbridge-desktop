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
    private final EventProcessor eventProcessor;
    private final DeviceRegistry deviceRegistry;
    private final Set<WebSocket> authenticatedSessions = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public WebSocketEventServer(int port, EventProcessor eventProcessor, DeviceRegistry deviceRegistry) {
        super(new InetSocketAddress(port));
        this.eventProcessor = eventProcessor;
        this.deviceRegistry = deviceRegistry;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        String ip = conn.getRemoteSocketAddress().getAddress().getHostAddress();
        System.out.println("New WebSocket connection: " + ip);

        // Cihaz zaten IP üzerinden yetkiliyse, auth paketini beklemeden doğrudan
        // yetkilendir.
        if (deviceRegistry.isIpActive(ip)) {
            authenticatedSessions.add(conn);
            System.out.println("WebSocket automatically authenticated by IP: " + ip);
        } else {
            System.out.println("Waiting for auth packet from: " + ip);
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Closed WebSocket connection: " + conn.getRemoteSocketAddress() + " with exit code " + code
                + " additional info: " + reason);
        authenticatedSessions.remove(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            if (!authenticatedSessions.contains(conn)) {
                JsonObject jsonObject = JsonParser.parseString(message).getAsJsonObject();
                if (jsonObject.has("type") && "auth".equals(jsonObject.get("type").getAsString())) {
                    String id = jsonObject.has("id") ? jsonObject.get("id").getAsString() : null;
                    if (id != null && deviceRegistry.isDeviceRegistered(id)) {
                        authenticatedSessions.add(conn);
                        System.out.println("WebSocket authenticated for device ID: " + id);
                    } else {
                        System.out.println("WebSocket auth failed or unrecognised ID.");
                        conn.close(1008, "Unauthorized");
                    }
                } else {
                    conn.close(1008, "Auth required");
                }
                return;
            }

            MBEvent event = EventParser.parse(message);
            if (event != null) {
                System.out.println("Received event from WebSocket: " + event.getClass().getSimpleName());
                eventProcessor.enqueueEvent(event);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
