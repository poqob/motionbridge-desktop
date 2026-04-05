package com.motionbridge.core.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;

public class AppConfig {
    private static final String CONFIG_FILE = "app_config.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private double scrollSpeed = 0.05;
    private double pointerSpeed = 1.6;
    private String hostName = "";
    private boolean darkTheme = false;

    public static AppConfig load() {
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                AppConfig config = gson.fromJson(reader, AppConfig.class);
                if (config != null)
                    return config;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new AppConfig();
    }

    public void save() {
        try (Writer writer = new FileWriter(CONFIG_FILE)) {
            gson.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public double getScrollSpeed() {
        return scrollSpeed;
    }

    public void setScrollSpeed(double scrollSpeed) {
        this.scrollSpeed = scrollSpeed;
    }

    public double getPointerSpeed() {
        return pointerSpeed;
    }

    public void setPointerSpeed(double pointerSpeed) {
        this.pointerSpeed = pointerSpeed;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public boolean isDarkTheme() {
        return darkTheme;
    }

    public void setDarkTheme(boolean darkTheme) {
        this.darkTheme = darkTheme;
    }
}
