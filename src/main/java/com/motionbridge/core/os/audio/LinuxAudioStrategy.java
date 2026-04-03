package com.motionbridge.core.os.audio;

import java.io.IOException;

public class LinuxAudioStrategy implements AudioStrategy {
    @Override
    public void setVolume(double volume) {
        int percent = (int) (volume * 100);
        executeCommand("pactl set-sink-volume @DEFAULT_SINK@ " + percent + "%",
                "amixer sset Master " + percent + "%");
    }

    @Override
    public void setMute(boolean mute) {
        String pulseState = mute ? "1" : "0";
        String alsaState = mute ? "mute" : "unmute";
        executeCommand("pactl set-sink-mute @DEFAULT_SINK@ " + pulseState,
                "amixer sset Master " + alsaState);
    }

    private void executeCommand(String pulseCommand, String fallbackCommand) {
        try {
            String sudoUser = System.getenv("SUDO_USER");
            String commandToRun;

            if (sudoUser != null && !sudoUser.trim().isEmpty()) {
                // Uygulama 'sudo' ile calistirildiginda PulseAudio kullanici oturumu
                // ortam degiskenlerine baglidir. Gercek kullanicinin daemonuna ulasmak icin
                // sudo -u ve XDG_RUNTIME_DIR yonlendirmesi yapilir.
                commandToRun = String.format(
                        "USER_ID=$(id -u %s); sudo -u %s env XDG_RUNTIME_DIR=/run/user/$USER_ID %s || %s",
                        sudoUser, sudoUser, pulseCommand, fallbackCommand);
            } else {
                commandToRun = pulseCommand + " || " + fallbackCommand;
            }

            Runtime.getRuntime().exec(new String[] { "sh", "-c", commandToRun });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}