package com.motionbridge;

import com.motionbridge.core.MotionBridgeCore;
import com.motionbridge.core.models.DeviceRegistration;
import javafx.application.Application;
import javafx.application.Platform;
import java.util.Random;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import com.motionbridge.core.network.DeviceListener;

public class App extends Application {
    private MotionBridgeCore core;
    private ObservableList<DeviceRegistration> pendingList;
    private ObservableList<DeviceRegistration> registeredList;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() throws Exception {
        core = new MotionBridgeCore();
        pendingList = FXCollections.observableArrayList();
        registeredList = FXCollections.observableArrayList(core.getDeviceRegistry().getRegisteredDevices());

        core.setDeviceListener(new DeviceListener() {
            @Override
            public void onDeviceDiscovered(DeviceRegistration device) {
                Platform.runLater(() -> {
                    boolean alreadyInList = pendingList.stream().anyMatch(d -> d.getId().equals(device.getId()));
                    if (!alreadyInList) {
                        pendingList.add(device);
                    }
                });
            }

            @Override
            public void onDeviceRegistered(DeviceRegistration device) {
                Platform.runLater(() -> {
                    pendingList.removeIf(d -> d.getId().equals(device.getId()));
                    boolean alreadyInList = registeredList.stream().anyMatch(d -> d.getId().equals(device.getId()));
                    if (!alreadyInList) {
                        registeredList.add(device);
                    }
                });
            }

            @Override
            public void onDeviceUnpaired(DeviceRegistration device) {
                Platform.runLater(() -> {
                    registeredList.removeIf(d -> d.getId().equals(device.getId()));
                });
            }
        });
        core.start();
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("MotionBridge Server");

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-font-family: 'Segoe UI', sans-serif;");

        // --- DASHBOARD TAB ---
        Tab dashboardTab = new Tab("Gösterge Paneli");
        VBox dashboardRoot = new VBox(15);
        dashboardRoot.setPadding(new Insets(15));

        Label statusLabel = new Label("Yayın Yapılıyor... (UDP: 44444, WS: 44445)");
        statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");

        Label pendingLabel = new Label("Eşleşme Bekleyen Cihazlar:");
        pendingLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        ListView<DeviceRegistration> pendingListView = new ListView<>(pendingList);
        pendingListView.setCellFactory(param -> new ListCell<DeviceRegistration>() {
            @Override
            protected void updateItem(DeviceRegistration device, boolean empty) {
                super.updateItem(device, empty);
                if (empty || device == null) {
                    setText(null);
                } else {
                    setText(device.getName() + " (" + device.getIp() + ")");
                }
            }
        });
        pendingListView.setPrefHeight(150);

        Button btnAccept = new Button("Kabul Et");
        btnAccept.setStyle(
                "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        Button btnReject = new Button("Reddet");
        btnReject.setStyle(
                "-fx-background-color: #F44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

        btnAccept.setOnAction(e -> {
            DeviceRegistration selected = pendingListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                // Eşleşme kodu oluştur (Örn: 6 haneli rastgele hex veya numara)
                String pairingCode = String.format("%06d", new Random().nextInt(999999));
                selected.setPairingCode(pairingCode);

                // Bunu kumandaya ilet
                core.acceptConnectionRequest(selected.getId(), pairingCode);

                // NOT: registerDevice'ı hemen yapmıyoruz! Kumanda send "pairing_request"
                // deyince WSServer kendisi halledecek
                // Kullanıcı bekleyişte olduğunu görsün diye UI listesinde beklemeye bırakıyoruz
                // ya da durum güncelliyoruz
                // Basitlik adina UI dan simdilik siliyoruz ama WS de pending de bekliyor.
                // Daha temiz olmasi adina UI listesinde tutmaya devam edip durumunu
                // güncelleyebiliriz ancak şu an için
                // listeden uçuruyorum. Eğer WS üzerinden `pairing_success` alırsak
                // registeredList'e eklenmesi lazım.
                // Ancak UI ile asenkron olduğundan bir listener eklememiz gerekecek.
            }
        });

        btnReject.setOnAction(e -> {
            DeviceRegistration selected = pendingListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                core.rejectConnectionRequest(selected.getId(), "User declined");
                core.getDeviceRegistry().blockIpTemporarily(selected.getIp());
                core.getDeviceRegistry().removePendingDevice(selected.getId());
                pendingList.remove(selected);
            }
        });

        HBox pendingActionBox = new HBox(10, btnAccept, btnReject);
        dashboardRoot.getChildren().addAll(statusLabel, pendingLabel, pendingListView, pendingActionBox);
        dashboardTab.setContent(dashboardRoot);

        // --- SETTINGS TAB ---
        Tab settingsTab = new Tab("Ayarlar");
        VBox settingsRoot = new VBox(20);
        settingsRoot.setPadding(new Insets(15));

        Label registeredLabel = new Label("Eşleşmiş (Yetkili) Cihazlar:");
        registeredLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        ListView<DeviceRegistration> registeredListView = new ListView<>(registeredList);
        registeredListView.setCellFactory(param -> new ListCell<DeviceRegistration>() {
            @Override
            protected void updateItem(DeviceRegistration device, boolean empty) {
                super.updateItem(device, empty);
                if (empty || device == null) {
                    setText(null);
                } else {
                    setText(device.getName() + " (" + device.getIp() + ")");
                }
            }
        });
        registeredListView.setPrefHeight(120);

        Button btnUnpair = new Button("Eşleşmeyi Sil");
        btnUnpair.setStyle(
                "-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        btnUnpair.setOnAction(e -> {
            DeviceRegistration selected = registeredListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                core.unpairDevice(selected);
                registeredList.remove(selected);
            }
        });

        Label trackpadLabel = new Label("Trackpad Ayarları:");
        trackpadLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label scrollSpeedLabel = new Label("Kaydırma Hızı (Scroll Speed):");
        Slider scrollSpeedSlider = new Slider(0.01, 0.2, core.getAppConfig().getScrollSpeed());
        scrollSpeedSlider.setShowTickMarks(true);
        scrollSpeedSlider.setShowTickLabels(true);
        scrollSpeedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            core.getAppConfig().setScrollSpeed(newVal.doubleValue());
            core.getAppConfig().save();
        });

        Label pointerSpeedLabel = new Label("İmleç Hızı (Pointer Speed):");
        Slider pointerSpeedSlider = new Slider(0.5, 5.0, core.getAppConfig().getPointerSpeed());
        pointerSpeedSlider.setShowTickMarks(true);
        pointerSpeedSlider.setShowTickLabels(true);
        pointerSpeedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            core.getAppConfig().setPointerSpeed(newVal.doubleValue());
            core.getAppConfig().save();
        });

        settingsRoot.getChildren().addAll(
                registeredLabel, registeredListView, btnUnpair,
                new Separator(),
                trackpadLabel, scrollSpeedLabel, scrollSpeedSlider,
                pointerSpeedLabel, pointerSpeedSlider);
        settingsTab.setContent(settingsRoot);

        tabPane.getTabs().addAll(dashboardTab, settingsTab);

        Scene scene = new Scene(tabPane, 450, 550);
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0); // Ensure all threads are terminated
        });
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        if (core != null) {
            core.stop();
        }
    }
}
