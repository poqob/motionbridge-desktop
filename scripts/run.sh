#!/bin/bash

# Projeyi kök dizininde çalıştırmak için dizin kontrolü
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$DIR/.."

echo "Projeyi derliyoruz (MotionBridge)..."
mvn clean compile

if [ $? -eq 0 ]; then
    echo "Derleme başarılı! Uygulama başlatılıyor..."
    mvn javafx:run
else
    echo "Derleme başarısız oldu. Lütfen yukarıdaki hataları kontrol edin."
    exit 1
fi

cd "$(dirname "$0")/.." && mvn javafx:run 