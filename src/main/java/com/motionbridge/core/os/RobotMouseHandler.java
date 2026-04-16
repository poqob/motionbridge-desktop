package com.motionbridge.core.os;

import com.motionbridge.core.models.MouseClickEvent;
import com.motionbridge.core.models.MouseDragEvent;
import com.motionbridge.core.models.MouseDragStartEvent;
import com.motionbridge.core.models.MouseDragEndEvent;
import com.motionbridge.core.models.MouseMoveEvent;
import com.motionbridge.core.models.MouseDoubleClickEvent;
import com.motionbridge.core.models.ScrollEvent;
import com.motionbridge.core.models.Swipe3Event;
import com.motionbridge.core.models.Tap4Event;
import com.motionbridge.core.models.DictationEvent;
import com.motionbridge.core.models.ClipboardEvent;
import com.motionbridge.core.models.CopyEvent;
import com.motionbridge.core.models.PasteEvent;
import com.motionbridge.core.models.AMModeEvent;
import com.motionbridge.core.models.AMSensEvent;
import com.motionbridge.core.models.AMMoveEvent;

import com.motionbridge.core.models.AppConfig;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Timer;
import java.util.TimerTask;

public class RobotMouseHandler {
    private Robot robot;
    private final AppConfig appConfig;

    // Hedef ivmeler (Velocity) - donanımsal akıcılık (soft geçiş) için
    private double vx = 0.0;
    private double vy = 0.0;
    private double scrollV = 0.0;

    // Küsüratların boşa gitmemesi için biriktiriciler (Accumulators)
    private double fractionalX = 0.0;
    private double fractionalY = 0.0;
    private double fractionalScroll = 0.0;

    // Sürükle bırak durumu
    private boolean isDragging = false;
    private long lastDragEndTime = 0; // Sürükleme sonrası tıklamaları reddetmek için zaman damgası

    // Air Mouse Durumu
    private boolean amModeEnabled = false;
    private double amSensitivity = 1.0;
    private static final double AM_MOVEMENT_SCALE = 30.0;
    private static final double AM_SCROLL_SCALE = 20.0;

    // IMU Filtreleme için low-pass filter katsayıları (0-1 arası, düşük = daha pürüzsüz)
    private static final double AM_FILTER_ALPHA = 0.25;

    // Filtrelenmiş IMU değerleri (smoothed)
    private double filteredX = 0.0;
    private double filteredY = 0.0;
    private double filteredZ = 0.0;

    // Dead zone eşikleri (0.05 = ~5% hayalet hareketi engeller)
    private static final double AM_DEADZONE_XY = 0.08;
    private static final double AM_DEADZONE_Z = 0.05;

    // Minimum hareket eşiği (gerçek hareket için)
    private static final double AM_MIN_THRESHOLD = 0.15;

    // Tıklama geciktirici (Debounce/Anti-Double Click)
    private Timer clickTimer = new Timer();
    private TimerTask pendingClick = null;

    private boolean running = true;

    public RobotMouseHandler(AppConfig appConfig) {
        this.appConfig = appConfig;

        // Wayland kontrolü - Kullanıcıyı bilgilendirmek için
        String sessionType = System.getenv("XDG_SESSION_TYPE");
        if ("wayland".equalsIgnoreCase(sessionType)) {
            System.err.println(
                    "UYARI: Linux sisteminiz Wayland kullanıyor. java.awt.Robot sınıfı Wayland üzerinde güvenlik nedeniyle fare/klavye simülasyonu yapamaz.");
            System.err.println(
                    "Çözüm: Oturum açarken 'Ubuntu on Xorg' (X11) seçin veya ydotool gibi alternatif bir araç kullanın.");
        }

        try {
            this.robot = new Robot();
            startMotionEngine();
        } catch (AWTException e) {
            System.err.println("Failed to initialize Robot: " + e.getMessage());
        }
    }

