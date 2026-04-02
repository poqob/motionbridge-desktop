package com.motionbridge.core.processor;

import com.motionbridge.core.models.*;
import com.motionbridge.core.os.RobotMouseHandler;
import com.motionbridge.core.os.brightness.BrightnessHandler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class EventProcessor implements Runnable {
    private final RobotMouseHandler mouseHandler;
    private final BrightnessHandler brightnessHandler;
    private final BlockingQueue<MBEvent> eventQueue = new LinkedBlockingQueue<>();
    private volatile boolean running = true;

    public EventProcessor(RobotMouseHandler mouseHandler, BrightnessHandler brightnessHandler) {
        this.mouseHandler = mouseHandler;
        this.brightnessHandler = brightnessHandler;
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
                processEvent(event);
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
        } else if (event instanceof DictationEvent) {
            mouseHandler.handleDictation((DictationEvent) event);
        }
    }
}
