package com.motionbridge.core.registry;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.motionbridge.core.models.DeviceRegistration;

import java.io.*;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class DeviceRegistry {
    private static final String REGISTRY_FILE = "device_registry.json";
    private final Gson gson = new Gson();

    private final List<DeviceRegistration> registeredDevices = new CopyOnWriteArrayList<>();
    private final Set<String> blockedIps = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<String> activeIps = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<String, DeviceRegistration> pendingDevices = new ConcurrentHashMap<>();

    public DeviceRegistry() {
        loadRegistry();
    }

    private void loadRegistry() {
        File file = new File(REGISTRY_FILE);
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                Type listType = new TypeToken<List<DeviceRegistration>>() {
                }.getType();
                List<DeviceRegistration> loaded = gson.fromJson(reader, listType);
                if (loaded != null) {
                    registeredDevices.addAll(loaded);
                    for (DeviceRegistration device : loaded) {
                        if (device.getIp() != null) {
                            activeIps.add(device.getIp());
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveRegistry() {
        try (Writer writer = new FileWriter(REGISTRY_FILE)) {
            gson.toJson(registeredDevices, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateRegisteredDeviceIp(String id, String ip) {
        if (id == null || ip == null)
            return;
        for (DeviceRegistration d : registeredDevices) {
            if (id.equals(d.getId())) {
                d.setIp(ip);
                activeIps.add(ip);
                saveRegistry();
                return;
            }
        }
    }

    public DeviceRegistration getRegisteredDevice(String id) {
        if (id == null) return null;
        for (DeviceRegistration d : registeredDevices) {
            if (id.equals(d.getId())) return d;
        }
        return null;
    }

    public boolean isDeviceRegistered(String id) {
        if (id == null)
            return false;
        return registeredDevices.stream().anyMatch(d -> id.equals(d.getId()));
    }

    public void registerDevice(DeviceRegistration device) {
        if (!isDeviceRegistered(device.getId())) {
            registeredDevices.add(device);
            saveRegistry();
        }
        activeIps.add(device.getIp());
    }

    public void removeRegisteredDevice(DeviceRegistration device) {
        if (device != null) {
            registeredDevices.removeIf(d -> d.getId().equals(device.getId()));
            activeIps.remove(device.getIp());
            saveRegistry();
        }
    }

    public boolean isIpBlocked(String ip) {
        return blockedIps.contains(ip);
    }

    public void blockIpTemporarily(String ip) {
        blockedIps.add(ip);
    }

    public boolean isIpActive(String ip) {
        return activeIps.contains(ip);
    }

    public boolean isDevicePending(String id) {
        return id != null && pendingDevices.containsKey(id);
    }

    public DeviceRegistration getPendingDevice(String id) {
        return id != null ? pendingDevices.get(id) : null;
    }

    public void addPendingDevice(DeviceRegistration device) {
        if (device != null && device.getId() != null) {
            pendingDevices.put(device.getId(), device);
        }
    }

    public void removePendingDevice(String id) {
        if (id != null) {
            pendingDevices.remove(id);
        }
    }

    public Map<String, DeviceRegistration> getPendingDevices() {
        return Collections.unmodifiableMap(pendingDevices);
    }

    public List<DeviceRegistration> getRegisteredDevices() {
        return Collections.unmodifiableList(registeredDevices);
    }
}