    private void startMotionEngine() {
        Thread engine = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(8); // ~120-125 Hz yenileme hızı
                    updateMotion();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        engine.setDaemon(true);
        engine.start();
    }

    private synchronized void updateMotion() {
        if (robot == null)
            return;

        // Fare Hareketini İşleme (Smoothing)
        if (Math.abs(vx) > 0.01 || Math.abs(vy) > 0.01) {
            double totalX = vx + fractionalX;
            double totalY = vy + fractionalY;

            int moveX = (int) totalX;
            int moveY = (int) totalY;

            fractionalX = totalX - moveX;
            fractionalY = totalY - moveY;

            if (moveX != 0 || moveY != 0) {
                PointerInfo pi = MouseInfo.getPointerInfo();
                if (pi != null) {
                    Point currentPos = pi.getLocation();
                    robot.mouseMove(currentPos.x + moveX, currentPos.y + moveY);
                }
            }

            // Sürtünme (Friction) ile hızı yavaşça sönümle. Donanım hissini veren kısım.
            vx *= 0.6;
            vy *= 0.6;
        } else {
            vx = 0;
            vy = 0;
        }

        // Scroll (Tekerlek) İşleme
        if (Math.abs(scrollV) > 0.01) {
            double totalScroll = scrollV + fractionalScroll;
            int moveScroll = (int) totalScroll;
            fractionalScroll = totalScroll - moveScroll;

            if (moveScroll != 0) {
                robot.mouseWheel(moveScroll);
            }

            // Scroll Sürtünmesi
            scrollV *= 0.80; // yumuşakça dursun
        } else {
            scrollV = 0;
        }
    }

    // Gelen veriler artık anında oynatmak yerine imlece "ivme" (velocity) veriyor
    public synchronized void handleMove(MouseMoveEvent event) {
        double multiplier = appConfig.getPointerSpeed();
        vx += event.getX() * multiplier;
        vy += event.getY() * multiplier;
    }

    public synchronized void handleClick(MouseClickEvent event) {
        // Eğer drag işlemi yeni bittiyse (son 300ms içinde), bu tıklamayı yoksay (false
        // alarm koruması)
        if (System.currentTimeMillis() - lastDragEndTime < 400) {
            return;
        }

        // Eğer önceden bir tıklama beklemedeyse iptal et! (Debouncing)
        if (pendingClick != null) {
            pendingClick.cancel();
        }

        pendingClick = new TimerTask() {
            @Override
            public void run() {
                executeClick(event.getButton());
            }
        };
        // Tıklamaları 150ms geciktir. Eğer bu süre zarfında DRAG gelirse bu Task iptal
        // olur!
        clickTimer.schedule(pendingClick, 150);
    }

    private void executeClick(int buttonParam) {
        if (robot == null)
            return;

        // Task tetiklendiğinde bile, (örneğin zamanlayıcı içindeyken) drag az önce
        // bittiyse engelle
        if (System.currentTimeMillis() - lastDragEndTime < 500) {
            return;
        }

        int buttonMask = (buttonParam == 1) ? InputEvent.BUTTON3_DOWN_MASK : InputEvent.BUTTON1_DOWN_MASK;
        robot.mousePress(buttonMask);
        robot.mouseRelease(buttonMask);
    }

    public synchronized void handleDoubleClick(MouseDoubleClickEvent event) {
        if (System.currentTimeMillis() - lastDragEndTime < 400) {
            return;
        }

        if (pendingClick != null) {
            pendingClick.cancel();
            pendingClick = null;
        }

        if (robot == null)
            return;

        int buttonMask = InputEvent.BUTTON1_DOWN_MASK;
        robot.mousePress(buttonMask);
        robot.mouseRelease(buttonMask);
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        robot.mousePress(buttonMask);
        robot.mouseRelease(buttonMask);
    }

    public synchronized void handleScroll(ScrollEvent event) {
        scrollV += (event.getY() * appConfig.getScrollSpeed());
    }

