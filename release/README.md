# Release APKs

This folder contains the compiled APK files for DroneInventoryScanner.

## Latest Release

The latest APK is automatically built and committed to this folder when code is merged to the `main` branch.

## How it Works

1. When a Pull Request is merged to `main`, the GitHub Actions workflow runs automatically
2. The workflow builds the APK and runs tests
3. If successful, the APK is:
   - Uploaded as a GitHub Actions artifact
   - Published in the GitHub Releases section
   - **Committed to this `release/` folder** for easy access

## Installation

To install the app on your Android device:

1. Download the latest `DroneInventoryScanner-v*.apk` file from this folder
2. Transfer it to your Android device
3. Enable "Install from unknown sources" in your device settings
4. Tap the APK file to install
5. Grant the required permissions (Bluetooth, Notifications)

## Versioning

APK files are named with their version number: `DroneInventoryScanner-vX.Y-debug.apk`

- The version number is automatically incremented with each merge to main
- Example: `DroneInventoryScanner-v1.0-debug.apk`, `DroneInventoryScanner-v1.1-debug.apk`, etc.

## Note

These are **debug builds** (unsigned). For production use, you would need a signed release build.

For more information, see the [WORKFLOW_GUIDE_ES.md](../WORKFLOW_GUIDE_ES.md) file.
