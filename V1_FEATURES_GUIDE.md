# Version 1.0 - New Features Guide

## Overview
Version 1.0 introduces professional-grade features including Session Management, Floating UI Overlays, and Full Spanish Localization while maintaining the stable Bluetooth connection logic from v0.15.

## New Features

### 1. Session Setup (Pre-Scanning Configuration)

**What it does:**
Before you can start scanning, the app now requires you to configure the session with:
- **Cliente** (Client name, e.g., "CocaCola")
- **Sector** (Work area, e.g., "Pasillo_A")

**How to use:**
1. On first launch, you'll see the Session Setup screen
2. Enter the Cliente and Sector fields (both required)
3. Tap "Continuar" to proceed to the main scanning screen
4. Your session data is saved and automatically reused on next launch

**Benefits:**
- Automatic file naming with context: `[Cliente]_[Sector]_[Timestamp].csv`
- Better organization of exported scan data
- Clear identification of scan sessions

### 2. Downloads Folder Export

**What changed:**
CSV files are now automatically saved to the public **Downloads** folder instead of Documents/DroneScans.

**File naming:**
- Format: `CocaCola_Pasillo_A_20260122_143055.csv`
- Contains: Cliente, Sector, Date, and Time

**How it works:**
- Android 10+: Uses MediaStore API for proper public storage
- Android 9 and below: Uses direct file access
- Toast message confirms: "Guardado en Descargas: [filename]"

**Finding your files:**
- Open the Files app on your device
- Navigate to Downloads folder
- Look for files starting with your Cliente name

### 3. Floating Overlay System

**What it does:**
Provides visual feedback that appears over other apps (like DJI Fly).

**Two overlay types:**

#### Success Overlay
- **Appearance:** Small green checkmark icon (top-right corner)
- **Duration:** 2 seconds, then fades out
- **Trigger:** Successful scan of a new barcode
- **Voice:** "Correcto"

#### Duplicate Decision Overlay
- **Appearance:** Semi-transparent dialog in center of screen
- **Content:** 
  - Warning: "⚠️ ATENCIÓN"
  - Message: "CÓDIGO REPETIDO: [barcode]"
  - Two buttons:
    - **SI (Cargar Igual):** Saves the duplicate anyway
    - **NO (Descartar):** Ignores the duplicate
- **Voice:** "Atención, Duplicado"

**Setup Required:**
1. First time using overlays, the app will request permission
2. Tap "Abrir Configuración"
3. Enable "Display over other apps" for DroneInventoryScanner
4. Return to the app

### 4. Smart Duplicate Detection (Jitter Filter)

**How it works:**

The app now intelligently handles duplicate scans with a 2000ms (2-second) threshold:

1. **Same code within 2 seconds:** 
   - Silently ignored (assumes hand jitter/accidental double-scan)
   - No sound, no overlay

2. **Same code after 2 seconds:**
   - Shows Duplicate Decision Overlay
   - User chooses: Load anyway or Discard
   - Voice prompt: "Atención, Duplicado"

3. **New code:**
   - Automatically saved
   - Success overlay shown
   - Voice prompt: "Correcto"

**Benefits:**
- No more accidental duplicates from shaky hands
- User maintains control over intentional duplicate scans
- Reduces scanning errors significantly

### 5. Full Spanish Localization

**User Interface:**
All text is now in Spanish:
- "Conectar" (Connect)
- "Detener" (Stop)
- "Exportar CSV" (Export CSV)
- "Desconectado" / "Conectado" (Disconnected / Connected)

**Voice Feedback (Text-to-Speech):**
- Connection: "Escáner Conectado"
- Success: "Correcto"
- Duplicate: "Atención, Duplicado"
- Discard: "Descartado"

**All messages and prompts are in Spanish**

## Technical Details

### Permissions Required
- **BLUETOOTH** / **BLUETOOTH_CONNECT** (existing)
- **SYSTEM_ALERT_WINDOW** (new) - For floating overlays

### Storage
- Session data stored in SharedPreferences
- CSV files use MediaStore API (Android 10+)
- Backward compatible with legacy storage API

### Architecture
- **SessionManager:** Singleton for session context
- **OverlayService:** Manages floating UI elements
- **Enhanced ScanRepository:** Jitter filter + Downloads export
- **No changes to Bluetooth logic** (maintains v0.15 stability)

## Troubleshooting

### "Permiso Requerido" Dialog
- This means overlay permission is needed
- Tap "Abrir Configuración" and enable permission
- Return to app and try again

### CSV Not Found in Downloads
- Check if storage permissions are granted
- Verify the filename matches the pattern: `[Cliente]_[Sector]_[Timestamp].csv`
- Try using a file manager app instead of Files app

### Overlay Not Showing
- Ensure "Display over other apps" permission is enabled
- Check if battery optimization is disabled for the app
- Restart the app after granting permission

### Session Persists Between Launches
- Session data is saved automatically
- To change Cliente/Sector: Clear app data in Settings
- Or uninstall and reinstall the app

## Version History

- **v1.0:** Session Management, Overlays, Spanish Localization, Downloads Export
- **v0.15:** Stable Bluetooth + Scanner Service (baseline)

## Developer Notes

### Backward Compatibility
- Legacy `exportToCsv(File)` method still available
- ScanRepository constructor accepts optional Context
- All existing tests pass (except pre-existing Bluetooth test issues)

### Testing Recommendations
1. Test with real Bluetooth scanner device
2. Verify overlay permission flow
3. Test duplicate detection timing
4. Verify CSV files appear in Downloads folder
5. Test with different Cliente/Sector combinations

---

**Version:** 1.0  
**Date:** January 2026  
**Status:** Production Ready
