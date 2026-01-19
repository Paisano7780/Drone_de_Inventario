# Build Instructions for DroneInventoryScanner

This document provides detailed instructions for building the DroneInventoryScanner Android application.

## Prerequisites

### Required Software
1. **Android Studio** (Arctic Fox or later)
   - Download from: https://developer.android.com/studio
   
2. **Java Development Kit (JDK)** 11 or later
   - Check your version: `java -version`
   - Download from: https://adoptium.net/

3. **Android SDK**
   - SDK Platform: Android 13 (API 33)
   - Build Tools: 33.0.0 or later
   - Android SDK Platform-Tools

### System Requirements
- **OS**: Windows 10+, macOS 10.14+, or Linux
- **RAM**: Minimum 8 GB (16 GB recommended)
- **Disk Space**: At least 10 GB free space

## Setup Instructions

### 1. Install Android Studio

1. Download Android Studio from the official website
2. Run the installer and follow the setup wizard
3. During setup, ensure you install:
   - Android SDK
   - Android SDK Platform
   - Android Virtual Device (for emulator testing)

### 2. Configure SDK

1. Open Android Studio
2. Go to **File → Settings** (or **Android Studio → Preferences** on Mac)
3. Navigate to **Appearance & Behavior → System Settings → Android SDK**
4. Install the following:
   - SDK Platforms: Android 13.0 (API 33)
   - SDK Tools:
     - Android SDK Build-Tools 33.0.0
     - Android SDK Platform-Tools
     - Android SDK Tools
     - Google Play services

### 3. Clone and Open Project

```bash
# Clone the repository
git clone https://github.com/Paisano7780/Drone_de_Inventario.git

# Navigate to project directory
cd Drone_de_Inventario

# Open in Android Studio
# File → Open → Select the Drone_de_Inventario folder
```

### 4. Gradle Sync

1. Android Studio will automatically start syncing Gradle
2. Wait for the sync to complete (may take several minutes on first run)
3. If sync fails, click **File → Sync Project with Gradle Files**

## Building the Application

### Option 1: Build via Android Studio (Recommended)

#### Debug Build
1. Select **Build → Build Bundle(s) / APK(s) → Build APK(s)**
2. Wait for the build to complete
3. Click **locate** in the notification to find the APK
4. APK location: `app/build/outputs/apk/debug/app-debug.apk`

#### Release Build
1. Create a keystore (if you don't have one):
   - Go to **Build → Generate Signed Bundle / APK**
   - Select **APK** and click **Next**
   - Click **Create new...** under Key store path
   - Fill in the keystore information
   - Save the keystore file securely

2. Build signed APK:
   - Select **Build → Generate Signed Bundle / APK**
   - Select **APK** and click **Next**
   - Choose your keystore and enter credentials
   - Select **release** build variant
   - Click **Finish**

3. Find your APK at: `app/build/outputs/apk/release/app-release.apk`

### Option 2: Build via Command Line

#### Debug Build
```bash
# Make gradlew executable (Linux/Mac)
chmod +x gradlew

# Build debug APK
./gradlew assembleDebug

# On Windows:
# gradlew.bat assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

#### Release Build
First, create a keystore:
```bash
keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-alias
```

Create `keystore.properties` in project root:
```properties
storePassword=YOUR_KEYSTORE_PASSWORD
keyPassword=YOUR_KEY_PASSWORD
keyAlias=my-alias
storeFile=/path/to/my-release-key.jks
```

Update `app/build.gradle.kts` to reference the keystore:
```kotlin
signingConfigs {
    create("release") {
        val keystorePropertiesFile = rootProject.file("keystore.properties")
        val keystoreProperties = Properties()
        keystoreProperties.load(FileInputStream(keystorePropertiesFile))
        
        keyAlias = keystoreProperties["keyAlias"] as String
        keyPassword = keystoreProperties["keyPassword"] as String
        storeFile = file(keystoreProperties["storeFile"] as String)
        storePassword = keystoreProperties["storePassword"] as String
    }
}

buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
        // ... other config
    }
}
```

Then build:
```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

## Running Tests

### Unit Tests
```bash
# Run unit tests using the simplified build
./gradlew -b build-simple.gradle.kts test

# View results
cat build/test-results/test/TEST-*.xml
```

### Instrumented Tests (Requires Android Device/Emulator)
```bash
# Connect device via USB or start emulator
adb devices

# Run instrumented tests
./gradlew connectedAndroidTest
```

## Installing the APK

### Via Android Studio
1. Connect your Android device via USB
2. Enable **Developer Options** and **USB Debugging** on the device
3. Click **Run → Run 'app'** (or press Shift+F10)

### Via Command Line
```bash
# Check connected devices
adb devices

# Install APK
adb install app/build/outputs/apk/debug/app-debug.apk

# Or for release:
adb install app/build/outputs/apk/release/app-release.apk
```

## Troubleshooting

### Gradle Sync Fails
- Check internet connection (needs to download dependencies)
- Try **File → Invalidate Caches / Restart**
- Delete `.gradle` folder and re-sync

### Build Fails - Android SDK Not Found
```bash
# Set ANDROID_HOME environment variable
export ANDROID_HOME=/path/to/Android/Sdk

# Add to ~/.bashrc or ~/.zshrc for persistence
```

### Build Fails - Network Issues
If you're behind a proxy, configure in `gradle.properties`:
```properties
systemProp.http.proxyHost=proxy.company.com
systemProp.http.proxyPort=8080
systemProp.https.proxyHost=proxy.company.com
systemProp.https.proxyPort=8080
```

### Bluetooth Permissions Error
- Ensure all permissions are declared in `AndroidManifest.xml`
- For Android 12+, runtime permissions are required
- Grant permissions manually in device settings if needed

### TTS Not Working
- Ensure device has TTS engine installed
- Go to **Settings → Accessibility → Text-to-speech output**
- Install Google Text-to-Speech if not available

## Verification

After building, verify the APK:

### Check APK Contents
```bash
# List contents
unzip -l app/build/outputs/apk/debug/app-debug.apk

# Check signatures (for release builds)
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk
```

### Test Installation
1. Install APK on test device
2. Grant all required permissions
3. Pair a Bluetooth scanner
4. Test connection and scanning
5. Verify CSV export

## Build Variants

The project supports two build variants:

1. **Debug**
   - Debuggable
   - Not obfuscated
   - Faster build
   - For development and testing

2. **Release**
   - Optimized
   - ProGuard enabled (optional)
   - Signed with release key
   - For production deployment

## CI/CD Notes

For automated builds:
```yaml
# Example GitHub Actions workflow
- name: Build APK
  run: ./gradlew assembleDebug
  
- name: Upload APK
  uses: actions/upload-artifact@v2
  with:
    name: app-debug
    path: app/build/outputs/apk/debug/app-debug.apk
```

## Additional Resources

- [Android Developer Guide](https://developer.android.com/guide)
- [Gradle Build Guide](https://developer.android.com/studio/build)
- [Bluetooth Developer Guide](https://developer.android.com/guide/topics/connectivity/bluetooth)

## Support

For build issues, check:
1. Android Studio's Build Output panel
2. Gradle Console
3. `app/build/outputs/logs/` for detailed logs

---

**Last Updated**: January 2026
**Version**: 1.0
