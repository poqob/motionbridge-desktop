package com.motionbridge.core.os.media;

import java.io.IOException;

public class WindowsMediaStrategy implements MediaStrategy {

    private final String PSH_CODE_PREFIX = "$code = \"using System; " +
            "using System.Runtime.InteropServices; " +
            "public class MediaControl { " +
            "[DllImport(\\\"user32.dll\\\")] public static extern void keybd_event(byte virtualKey, byte scanCode, uint flags, IntPtr extraInfo); "
            +
            "public static void PlayPause() { keybd_event(0xB3, 0, 0, IntPtr.Zero); keybd_event(0xB3, 0, 2, IntPtr.Zero); } "
            +
            "public static void Next() { keybd_event(0xB0, 0, 0, IntPtr.Zero); keybd_event(0xB0, 0, 2, IntPtr.Zero); } "
            +
            "public static void Previous() { keybd_event(0xB1, 0, 0, IntPtr.Zero); keybd_event(0xB1, 0, 2, IntPtr.Zero); } "
            +
            "}\"; Add-Type -TypeDefinition $code; ";

    @Override
    public void playPause() {
        executePowerShell("[MediaControl]::PlayPause()");
    }

    @Override
    public void next() {
        executePowerShell("[MediaControl]::Next()");
    }

    @Override
    public void previous() {
        executePowerShell("[MediaControl]::Previous()");
    }

    private void executePowerShell(String actionCommand) {
        try {
            String fullScript = PSH_CODE_PREFIX + actionCommand;
            ProcessBuilder pb = new ProcessBuilder("powershell", "-NoProfile", "-WindowStyle", "Hidden", "-Command",
                    fullScript);
            pb.start();
        } catch (IOException e) {
            System.err.println("Failed to execute Windows medial command.");
            e.printStackTrace();
        }
    }
}
