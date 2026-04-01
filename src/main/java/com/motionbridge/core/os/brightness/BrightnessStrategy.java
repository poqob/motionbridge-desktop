package com.motionbridge.core.os.brightness;

public interface BrightnessStrategy {
    /**
     * Sets the brightness.
     * 
     * @param value 0.0 to 1.0
     */
    void setBrightness(double value);
}
