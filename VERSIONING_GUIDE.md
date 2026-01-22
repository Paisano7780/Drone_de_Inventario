# Dynamic Versioning and Release Guide

## Overview

This project now supports dynamic versioning for Android builds with automated releases via GitHub Actions.

## How It Works

### Local Development

When building locally without specifying a version:
```bash
./gradlew assembleDebug
```
The APK will have `versionName = "DEV-SNAPSHOT"` and a timestamp-based `versionCode`.

To build with a specific version:
```bash
./gradlew assembleDebug -PVERSION_NAME='1.2.3'
```
The APK will have `versionName = "1.2.3"`.

### Automated CI/CD Workflows

#### 1. **Tag-Based Release** (`.github/workflows/android.yml`)

**Trigger:** Push a Git tag starting with `v` (e.g., `v1.0`, `v2.3.1`)

**Process:**
1. Extract version from tag (removes `v` prefix)
2. Build APK with injected version
3. Rename APK to `DroneInventoryScanner-[VERSION].apk`
4. Upload as artifact
5. Create GitHub Release with APK attached

**How to use:**
```bash
# Create and push a version tag
git tag v1.0.0
git push origin v1.0.0
```

This will automatically:
- Build the APK with version `1.0.0`
- Create a GitHub Release titled "DroneInventoryScanner 1.0.0"
- Attach the APK file `DroneInventoryScanner-1.0.0.apk`

#### 2. **Continuous Build** (`.github/workflows/build-apk.yml`)

**Trigger:** Push to `main` branch or pull requests

**Process:**
1. Calculate version from latest Git tag + auto-increment
2. Build APK with dynamic version
3. Run tests
4. Upload artifacts (on main branch builds)
5. Create release (on main branch only)
6. Commit APK to `release/` folder (on main branch only)

#### 3. **Manual Release** (`.github/workflows/release-apk.yml`)

**Trigger:** Manual workflow dispatch with version input

**Process:**
1. Use user-specified version
2. Build and test
3. Upload artifact
4. Create GitHub release

## Version Management

### Version Name
- **Production builds:** Read from `-PVERSION_NAME` parameter
- **Local/dev builds:** Defaults to `"DEV-SNAPSHOT"`
- **Format:** Semantic versioning recommended (e.g., `1.0.0`, `2.1.3`)

### Version Code
- **CI builds:** Uses `GITHUB_RUN_NUMBER` (auto-increments on each workflow run)
- **Local builds:** Uses current timestamp in seconds

## Example Workflow

### Releasing Version 2.0.0

1. **Prepare your code:**
   ```bash
   git checkout main
   git pull origin main
   # Make your changes, test locally
   ./gradlew assembleDebug -PVERSION_NAME='2.0.0'
   ```

2. **Create and push the release tag:**
   ```bash
   git tag v2.0.0
   git push origin v2.0.0
   ```

3. **Monitor the workflow:**
   - Go to: https://github.com/Paisano7780/Drone_de_Inventario/actions
   - Watch the "Android Release Build" workflow
   - Wait for completion (~3-5 minutes)

4. **Verify the release:**
   - Go to: https://github.com/Paisano7780/Drone_de_Inventario/releases
   - You should see "DroneInventoryScanner 2.0.0"
   - Download `DroneInventoryScanner-2.0.0.apk`

## Troubleshooting

### Build fails during workflow
- Check the workflow logs in GitHub Actions
- Common issues:
  - Compilation errors
  - Dependency resolution problems
  - SDK version mismatches

### APK not found error
- The workflow verifies the APK exists after build
- If this fails, check the Gradle build output for errors

### Version not injected correctly
- Verify you're using the `-PVERSION_NAME` parameter
- Check that the property is passed correctly in the workflow

## Migration Notes

- **Old system:** Hardcoded version in `build.gradle.kts`
- **New system:** Dynamic versioning via project properties
- **Breaking change:** None - backward compatible with local builds
- **Benefits:**
  - Single source of truth for versions (Git tags)
  - Automated release process
  - No manual version bumps in code
  - Build artifacts include version in filename

## Files Modified

1. **`app/build.gradle.kts`**
   - Added dynamic `versionName` from project property
   - Added dynamic `versionCode` from environment or timestamp

2. **`.github/workflows/android.yml`** (NEW)
   - Tag-based release workflow
   - Automated GitHub Release creation

3. **`.github/workflows/build-apk.yml`** (UPDATED)
   - Now uses dynamic versioning
   - Passes VERSION_NAME to Gradle build

4. **`.github/workflows/release-apk.yml`** (EXISTING)
   - Manual release workflow
   - Available if needed

## Best Practices

1. **Use semantic versioning:** `MAJOR.MINOR.PATCH` (e.g., `1.0.0`, `2.1.3`)
2. **Tag production releases:** Always use tags for production builds
3. **Test before tagging:** Ensure your code is tested and working
4. **Don't delete tags:** Git tags should be permanent for version history
5. **Document releases:** Add release notes in GitHub Releases after workflow completes

## Future Enhancements

Possible improvements:
- Add automatic changelog generation from commit messages
- Support beta/RC versions (e.g., `v2.0.0-beta1`)
- Add signed APK support for release builds
- Implement Play Store deployment
