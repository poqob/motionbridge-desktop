#!/bin/bash

echo "MotionBridge Linux Dependencies Installer"
echo "----------------------------------------"

# Check if the system is Debian/Ubuntu based
if [ -x "$(command -v apt-get)" ]; then
    echo "Installing brightnessctl via apt..."
    sudo apt-get update
    sudo apt-get install -y brightnessctl
# Check if the system is Fedora based
elif [ -x "$(command -v dnf)" ]; then
    echo "Installing brightnessctl via dnf..."
    sudo dnf install -y brightnessctl
# Check if the system is Arch based
elif [ -x "$(command -v pacman)" ]; then
    echo "Installing brightnessctl via pacman..."
    sudo pacman -S --noconfirm brightnessctl
else
    echo "Unsupported package manager. Please install 'brightnessctl' manually."
    exit 1
fi

echo "----------------------------------------"
echo "Installation complete!"
echo "Note: You might need to add your user to the 'video' group to allow brightness control without sudo."
echo "Command: sudo usermod -aG video \$USER"
echo "After adding to the group, you need to log out and log back in."
