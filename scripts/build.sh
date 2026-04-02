#!/bin/bash

# MotionBridge Çoklu Platform Paketleme Scripti
APP_NAME="MotionBridge"
MAIN_JAR="motionbridge-desktop-1.0.jar"
MAIN_CLASS="com.motionbridge.Launcher"
INPUT_DIR="target"
VERSION="1.0.0"

echo "--- $APP_NAME Paketleme İşlemi Başlatılıyor ---"

# İşletim sistemi tespiti
OS_TYPE="$(uname -s)"

case "${OS_TYPE}" in
    Linux*)
        echo "Tespit Edilen Sistem: Linux (Ubuntu/Debian)"
        # Not: Linux için icon .png olmalı
        jpackage \
          --name "$APP_NAME" \
          --type deb \
          --input "$INPUT_DIR" \
          --main-jar "$MAIN_JAR" \
          --main-class "$MAIN_CLASS" \
          --linux-shortcut \
          --linux-menu-group "Utility" \
          --app-version "$VERSION"
        ;;
        
    Darwin*)
        echo "Tespit Edilen Sistem: macOS"
        # Not: macOS için icon .icns olmalı
        jpackage \
          --name "$APP_NAME" \
          --type dmg \
          --input "$INPUT_DIR" \
          --main-jar "$MAIN_JAR" \
          --main-class "$MAIN_CLASS" \
          --app-version "$VERSION" \
          --mac-package-name "$APP_NAME"
        ;;

    CYGWIN*|MINGW32*|MSYS*|MINGW*)
        echo "Tespit Edilen Sistem: Windows"
        # Not: Windows için icon .ico olmalı ve WiX Toolset kurulu olmalı
        jpackage \
          --name "$APP_NAME" \
          --type exe \
          --win-shortcut \
          --win-menu \
          --input "$INPUT_DIR" \
          --main-jar "$MAIN_JAR" \
          --main-class "$MAIN_CLASS" \
          --app-version "$VERSION"
        ;;

    *)
        echo "Hata: Desteklenmeyen işletim sistemi: ${OS_TYPE}"
        exit 1
        ;;
esac

echo "--- Paketleme Tamamlandı! ---"