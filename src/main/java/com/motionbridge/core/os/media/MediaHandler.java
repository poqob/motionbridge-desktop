package com.motionbridge.core.os.media;

import com.motionbridge.core.models.MediaEvent;

public class MediaHandler {
    private final MediaStrategy strategy;

    public MediaHandler(MediaStrategy strategy) {
        this.strategy = strategy;
    }

    public void handleMedia(MediaEvent event) {
        String action = event.getAction();
        // System.out.println("Processing MediaEvent, action: " + action);
        if (action == null)
            return;

        switch (action.toUpperCase()) {
            case "PLAY":
            case "PAUSE":
                strategy.playPause();
                break;
            case "NEXT":
                strategy.next();
                break;
            case "PREV":
                strategy.previous();
                break;
            default:
                System.err.println("Unknown media action: " + action);
        }
    }
}
