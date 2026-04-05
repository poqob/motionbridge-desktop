package com.motionbridge.core.os.media;

public class MacMediaStrategy implements MediaStrategy {

    @Override
    public void playPause() {
        executeAppleScript("tell application \"System Events\" to key code 100"); // Play/Pause
    }

    @Override
    public void next() {
        executeAppleScript("tell application \"System Events\" to key code 101"); // Next
    }

    @Override
    public void previous() {
        executeAppleScript("tell application \"System Events\" to key code 98"); // Previous
    }

    private void executeAppleScript(String script) {
        try {
            String[] cmd = { "osascript", "-e", script };
            Runtime.getRuntime().exec(cmd);
        } catch (Exception e) {
            System.err.println("Failed to execute Mac media command.");
            e.printStackTrace();
        }
    }
}