    public synchronized void handleDragStart(MouseDragStartEvent event) {
        // DRAG_START gelirse, kazayla algılanan beklemedeki C tıklaması iptal edilsin!
        if (pendingClick != null) {
            pendingClick.cancel();
            pendingClick = null;
        }

        if (!isDragging) {
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            isDragging = true;
        }
    }

    public synchronized void handleDrag(MouseDragEvent event) {
        if (!isDragging) {
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            isDragging = true;
        }

        double multiplier = 1.4;
        vx += event.getX() * multiplier;
        vy += event.getY() * multiplier;
    }

    public synchronized void handleDragEnd(MouseDragEndEvent event) {
        if (isDragging) {
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            isDragging = false;
            lastDragEndTime = System.currentTimeMillis();
        }
    }

    public synchronized void handleSwipe3(Swipe3Event event) {
        if (robot == null)
            return;
        String dir = event.getDir();
        if (dir == null)
            return;

        String os = System.getProperty("os.name").toLowerCase();

        try {
            if (os.contains("win")) {
                handleWindowsSwipe(dir);
            } else if (os.contains("mac")) {
                handleMacSwipe(dir);
            } else {
                handleLinuxSwipe(dir);
            }
        } catch (Exception e) {
            System.err.println("Failed to execute swipe3: " + e.getMessage());
        }
    }

    private void handleWindowsSwipe(String dir) {
        switch (dir) {
            case "LEFT":
                robot.keyPress(KeyEvent.VK_CONTROL);
                robot.keyPress(KeyEvent.VK_WINDOWS);
                robot.keyPress(KeyEvent.VK_RIGHT);
                robot.keyRelease(KeyEvent.VK_RIGHT);
                robot.keyRelease(KeyEvent.VK_WINDOWS);
                robot.keyRelease(KeyEvent.VK_CONTROL);
                break;
            case "RIGHT":
                robot.keyPress(KeyEvent.VK_CONTROL);
                robot.keyPress(KeyEvent.VK_WINDOWS);
                robot.keyPress(KeyEvent.VK_LEFT);
                robot.keyRelease(KeyEvent.VK_LEFT);
                robot.keyRelease(KeyEvent.VK_WINDOWS);
                robot.keyRelease(KeyEvent.VK_CONTROL);
                break;
            case "UP":
                robot.keyPress(KeyEvent.VK_WINDOWS);
                robot.keyPress(KeyEvent.VK_TAB);
                robot.keyRelease(KeyEvent.VK_TAB);
                robot.keyRelease(KeyEvent.VK_WINDOWS);
                break;
            case "DOWN":
                robot.keyPress(KeyEvent.VK_WINDOWS);
                robot.keyPress(KeyEvent.VK_D);
                robot.keyRelease(KeyEvent.VK_D);
                robot.keyRelease(KeyEvent.VK_WINDOWS);
                break;
        }
    }

    private void handleMacSwipe(String dir) {
        switch (dir) {
            case "LEFT":
                robot.keyPress(KeyEvent.VK_CONTROL);
                robot.keyPress(KeyEvent.VK_RIGHT);
                robot.keyRelease(KeyEvent.VK_RIGHT);
                robot.keyRelease(KeyEvent.VK_CONTROL);
                break;
            case "RIGHT":
                robot.keyPress(KeyEvent.VK_CONTROL);
                robot.keyPress(KeyEvent.VK_LEFT);
                robot.keyRelease(KeyEvent.VK_LEFT);
                robot.keyRelease(KeyEvent.VK_CONTROL);
                break;
            case "UP":
                robot.keyPress(KeyEvent.VK_CONTROL);
                robot.keyPress(KeyEvent.VK_UP);
                robot.keyRelease(KeyEvent.VK_UP);
                robot.keyRelease(KeyEvent.VK_CONTROL);
                break;
            case "DOWN":
                robot.keyPress(KeyEvent.VK_F11);
                robot.keyRelease(KeyEvent.VK_F11);
                break;
        }
    }

