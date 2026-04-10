package com.motionbridge.core.os.system;

import com.motionbridge.core.models.SystemEvent;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

public class SystemHandler {
    private final SystemStrategy strategy;

    public SystemHandler() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            strategy = new WindowsSystemStrategy();
        } else if (osName.contains("mac")) {
            strategy = new MacSystemStrategy();
        } else {
            strategy = new LinuxSystemStrategy();
        }
    }

    public void handleSystemEvent(SystemEvent event) {
        String action = event.getAction();
        if ("POWEROFF".equals(action) || "REBOOT".equals(action)) {
            Platform.runLater(() -> showCountdownDialog(action, event));
        } else {
            strategy.handleSystemEvent(event);
        }
    }

    private void showCountdownDialog(String action, SystemEvent event) {
        String actionName = "REBOOT".equals(action) ? "yeniden başlatılacak" : "kapatılacak";

        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Sistem Uyarısı");
        alert.setHeaderText("Sistem Komutu Alındı!");
        alert.setContentText("Bilgisayarınız 30 saniye içinde " + actionName + ".");

        ButtonType btnCancel = new ButtonType("İptal", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnCancel);

        AtomicInteger timeLeft = new AtomicInteger(30);
        Timer timer = new Timer(true);

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                int remaining = timeLeft.decrementAndGet();
                if (remaining <= 0) {
                    timer.cancel();
                    Platform.runLater(() -> {
                        alert.close();
                        strategy.handleSystemEvent(event);
                    });
                } else {
                    Platform.runLater(() -> {
                        alert.setContentText("Bilgisayarınız " + remaining + " saniye içinde " + actionName + ".");
                    });
                }
            }
        };

        timer.scheduleAtFixedRate(task, 1000, 1000);

        alert.showAndWait().ifPresent(response -> {
            if (response == btnCancel) {
                timer.cancel();
            }
        });

        alert.setOnHidden(e -> timer.cancel());
    }
}
