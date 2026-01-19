# Testing Guide for DroneInventoryScanner

This document describes the testing strategy and how to run tests for the DroneInventoryScanner application.

## Testing Architecture

The project follows a comprehensive testing strategy across three levels:

1. **Unit Tests** - Test individual components in isolation
2. **Instrumented Tests** - Test Android-specific components
3. **Manual Testing** - Test end-to-end functionality with real hardware

## Unit Tests (Phase A ✅)

### Test Coverage

#### DataParserTest (10 tests)
Tests the raw input parser that cleans and validates barcode data:

- ✅ `parseRawInput should remove CR and LF characters` - Verifies \r and \n removal
- ✅ `parseRawInput should handle clean strings` - Tests clean input passthrough
- ✅ `parseRawInput should return null for empty strings` - Edge case handling
- ✅ `parseRawInput should return null for only control characters` - Invalid input handling
- ✅ `parseRawInput should trim whitespace` - Whitespace handling
- ✅ `parseRawInput should handle complex mixed input` - Complex scenarios
- ✅ `isValidCode should return true for alphanumeric codes` - Valid code detection
- ✅ `isValidCode should return false for empty string` - Empty validation
- ✅ `isValidCode should return false for control characters` - Control char detection
- ✅ `isValidCode should allow spaces` - Space character handling

#### ScanRepositoryTest (10 tests)
Tests the scan data repository and duplicate detection:

- ✅ `addScan should add new scan successfully` - Add operation
- ✅ `addScan should detect duplicates` - Duplicate prevention
- ✅ `addScan should allow different codes` - Multiple unique scans
- ✅ `isDuplicate should detect existing codes` - Duplicate checking
- ✅ `getLastScan should return most recent scan` - Last scan retrieval
- ✅ `getLastScan should return null when empty` - Empty state handling
- ✅ `getAllScans should return all records in order` - Full data retrieval
- ✅ `clearAll should remove all scans` - Clear operation
- ✅ `exportToCsv should create valid CSV file` - CSV export validation
- ✅ `scan timestamps should be in order` - Timestamp ordering

### Running Unit Tests

```bash
# Run all unit tests
./gradlew -b build-simple.gradle.kts test

# Run with output
./gradlew -b build-simple.gradle.kts test --info

# Run specific test class
./gradlew -b build-simple.gradle.kts test --tests "DataParserTest"

# View results
cat build/test-results/test/TEST-*.xml
```

### Test Results

All 20 unit tests pass successfully:
```
DataParserTest: 10/10 passed
ScanRepositoryTest: 10/10 passed
Total: 20/20 passed ✅
```

## Instrumented Tests (Phase B)

### Test Coverage

#### ScannerServiceTest (3 tests)
Tests the foreground service and integration:

- `testServiceBind` - Verifies service can be bound
- `testRepositoryOperations` - Tests repository through service
- `testServiceListener` - Validates listener interface

### Running Instrumented Tests

**Prerequisites:**
- Android device or emulator
- USB debugging enabled
- App installed on device

```bash
# Check connected devices
adb devices

# Run all instrumented tests
./gradlew connectedAndroidTest

# Run specific test
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.paisano.droneinventoryscanner.service.ScannerServiceTest

# View results
cat app/build/outputs/androidTest-results/connected/*.xml
```

## Manual Testing Checklist

### Setup Tests
- [ ] App installs successfully
- [ ] All permissions requested on first launch
- [ ] Bluetooth permission granted
- [ ] Notification permission granted (Android 13+)

### Connection Tests
- [ ] Bluetooth scanner appears in device list
- [ ] Can connect to paired scanner
- [ ] Connection status updates in UI
- [ ] "Connected" status shows green
- [ ] "Disconnected" status shows red
- [ ] TTS speaks "Correct" on connection

### Scanning Tests
- [ ] Scanner receives barcode input
- [ ] Barcode appears in "Last Scanned Code" field
- [ ] TTS speaks last 3 digits of code
- [ ] Duplicate scan detected
- [ ] TTS speaks "Duplicate" for duplicates
- [ ] Different barcodes all recorded

### Background Operation Tests
- [ ] Service shows persistent notification
- [ ] Switch to DJI Fly app
- [ ] Scan barcodes while DJI Fly is foreground
- [ ] Return to scanner app, scans are recorded
- [ ] Service remains active after screen off
- [ ] Scans continue with screen off

