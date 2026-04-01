package com.motionbridge.core.network;

import com.google.gson.JsonObject;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class HostBroadcaster implements Runnable {
    private static final int BROADCAST_PORT = 44446; // The port the mobile app will be listening on for host broadcasts
    private volatile boolean running = true;
    private final int udpPort;

    public HostBroadcaster(int udpPort) {
        this.udpPort = udpPort;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);

            while (running) {
                String hostName = System.getenv("COMPUTERNAME");
                if (hostName == null)
                    hostName = System.getenv("HOSTNAME");
                if (hostName == null)
                    hostName = "MotionBridge-Desktop";

                JsonObject json = new JsonObject();
                json.addProperty("type", "host_announcement");
                json.addProperty("host_name", hostName);
                json.addProperty("data_port", udpPort);
                json.addProperty("ws_port", 44445);

                byte[] buffer = json.toString().getBytes();
                // Broadcast to local subnet
                InetAddress address = InetAddress.getByName("255.255.255.255");
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, BROADCAST_PORT);

                socket.send(packet);

                Thread.sleep(2000); // Broadcast every 2 seconds
            }
        } catch (Exception e) {
            if (running) {
                System.err.println("Host broadcasting failed: " + e.getMessage());
            }
        }
    }

    public void stop() {
        running = false;
    }
}
