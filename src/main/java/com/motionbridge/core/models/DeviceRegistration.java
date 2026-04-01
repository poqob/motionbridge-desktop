package com.motionbridge.core.models;

public class DeviceRegistration {
    private String id;
    private String name;
    private String role;
    private String os;
    private String ip;
    private int port;
    private int version;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }

    public String getOs() {
        return os;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "DeviceRegistration{id='" + id + "', name='" + name + "', ip='" + ip + "'}";
    }
}
