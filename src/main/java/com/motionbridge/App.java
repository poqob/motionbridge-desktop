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

import atlantafx.base.theme.CupertinoLight;
import atlantafx.base.theme.CupertinoDark;
import atlantafx.base.theme.Styles;
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
        if (core.getAppConfig().isDarkTheme()) {
            Application.setUserAgentStylesheet(new CupertinoDark().getUserAgentStylesheet());
        } else {
            Application.setUserAgentStylesheet(new CupertinoLight().getUserAgentStylesheet());
        }

        primaryStage.setTitle("MotionBridge Server");

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // --- DASHBOARD TAB ---
        Tab dashboardTab = new Tab("Gösterge Paneli");
        VBox dashboardRoot = new VBox(15);
        dashboardRoot.setPadding(new Insets(15));

        Label statusLabel = new Label("Yayın Yapılıyor... (UDP: 44444, WS: 44445)");
        statusLabel.getStyleClass().addAll(Styles.SUCCESS, Styles.TEXT_BOLD);

        Label pendingLabel = new Label("Eşleşme Bekleyen Cihazlar:");
        pendingLabel.getStyleClass().add(Styles.TITLE_4);

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
        btnAccept.getStyleClass().add(Styles.SUCCESS);
        Button btnReject = new Button("Reddet");
        btnReject.getStyleClass().add(Styles.DANGER);

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

        HBox topSettingsBox = new HBox(20);
        VBox hostNameBox = new VBox(5);
        Label hostNameLabel = new Label("Yayın Adı (Hostname):");
        hostNameLabel.getStyleClass().add(Styles.TEXT_BOLD);
        TextField hostNameField = new TextField(core.getAppConfig().getHostName());
        hostNameField.setPromptText("Boş bırakılırsa bilgisayar adı kullanılır");
        Button btnSaveHost = new Button("Kaydet");
        btnSaveHost.getStyleClass().add(Styles.ACCENT);
        btnSaveHost.setOnAction(e -> {
            core.getAppConfig().setHostName(hostNameField.getText());
            core.getAppConfig().save();
            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                    "Cihaz adı kaydedildi! Eşleşme yayınında kullanılacak.", ButtonType.OK);
            alert.show();
        });
        hostNameBox.getChildren().addAll(hostNameLabel, hostNameField, btnSaveHost);

        VBox themeBox = new VBox(5);
        Label themeLabel = new Label("Tema (Karanlık/Aydınlık):");
        themeLabel.getStyleClass().add(Styles.TEXT_BOLD);
        ToggleButton themeToggle = new ToggleButton(core.getAppConfig().isDarkTheme() ? "Karanlık" : "Aydınlık");
        themeToggle.setSelected(core.getAppConfig().isDarkTheme());
        themeToggle.setOnAction(e -> {
            boolean isDark = themeToggle.isSelected();
            core.getAppConfig().setDarkTheme(isDark);
            core.getAppConfig().save();
            themeToggle.setText(isDark ? "Karanlık" : "Aydınlık");
            if (isDark) {
                Application.setUserAgentStylesheet(new CupertinoDark().getUserAgentStylesheet());
            } else {
                Application.setUserAgentStylesheet(new CupertinoLight().getUserAgentStylesheet());
            }
        });
        themeBox.getChildren().addAll(themeLabel, themeToggle);

        topSettingsBox.getChildren().addAll(hostNameBox, themeBox);

        Label registeredLabel = new Label("Eşleşmiş (Yetkili) Cihazlar:");
        registeredLabel.getStyleClass().add(Styles.TITLE_4);

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
        btnUnpair.getStyleClass().add(Styles.WARNING);
        btnUnpair.setOnAction(e -> {
            DeviceRegistration selected = registeredListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                core.unpairDevice(selected);
                registeredList.remove(selected);
            }
        });

        Label trackpadLabel = new Label("Trackpad Ayarları:");
        trackpadLabel.getStyleClass().add(Styles.TITLE_4);

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
                topSettingsBox,
                new Separator(),
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
