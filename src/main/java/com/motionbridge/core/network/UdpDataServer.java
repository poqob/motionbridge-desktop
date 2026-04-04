package com.motionbridge.core.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.motionbridge.core.models.*;
import com.motionbridge.core.parser.EventParser;
import com.motionbridge.core.processor.EventProcessor;
import com.motionbridge.core.registry.DeviceRegistry;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UdpDataServer implements Runnable {
    private static final int UDP_PORT = 44444;
    private static final int WS_PORT = 44445;
    private final DeviceListener listener;
    private volatile boolean running = true;
    private DatagramSocket socket;
    private final Gson gson = new Gson();

    private final EventProcessor eventProcessor;
    private final DeviceRegistry deviceRegistry;

    public UdpDataServer(DeviceListener listener, EventProcessor eventProcessor, DeviceRegistry deviceRegistry) {
        this.listener = listener;
        this.eventProcessor = eventProcessor;
        this.deviceRegistry = deviceRegistry;
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(UDP_PORT);
            socket.setBroadcast(true);
            // System.out.println("UDP Data Server listening on UDP port " + UDP_PORT);

            byte[] buffer = new byte[2048];

            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String jsonString = new String(packet.getData(), 0, packet.getLength()).trim();

                try {
                    JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
                    String sourceIp = packet.getAddress().getHostAddress();

                    if (jsonObject.has("type") && "pairing_request".equals(jsonObject.get("type").getAsString())) {
                        DeviceRegistration device = gson.fromJson(jsonObject, DeviceRegistration.class);
                        if (device != null && listener != null) {
                            // Update IP in case it changed or wasn't sent correctly
                            device.setIp(sourceIp);
                            listener.onDeviceDiscovered(device);
                            // Sadece kayitliyse ACK dondurur. Kayit yeni yapilmissa UI tarafindan
                            // gonderilecek.
                            if (deviceRegistry.isDeviceRegistered(device.getId())) {
                                sendAckPacket(device); // Handshake mesajını geri gönder.
                            }
                        }
                    } else if (jsonObject.has("t")) {
                        // Eğer aktif eşleşmiş bir cihaz varsa veya IP aktifse paketleri kabul et
                        // Hotspot kullanımında Android UDP için farklı bir IP arayüzü (100.x.x.x gibi)
                        // kullanabiliyor.
                        boolean isRegisteredDevicePresent = deviceRegistry.getRegisteredDevices().size() > 0;
                        if (deviceRegistry.isIpActive(sourceIp) || isRegisteredDevicePresent) {
                            if (!deviceRegistry.isIpActive(sourceIp) && isRegisteredDevicePresent) {
                                // Bu yeni UDP IP'sini geçici olarak aktif listesine alalım
                                deviceRegistry.blockIpTemporarily(sourceIp); // just an example, maybe we need active
                                                                             // string
                                // Let's just process it anyway
                                // System.out.println("Accepting UDP from alternative IP: " + sourceIp);
                            }
                            MBEvent event = EventParser.parse(jsonString);
                            if (event != null) {
                                eventProcessor.enqueueEvent(event);
                            }
                        } else {
                            // System.out.println("UDP Packet ignored from inactive IP: " + sourceIp);
                        }
                    }
                } catch (Exception e) {
                    // System.err.println("Invalid UDP Packet: " + jsonString);
                }
            }
        } catch (Exception e) {
            if (running) {
                e.printStackTrace();
            }
        } finally {
            stop();
        }
    }

    public void stop() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    public void sendAckPacket(DeviceRegistration device) {
        try {
            String hostName = System.getenv("COMPUTERNAME");
            if (hostName == null)
                hostName = System.getenv("HOSTNAME");
            if (hostName == null)
                hostName = "MotionBridge-Desktop";

            DiscoveryAck ack = new DiscoveryAck(hostName, UDP_PORT, WS_PORT);
            String jsonAck = gson.toJson(ack);

            byte[] sendData = jsonAck.getBytes();
            InetAddress clientIP = InetAddress.getByName(device.getIp());
            int clientPort = device.getPort();

            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientIP, clientPort);
            DatagramSocket sendSocket = new DatagramSocket();
            sendSocket.send(sendPacket);
            sendSocket.close();

            // System.out.println("Ack paket gonderildi -> " + device.getIp() + ":" +
            // device.getPort());

        } catch (Exception e) {
            System.err.println("Failed to send Handshake ACK: " + e.getMessage());
        }
    }
}
