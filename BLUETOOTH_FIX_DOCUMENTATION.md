# Bluetooth Connection Fix and Dependency Injection Architecture

## Overview
This document describes the fixes applied to resolve Bluetooth SPP connection issues and the architectural refactoring to enable automated UI testing.

## Issues Addressed

### Original Problem
The user reported persistent Bluetooth connection and disconnection issues with a JR Model HC-Z38W scanner on a Xiaomi Redmi Note 13 Pro. The scanner worked fine with other Bluetooth terminal apps, indicating the issue was in the application code.

### Root Causes Identified
1. **Connection State Management**: The disconnect callback was being called even when not connected, causing state confusion
2. **Rapid Reconnection Attempts**: Missing cooldown period between connection attempts could overwhelm the Bluetooth stack
3. **Socket Cleanup**: Incomplete socket cleanup could leave resources in an inconsistent state
4. **Discovery Interference**: Bluetooth discovery was not always cancelled before connection attempts
5. **No Connection Validation**: Missing checks for adapter state before connection

## Fixes Implemented

### 1. Connection State Management (`BluetoothSppManager.kt`)

**Added Volatile Flags**:
```kotlin
@Volatile private var isConnected = false
@Volatile private var isReading = false
private var isDisconnecting = false
```
- Made connection flags thread-safe with `@Volatile`
- Added `isDisconnecting` flag to prevent duplicate disconnection callbacks

**Improved Disconnect Logic**:
```kotlin
fun disconnect() {
    if (isDisconnecting) {
        Log.d(TAG, "Disconnection already in progress")
        return
    }
    
    val wasConnected = isConnected
    // ... cleanup code ...
    
    // Only call onDisconnected if we were actually connected
    if (wasConnected) {
        listener?.onDisconnected()
    }
}
```

### 2. Connection Retry with Cooldown
```kotlin
// Check cooldown period to prevent rapid reconnection attempts
val now = System.currentTimeMillis()
if (now - lastConnectionAttempt < RETRY_COOLDOWN_MS) {
    val waitTime = RETRY_COOLDOWN_MS - (now - lastConnectionAttempt)
    Log.d(TAG, "Connection cooldown active, waiting ${waitTime}ms")
    delay(waitTime)
}
```

### 3. Enhanced Discovery Cancellation
```kotlin
// Cancel Discovery (Crucial for bandwidth)
try {
    adapter?.cancelDiscovery()
    // Give the adapter time to fully cancel discovery
    delay(300)
} catch (e: SecurityException) {
    Log.w(TAG, "Could not cancel discovery: ${e.message}")
}
```

### 4. Improved Socket Cleanup
```kotlin
// Close failed socket
try {
    socketToUse?.close()
} catch (e2: IOException) {
    Log.w(TAG, "Error closing failed socket: ${e2.message}")
}
socketToUse = null
```
- Added explicit socket closing after failed connection attempts
- Ensured all resources are cleaned up even on exceptions

### 5. Connection Validation
```kotlin
// Validate adapter is enabled
if (adapter != null && !adapter.isEnabled) {
    Log.e(TAG, "Bluetooth adapter is not enabled")
    withContext(Dispatchers.Main) {
        listener?.onError("Bluetooth is not enabled")
    }
    return@withContext false
}
```

## Architectural Refactoring: Dependency Injection

### Problem
Automated UI tests in GitHub Actions cannot use real Bluetooth hardware, making it impossible to test the scanner functionality.

### Solution: Interface-Based Dependency Injection

#### 1. Created `IScannerManager` Interface
```kotlin
interface IScannerManager {
    interface ConnectionListener {
        fun onConnected()
        fun onDisconnected()
        fun onDataReceived(data: String)
        fun onError(error: String)
    }
    
    fun setConnectionListener(listener: ConnectionListener)
    suspend fun connect(device: BluetoothDevice?, adapter: BluetoothAdapter? = null): Boolean
    suspend fun startReading()
    fun stopReading()
    fun disconnect()
    fun isConnected(): Boolean
    fun getPairedDevices(adapter: BluetoothAdapter): Set<BluetoothDevice>
}
```