### Reconnection Tests
- [ ] Turn off scanner
- [ ] TTS speaks "Scanner Disconnected"
- [ ] Turn on scanner
- [ ] App auto-reconnects (within 25 seconds)
- [ ] TTS speaks "Correct" on reconnection
- [ ] Continue scanning after reconnection

### Export Tests
- [ ] Tap "Export CSV" button
- [ ] Success toast appears
- [ ] File created in Documents/DroneScans
- [ ] CSV file has correct headers
- [ ] All scans present in CSV
- [ ] Timestamps formatted correctly
- [ ] Export with no scans shows "No data" message

### Edge Cases
- [ ] Rapid successive scans
- [ ] Very long barcode strings
- [ ] Special characters in barcodes
- [ ] Empty or whitespace-only scans ignored
- [ ] Connection during active scan
- [ ] Multiple app restarts

## Test Data Examples

### Valid Barcodes
```
12345
ABC123
PRODUCT-CODE-2024
9876543210123
TEST_ITEM_001
```

### Invalid Inputs (Should be rejected)
```
(empty string)
\r\n
    (only spaces)
\x00\x01 (control characters)
```

### Expected CSV Output Format
```csv
Timestamp,Code
"2026-01-19 18:30:45","12345"
"2026-01-19 18:31:02","ABC123"
"2026-01-19 18:31:15","PRODUCT-CODE-2024"
```

## Performance Testing

### Metrics to Monitor
- Connection time: Should be < 5 seconds
- Scan processing: Should be instant (< 100ms)
- Auto-reconnect time: Up to 25 seconds (5 attempts × 5 seconds)
- Memory usage: Should remain stable during prolonged use
- CSV export: Should complete in < 2 seconds for 1000 scans

### Stress Testing
```bash
# Simulate 100 scans
for i in {1..100}; do
    echo "Scan $i: CODE$i"
    # Scan barcode
    sleep 0.5
done

# Check memory usage
adb shell dumpsys meminfo com.paisano.droneinventoryscanner
```

## Continuous Integration

### GitHub Actions Example
```yaml
name: Run Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 11
        uses: actions/setup-java@v2
        with:
          java-version: '11'
      - name: Run unit tests
        run: ./gradlew -b build-simple.gradle.kts test
      - name: Upload test results
        uses: actions/upload-artifact@v2
        with:
          name: test-results
          path: build/test-results/
```

## Test Environment

### Recommended Test Devices
- Xiaomi Redmi Note 13 Pro (primary target)
- Samsung Galaxy S21+ (Android 13)
- Google Pixel 6 (Android 13)
- OnePlus 9 (Android 12)

### Test Scanners
- Eyoyo Ring Scanner (SPP mode)
- Generic Bluetooth barcode scanners
- Any scanner supporting SPP UUID: 00001101-0000-1000-8000-00805F9B34FB

## Debugging Tests

### Enable verbose logging
```bash
adb logcat | grep -E "DroneInventory|ScannerService|BluetoothSpp"
```

### Common Issues

**Tests fail to connect to service:**
- Ensure service is declared in AndroidManifest.xml
- Check that permissions are granted
- Verify Bluetooth is enabled

**TTS not speaking:**
- Install Google TTS on test device
- Check device volume is not muted
- Verify TTS initialized in logs

**CSV export fails:**
- Check storage permissions
- Ensure Documents directory exists
- Verify available storage space

## Coverage Goals

- Unit test coverage: 90%+ for logic layer ✅
- Integration test coverage: 80%+ for service layer
- Manual test coverage: 100% for critical paths

## Test Status Summary

| Test Suite | Tests | Passed | Failed | Status |
|------------|-------|--------|--------|--------|
| DataParserTest | 10 | 10 | 0 | ✅ PASS |
| ScanRepositoryTest | 10 | 10 | 0 | ✅ PASS |
| ScannerServiceTest | 3 | - | - | ⏳ PENDING |
| Manual Tests | 30+ | - | - | ⏳ PENDING |

---

**Note**: Instrumented and manual tests require Android device and cannot be run in the current CI environment without Android SDK setup.

**Last Updated**: January 2026
