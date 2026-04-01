package com.motionbridge.core.os.brightness;

import java.io.IOException;

public class LinuxBrightnessStrategy implements BrightnessStrategy {
    @Override
    public void setBrightness(double value) {
        double boundedValue = Math.max(0.1, Math.min(1.0, value)); // min %10 diyelim
        try {
            System.out.println("Linux Brightness set to: " + boundedValue);

            // ProcessBuilder kullanarak daha iyi hata yakalama
            ProcessBuilder pb = new ProcessBuilder("brightnessctl", "set", (int) (boundedValue * 100) + "%");
            Process p = pb.start();

            int exitCode = p.waitFor();
            if (exitCode != 0) {
                // Hata mesajını oku ve yazdır
                java.io.InputStream errorStream = p.getErrorStream();
                java.util.Scanner scanner = new java.util.Scanner(errorStream).useDelimiter("\\A");
                String errorMsg = scanner.hasNext() ? scanner.next() : "Bilinmeyen hata";
                System.err.println("brightnessctl error (exit " + exitCode + "): " + errorMsg.trim());
            }
        } catch (Exception e) {
            System.err.println("Could not set Linux brightness: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
