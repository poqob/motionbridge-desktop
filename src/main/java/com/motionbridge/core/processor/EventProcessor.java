package com.motionbridge.core.processor;

import com.motionbridge.core.models.*;
import com.motionbridge.core.os.RobotMouseHandler;
import com.motionbridge.core.os.audio.AudioHandler;
import com.motionbridge.core.os.brightness.BrightnessHandler;
import com.motionbridge.core.os.media.MediaHandler;
import com.motionbridge.core.os.system.SystemHandler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class EventProcessor implements Runnable {
    private final RobotMouseHandler mouseHandler;
    private final BrightnessHandler brightnessHandler;
    private final AudioHandler audioHandler;
    private final MediaHandler mediaHandler;
    private final SystemHandler systemHandler;
    private final BlockingQueue<MBEvent> eventQueue = new LinkedBlockingQueue<>();
    private volatile boolean running = true;

    public EventProcessor(RobotMouseHandler mouseHandler, BrightnessHandler brightnessHandler,
            AudioHandler audioHandler, MediaHandler mediaHandler, SystemHandler systemHandler) {
        this.mouseHandler = mouseHandler;
        this.brightnessHandler = brightnessHandler;
        this.audioHandler = audioHandler;
        this.mediaHandler = mediaHandler;
        this.systemHandler = systemHandler;
    }

    public void enqueueEvent(MBEvent event) {
        if (event != null) {
            eventQueue.offer(event);
        }
    }

    @Override
    public void run() {
        while (running) {
            try {
                MBEvent event = eventQueue.take();
                try {
                    processEvent(event);
                } catch (Exception e) {
                    System.err.println("Error processing event: " + event.getClass().getSimpleName());
                    e.printStackTrace();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
    }

    public void stop() {
        running = false;
    }

    private void processEvent(MBEvent event) {
        if (event instanceof MouseMoveEvent) {
            mouseHandler.handleMove((MouseMoveEvent) event);
        } else if (event instanceof ScrollEvent) {
            mouseHandler.handleScroll((ScrollEvent) event);
        } else if (event instanceof DimmerEvent) {
            brightnessHandler.handleDimmer((DimmerEvent) event);
        } else if (event instanceof MouseDragEvent) {
            mouseHandler.handleDrag((MouseDragEvent) event);
        } else if (event instanceof MouseClickEvent) {
            mouseHandler.handleClick((MouseClickEvent) event);
        } else if (event instanceof MouseDragStartEvent) {
            mouseHandler.handleDragStart((MouseDragStartEvent) event);
        } else if (event instanceof MouseDragEndEvent) {
            mouseHandler.handleDragEnd((MouseDragEndEvent) event);
        } else if (event instanceof MouseDoubleClickEvent) {
            mouseHandler.handleDoubleClick((MouseDoubleClickEvent) event);
        } else if (event instanceof Swipe3Event) {
            mouseHandler.handleSwipe3((Swipe3Event) event);
        } else if (event instanceof Tap4Event) {
            mouseHandler.handleTap4((Tap4Event) event);
        } else if (event instanceof DictationEvent) {
            mouseHandler.handleDictation((DictationEvent) event);
        } else if (event instanceof ClipboardEvent) {
            mouseHandler.handleClipboard((ClipboardEvent) event);
        } else if (event instanceof VolumeEvent) {
            audioHandler.handleVolume((VolumeEvent) event);
        } else if (event instanceof MuteEvent) {
            audioHandler.handleMute((MuteEvent) event);
        } else if (event instanceof MediaEvent) {
            mediaHandler.handleMedia((MediaEvent) event);
        } else if (event instanceof SystemEvent) {
            systemHandler.handleSystemEvent((SystemEvent) event);
        }
    }
}