    private void handleLinuxSwipe(String dir) {
        switch (dir) {
            case "LEFT":
                robot.keyPress(KeyEvent.VK_CONTROL);
                robot.keyPress(KeyEvent.VK_ALT);
                robot.keyPress(KeyEvent.VK_RIGHT);
                robot.keyRelease(KeyEvent.VK_RIGHT);
                robot.keyRelease(KeyEvent.VK_ALT);
                robot.keyRelease(KeyEvent.VK_CONTROL);
                break;
            case "RIGHT":
                robot.keyPress(KeyEvent.VK_CONTROL);
                robot.keyPress(KeyEvent.VK_ALT);
                robot.keyPress(KeyEvent.VK_LEFT);
                robot.keyRelease(KeyEvent.VK_LEFT);
                robot.keyRelease(KeyEvent.VK_ALT);
                robot.keyRelease(KeyEvent.VK_CONTROL);
                break;
            case "UP":
                try {
                    Runtime.getRuntime().exec("xdotool key Super_L");
                } catch (Exception e) {
                    System.err.println("Failed to execute xdotool: " + e.getMessage());
                }
                break;
            case "DOWN":
                robot.keyPress(KeyEvent.VK_CONTROL);
                robot.keyPress(KeyEvent.VK_ALT);
                robot.keyPress(KeyEvent.VK_D);
                robot.keyRelease(KeyEvent.VK_D);
                robot.keyRelease(KeyEvent.VK_ALT);
                robot.keyRelease(KeyEvent.VK_CONTROL);
                break;
        }
    }