#### 2. Made `BluetoothSppManager` Implement Interface
```kotlin
class BluetoothSppManager : IScannerManager {
    // Implementation remains the same, just implements the interface
}
```

#### 3. Added Dependency Injection to `ScannerService`
```kotlin
companion object {
    // For dependency injection during testing
    var scannerManagerFactory: (() -> IScannerManager)? = null
}

private val bluetoothManager: IScannerManager by lazy {
    scannerManagerFactory?.invoke() ?: BluetoothSppManager()
}
```

#### 4. Created `MockScannerManager` for Testing
Located in `app/src/androidTest/`:
```kotlin
class MockScannerManager : IScannerManager {
    suspend fun simulateScan(code: String) = withContext(Dispatchers.Main) {
        if (isConnected) {
            listener?.onDataReceived("$code\r\n")
        }
    }
}
```

#### 5. Created Instrumented UI Tests
`ScannerFlowTest.kt` includes tests for:
- Mock scanner connection
- Simulated scan reception
- UI updates
- Duplicate detection
- CSV export

### Test Architecture

**Production Code**:
```
MainActivity → ScannerService → BluetoothSppManager → Real Bluetooth Hardware
```

**Test Code**:
```
ScannerFlowTest → ScannerService → MockScannerManager → Simulated Scans
```

## Testing

### Unit Tests (`BluetoothSppManagerTest.kt`)
- Connection state management
- Listener callbacks
- Multiple disconnect handling
- SPP UUID validation

### Instrumented Tests (`ScannerFlowTest.kt`)
- Complete scan flow with mock scanner
- Multiple scan handling
- Duplicate detection
- CSV export verification

### Running Tests Locally
```bash
# Unit tests
./gradlew -b build-simple.gradle.kts test

# Instrumented tests (requires emulator or device)
./gradlew connectedAndroidTest
```

## CI/CD Integration

The GitHub Actions workflow (`build-apk.yml`) now:
1. Runs unit tests (including Bluetooth connection tests)
2. Validates connection state management
3. Only builds APK if tests pass

## Benefits

1. **Improved Connection Stability**: Proper state management and cooldown periods
2. **Better Resource Management**: Thorough socket cleanup prevents resource leaks
3. **Testable Architecture**: Interface-based design allows mocking for tests
4. **Automated Testing**: CI/CD validates connection logic before every build
5. **Maintainability**: Clear separation of concerns and dependency injection

## Migration Guide

### For Future Development
When adding new scanner features:

1. Add methods to `IScannerManager` interface
2. Implement in `BluetoothSppManager` for production
3. Implement in `MockScannerManager` for testing
4. Add tests in `ScannerFlowTest.kt`

### For Custom Scanner Implementations
To use a different scanner type:

1. Create a new class implementing `IScannerManager`
2. Inject via `ScannerService.scannerManagerFactory`
3. No changes needed in MainActivity or UI code

## Compatibility

- **Minimum SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Tested on**: Xiaomi Redmi Note 13 Pro
- **Scanner**: JR Model HC-Z38W (SPP mode)

## Related Files

### Core Implementation
- `app/src/main/java/com/paisano/droneinventoryscanner/bluetooth/IScannerManager.kt`
- `app/src/main/java/com/paisano/droneinventoryscanner/bluetooth/BluetoothSppManager.kt`
- `app/src/main/java/com/paisano/droneinventoryscanner/service/ScannerService.kt`

### Testing
- `app/src/test/java/com/paisano/droneinventoryscanner/bluetooth/BluetoothSppManagerTest.kt`
- `app/src/androidTest/java/com/paisano/droneinventoryscanner/bluetooth/MockScannerManager.kt`
- `app/src/androidTest/java/com/paisano/droneinventoryscanner/ui/ScannerFlowTest.kt`

### CI/CD
- `.github/workflows/build-apk.yml`
