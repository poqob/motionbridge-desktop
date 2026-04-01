package com.motionbridge.core.os.brightness;

import com.motionbridge.core.models.DimmerEvent;

public class BrightnessHandler {
    private BrightnessStrategy strategy;

    private double currentBrightness = -1.0;
    private double targetBrightness = -1.0;
    private boolean isFading = false;

    public BrightnessHandler() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            strategy = new WindowsBrightnessStrategy();
        } else if (os.contains("mac")) {
            strategy = new MacBrightnessStrategy();
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            strategy = new LinuxBrightnessStrategy();
        } else {
            System.err.println("Unsupported OS for brightness control: " + os);
        }
    }

    public synchronized void handleDimmer(DimmerEvent event) {
        if (strategy == null) return;
        
        targetBrightness = event.getValue();
        
        if (!isFading) {
            isFading = true;
            startFadeThread();
        }
    }

    private void startFadeThread() {
        Thread fadeThread = new Thread(() -> {
            while (isFading) {
                try {
                    Thread.sleep(50); // Saniyede 20 kare güncelleme
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                synchronized (this) {
                    // Start from target on the first run
                    if (currentBrightness < 0) {
                        currentBrightness = targetBrightness;
                        strategy.setBrightness(currentBrightness);
                        isFading = false;
                        break;
                    }
                    
                    double diff = targetBrightness - currentBrightness;
                    
                    // Çok yaklaştığımızda direk hedefe otur ve durdur
                    if (Math.abs(diff) < 0.02) {
                        currentBrightness = targetBrightness;
                        strategy.setBrightness(currentBrightness);
                        isFading = false;
                        break;
                    }
                    
                    // Farkın %20'si kadar hareket et (Ease-Out hareketi) veya en az %3'lük minimum adım at
                    double step = diff * 0.2;
                    if (Math.abs(step) < 0.03) {
                        step = Math.signum(diff) * 0.03;
                    }
                    
                    currentBrightness += step;
                    
                    // Sınırlandırma (Clamp)
                    if (currentBrightness < 0.0) currentBrightness = 0.0;
                    if (currentBrightness > 1.0) currentBrightness = 1.0;
                    
                    strategy.setBrightness(currentBrightness);
                }
            }
        });
        fadeThread.setDaemon(true);
        fadeThread.start();
    }
}