    public synchronized void handleTap4(Tap4Event event) {
        if (robot == null)
            return;

        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("mac")) {
                robot.keyPress(KeyEvent.VK_META);
                robot.keyPress(KeyEvent.VK_V);
                robot.keyRelease(KeyEvent.VK_V);
                robot.keyRelease(KeyEvent.VK_META);
            } else {
                robot.keyPress(KeyEvent.VK_CONTROL);
                robot.keyPress(KeyEvent.VK_V);
                robot.keyRelease(KeyEvent.VK_V);
                robot.keyRelease(KeyEvent.VK_CONTROL);
            }
        } catch (Exception e) {
            System.err.println("Failed to execute tap4: " + e.getMessage());
        }
    }

    public synchronized void handleDictation(DictationEvent event) {
        if (robot == null || event.getText() == null || event.getText().isEmpty()) {
            return;
        }

        try {
            // Append a space as recommended
            String textToType = event.getText() + " ";

            // Set the clipboard contents
            StringSelection stringSelection = new StringSelection(textToType);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, stringSelection);

            String os = System.getProperty("os.name").toLowerCase();

            // Perform paste operation
            if (os.contains("mac")) {
                robot.keyPress(KeyEvent.VK_META);
                robot.keyPress(KeyEvent.VK_V);
                robot.keyRelease(KeyEvent.VK_V);
                robot.keyRelease(KeyEvent.VK_META);
            } else {
                robot.keyPress(KeyEvent.VK_CONTROL);
                robot.keyPress(KeyEvent.VK_V);
                robot.keyRelease(KeyEvent.VK_V);
                robot.keyRelease(KeyEvent.VK_CONTROL);
            }

            // Small delay to allow the OS to process the paste command
            Thread.sleep(50);

        } catch (Exception e) {
            System.err.println("Failed to execute dictation: " + e.getMessage());
        }
    }

    public synchronized void handleClipboard(ClipboardEvent event) {
        if (robot == null || event.getText() == null || event.getText().isEmpty()) {
            return;
        }

        try {
            String textToPaste = event.getText();

            // Set the clipboard contents
            StringSelection stringSelection = new StringSelection(textToPaste);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, stringSelection);

            String os = System.getProperty("os.name").toLowerCase();

            // Perform paste operation
            if (os.contains("mac")) {
                robot.keyPress(KeyEvent.VK_META);
                robot.keyPress(KeyEvent.VK_V);
                robot.keyRelease(KeyEvent.VK_V);
                robot.keyRelease(KeyEvent.VK_META);
            } else {
                robot.keyPress(KeyEvent.VK_CONTROL);
                robot.keyPress(KeyEvent.VK_V);
                robot.keyRelease(KeyEvent.VK_V);
                robot.keyRelease(KeyEvent.VK_CONTROL);
            }

            // Small delay to allow the OS to process the paste command
            Thread.sleep(50);

        } catch (Exception e) {
            System.err.println("Failed to execute clipboard paste: " + e.getMessage());
        }
    }

    public synchronized void handleCopy(CopyEvent event) {
        if (robot == null)
            return;
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("mac")) {
                robot.keyPress(KeyEvent.VK_META);
                robot.keyPress(KeyEvent.VK_C);
                robot.keyRelease(KeyEvent.VK_C);
                robot.keyRelease(KeyEvent.VK_META);
            } else {
                robot.keyPress(KeyEvent.VK_CONTROL);
                robot.keyPress(KeyEvent.VK_C);
                robot.keyRelease(KeyEvent.VK_C);
                robot.keyRelease(KeyEvent.VK_CONTROL);
            }
        } catch (Exception e) {
            System.err.println("Failed to execute copy: " + e.getMessage());
        }
    }

    public synchronized void handlePaste(PasteEvent event) {
        if (robot == null)
            return;
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("mac")) {
                robot.keyPress(KeyEvent.VK_META);
                robot.keyPress(KeyEvent.VK_V);
                robot.keyRelease(KeyEvent.VK_V);
                robot.keyRelease(KeyEvent.VK_META);
            } else {
                robot.keyPress(KeyEvent.VK_CONTROL);
                robot.keyPress(KeyEvent.VK_V);
                robot.keyRelease(KeyEvent.VK_V);
                robot.keyRelease(KeyEvent.VK_CONTROL);
            }
        } catch (Exception e) {
            System.err.println("Failed to execute paste: " + e.getMessage());
        }
    }

    public synchronized void handleAmMode(AMModeEvent event) {
        this.amModeEnabled = event.isEnabled();
    }

    public synchronized void handleAmSens(AMSensEvent event) {
        this.amSensitivity = event.getValue();
    }

    public synchronized void handleAmMove(AMMoveEvent event) {
        if (!amModeEnabled || robot == null)
            return;

        double rawX = event.getX();
        double rawY = event.getY();
        double rawZ = event.getZ();

        filteredX = filteredX + AM_FILTER_ALPHA * (rawX - filteredX);
        filteredY = filteredY + AM_FILTER_ALPHA * (rawY - filteredY);
        filteredZ = filteredZ + AM_FILTER_ALPHA * (rawZ - filteredZ);

        double appliedX = applyDeadzone(filteredX, AM_DEADZONE_XY);
        double appliedY = applyDeadzone(filteredY, AM_DEADZONE_XY);
        double appliedZ = applyDeadzone(filteredZ, AM_DEADZONE_Z);

        double magnitude = Math.sqrt(appliedX * appliedX + appliedY * appliedY);
        if (magnitude < AM_MIN_THRESHOLD) {
            return;
        }

        double basePointerSpeed = appConfig.getPointerSpeed();

        vx += appliedX * amSensitivity * AM_MOVEMENT_SCALE * basePointerSpeed;
        vy += appliedY * amSensitivity * AM_MOVEMENT_SCALE * basePointerSpeed;

        if (Math.abs(appliedZ) > 0.01) {
            scrollV += appliedZ * amSensitivity * AM_SCROLL_SCALE * appConfig.getScrollSpeed();
        }
    }

    private double applyDeadzone(double value, double deadzone) {
        if (Math.abs(value) < deadzone) {
            return 0.0;
        }
        double sign = value > 0 ? 1.0 : -1.0;
        return sign * (Math.abs(value) - deadzone) / (1.0 - deadzone);
    }
}
