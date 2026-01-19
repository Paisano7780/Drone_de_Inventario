# GitHub Actions Workflows

This directory contains automated workflows for building the DroneInventoryScanner APK.

## Available Workflows

### 1. Build APK (`build-apk.yml`)

**Triggers:**
- Push to `main` branch
- Push to any `copilot/**` branch
- Pull requests to `main`
- Manual trigger via GitHub Actions UI

**What it does:**
1. Runs unit tests (20 tests)
2. Builds debug APK
3. Uploads APK as artifact

**To download the APK:**
1. Go to the **Actions** tab
2. Click on the latest workflow run
3. Scroll down to **Artifacts**
4. Download `app-debug` artifact

### 2. Build Release APK (`release-apk.yml`)

**Triggers:**
- Manual trigger only (workflow_dispatch)

**What it does:**
1. Runs unit tests
2. Builds debug APK
3. Creates a GitHub Release with the APK
4. Tags the release with version number

**To create a release:**
1. Go to **Actions** tab
2. Select "Build Release APK" workflow
3. Click "Run workflow"
4. Enter version name (e.g., `1.0.0`)
5. Click "Run workflow" button

The APK will be:
- Available as an artifact (90 days retention)
- Published in the Releases section
- Named: `DroneInventoryScanner-v{version}-debug.apk`

## Workflow Details

### Build Environment
- **OS:** Ubuntu Latest
- **Java:** JDK 17 (Temurin)
- **Gradle:** Uses project's wrapper
- **Cache:** Gradle dependencies cached for faster builds

### Build Steps
1. **Checkout:** Fetches repository code
2. **Setup Java:** Installs JDK 17
3. **Test:** Runs unit tests via `build-simple.gradle.kts`
4. **Build:** Compiles APK via `./gradlew assembleDebug`
5. **Upload:** Saves APK as artifact

### Output
- **APK Location:** `app/build/outputs/apk/debug/app-debug.apk`
- **APK Size:** ~2-5 MB (typical for this app)

## Manual Build (Local)

If you prefer to build locally instead of using workflows:

```bash
# Clone repository
git clone https://github.com/Paisano7780/Drone_de_Inventario.git
cd Drone_de_Inventario

# Build APK
./gradlew assembleDebug

# APK will be at: app/build/outputs/apk/debug/app-debug.apk
```

## Troubleshooting

### Workflow fails with "SDK not found"
- This shouldn't happen as the workflow sets up Android SDK automatically
- If it does, check the Java version and Gradle cache

### APK not uploading
- Check the build logs for errors in the "Build Debug APK" step
- Verify the APK was created in the expected path

### Tests failing
- Review test output in the "Run unit tests" step
- All 20 unit tests must pass before APK is built

## Notes

- **Signing:** Current builds are debug APKs (unsigned)
- **Size:** Debug APKs are larger than release builds
- **ProGuard:** Not enabled for debug builds
- **Permissions:** Workflows use `GITHUB_TOKEN` (automatic)

For production releases with signing, you would need to:
1. Create a keystore
2. Add keystore as GitHub secret
3. Update workflow to use release build variant
4. Configure signing in `app/build.gradle.kts`

See `BUILD_INSTRUCTIONS.md` for detailed signing instructions.
