# MotionBridge

## Requirements

### Linux Dependencies
To control screen brightness on Linux, the application relies on the `brightnessctl` utility. This utility interacts directly with the system's backlight files and needs to be installed on the machine running this software.

To easily set up Linux dependencies, run the provided script located in the `scripts/` directory:
```bash
bash scripts/install_linux_dependencies.sh
```
This script will detect your package manager (APT, DNF, Pacman) and attempt to install `brightnessctl`. 

**Note for Linux users:** You might need to add your user to the `video` group to prevent permission issues when changing brightness. Run `sudo usermod -aG video $USER` and log back in, or rely on `brightnessctl` setup that commonly adds udev rules handling this.
