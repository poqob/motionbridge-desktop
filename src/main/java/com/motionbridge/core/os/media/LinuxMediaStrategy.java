package com.motionbridge.core.os.media;

public class LinuxMediaStrategy implements MediaStrategy {

    @Override
    public void playPause() {
        executeCommand("xdotool key XF86AudioPlay");
    }

    @Override
    public void next() {
        executeCommand("xdotool key XF86AudioNext");
    }

    @Override
    public void previous() {
        executeCommand("xdotool key XF86AudioPrev");
    }

    private void executeCommand(String command) {
        // System.out.println("Executing Media Command: " + command);
        try {
            // If running as sudo/root, playerctl cannot access the current user's DBUS
            // session.
            String sudoUser = System.getenv("SUDO_USER");
            String[] cmdArray;

            if (sudoUser != null && !sudoUser.isEmpty()) {
                String display = System.getenv("DISPLAY");
                if (display == null || display.isEmpty()) {
                    display = ":0";
                }

                String envVars = "DISPLAY=" + display + " " +
                        "XDG_RUNTIME_DIR=/run/user/$(id -u " + sudoUser + ") " +
                        "DBUS_SESSION_BUS_ADDRESS=unix:path=/run/user/$(id -u " + sudoUser + ")/bus ";
                String shellCmd = "sudo -u " + sudoUser + " env " + envVars + command;
                cmdArray = new String[] { "sh", "-c", shellCmd };
                // System.out.println("Running as sudo-user: " + shellCmd);
            } else {
                cmdArray = command.split(" ");
            }

            Process process = Runtime.getRuntime().exec(cmdArray);
            process.waitFor();
            if (process.exitValue() != 0) {
                System.err.println("Media command exited with code " + process.exitValue());
                java.util.Scanner s = new java.util.Scanner(process.getErrorStream()).useDelimiter("\\A");
                if (s.hasNext()) {
                    System.err.println("Error output: " + s.next());
                }
                s.close();
            }
        } catch (Exception e) {
            System.err.println("Failed to execute Linux media command: " + command);
            e.printStackTrace();
        }
    }
}
