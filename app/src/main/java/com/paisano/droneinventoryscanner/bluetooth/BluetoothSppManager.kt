package com.paisano.droneinventoryscanner.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.util.UUID

/**
 * BluetoothSppManager - Manages Bluetooth SPP (Serial Port Profile) connection
 * Connects to a Bluetooth device and reads data from it
 */
class BluetoothSppManager : IScannerManager {

    companion object {
        private const val TAG = "BluetoothSppManager"
        // Standard Serial Port Profile UUID
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        // Connection timeout in milliseconds (increased for RF noise tolerance)
        private const val CONNECTION_TIMEOUT_MS = 30000L // 30 seconds
        // Cooldown period before retry in milliseconds
        private const val RETRY_COOLDOWN_MS = 3000L // 3 seconds
    }

    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    @Volatile private var isConnected = false
    private var listener: IScannerManager.ConnectionListener? = null
    @Volatile private var isReading = false
    private var lastConnectionAttempt = 0L
    private var isDisconnecting = false

    /**
     * Set the connection listener
     */
    override fun setConnectionListener(listener: IScannerManager.ConnectionListener) {
        this.listener = listener
    }

    /**
     * Connect to a Bluetooth device
     * @param device The Bluetooth device to connect to
     * @param adapter The Bluetooth adapter (optional, needed to cancel discovery for improved stability)
     * @return true if connection successful, false otherwise
     */
    override suspend fun connect(device: BluetoothDevice?, adapter: BluetoothAdapter?): Boolean = withContext(Dispatchers.IO) {
        if (device == null) {
            Log.e(TAG, "Device is null")
            withContext(Dispatchers.Main) {
                listener?.onError("Device is null")
            }
            return@withContext false
        }
        
        // Validate adapter is enabled
        if (adapter != null && !adapter.isEnabled) {
            Log.e(TAG, "Bluetooth adapter is not enabled")
            withContext(Dispatchers.Main) {
                listener?.onError("Bluetooth is not enabled")
            }
            return@withContext false
        }

        // Check cooldown period to prevent rapid reconnection attempts
        val now = System.currentTimeMillis()
        if (now - lastConnectionAttempt < RETRY_COOLDOWN_MS) {
            val waitTime = RETRY_COOLDOWN_MS - (now - lastConnectionAttempt)
            Log.d(TAG, "Connection cooldown active, waiting ${waitTime}ms")
            delay(waitTime)
        }
        lastConnectionAttempt = System.currentTimeMillis()

        // 1. Cancel Discovery (Crucial for bandwidth)
        try {
            adapter?.cancelDiscovery()
            // Give the adapter time to fully cancel discovery
            delay(300)
        } catch (e: SecurityException) {
            Log.w(TAG, "Could not cancel discovery: ${e.message}")
        }

        // 2. Clear old connection
        disconnect()

        var success = false
        var socketToUse: BluetoothSocket? = null
        
        try {
            Log.d(TAG, "Attempting Standard Insecure Connection...")
            socketToUse = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
            socketToUse?.connect()
            success = true
        } catch (e: IOException) {
            Log.w(TAG, "Standard connection failed: ${e.message}. Trying Reflection (Port 1)...")
            // Close failed socket
            try {
                socketToUse?.close()
            } catch (e2: IOException) {
                Log.w(TAG, "Error closing failed socket: ${e2.message}")
            }
            socketToUse = null
            
            try {
                // FALLBACK: Port 1 Reflection Method
                val m = device.javaClass.getMethod("createInsecureRfcommSocket", Int::class.javaPrimitiveType)
                socketToUse = m.invoke(device, 1) as BluetoothSocket
                socketToUse?.connect()
                success = true
                Log.d(TAG, "Connected via Reflection (Port 1)!")
            } catch (e2: Exception) {
                Log.e(TAG, "Reflection connection also failed: ${e2.message}")
                success = false
                // Close failed socket
                try {
                    socketToUse?.close()
                } catch (e3: IOException) {
                    Log.w(TAG, "Error closing failed reflection socket: ${e3.message}")
                }
                socketToUse = null
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception during connection: ${e.message}")
            success = false
            try {
                socketToUse?.close()
            } catch (e2: IOException) {
                Log.w(TAG, "Error closing socket after security exception: ${e2.message}")
            }
            socketToUse = null
        }

        if (success && socketToUse != null) {
            bluetoothSocket = socketToUse
            inputStream = bluetoothSocket?.inputStream
            isConnected = true
            isDisconnecting = false
            withContext(Dispatchers.Main) {
                listener?.onConnected()
            }
            Log.d(TAG, "Successfully connected to ${device.name} (${device.address})")
            return@withContext true
        } else {
            disconnect() // Cleanup
            withContext(Dispatchers.Main) {
                listener?.onError("Connection failed (Protocol Error)")
            }
            return@withContext false
        }
    }

    /**
     * Start reading data from the connected device
     */
    override suspend fun startReading() = withContext(Dispatchers.IO) {
        if (!isConnected || inputStream == null) {
            Log.w(TAG, "Not connected, cannot start reading")
            return@withContext
        }

        isReading = true
        val buffer = ByteArray(1024)
        val stringBuilder = StringBuilder()

        try {
            while (isReading && isConnected) {
                val bytesRead = inputStream?.read(buffer) ?: break
                
                if (bytesRead > 0) {
                    val data = String(buffer, 0, bytesRead)
                    stringBuilder.append(data)
                    
                    // Check if we have a complete line (ends with CR or LF)
                    if (data.contains("\r") || data.contains("\n")) {
                        val completeData = stringBuilder.toString().trim()
                        stringBuilder.clear()
                        
                        // Only send non-empty data after trimming
                        if (completeData.isNotEmpty()) {
                            withContext(Dispatchers.Main) {
                                listener?.onDataReceived(completeData)
                            }
                        }
                    }
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error reading data: ${e.message}", e)
            withContext(Dispatchers.Main) {
                listener?.onError("Connection lost: ${e.message}")
                listener?.onDisconnected()
            }
            isConnected = false
        }
    }

    /**
     * Stop reading data
     */
    override fun stopReading() {
        isReading = false
    }

    /**
     * Disconnect from the device
     * Force socket cleanup with try-catch to prevent crashes
     */
    override fun disconnect() {
        // Prevent duplicate disconnection callbacks
        if (isDisconnecting) {
            Log.d(TAG, "Disconnection already in progress")
            return
        }
        
        val wasConnected = isConnected
        
        try {
            isDisconnecting = true
            isReading = false
            isConnected = false
            
            // Close input stream with explicit error handling
            try {
                inputStream?.close()
            } catch (e: IOException) {
                Log.w(TAG, "Error closing input stream: ${e.message}")
            } finally {
                inputStream = null
            }
            
            // Force socket cleanup - close even if it appears null or closed
            try {
                bluetoothSocket?.close()
            } catch (e: IOException) {
                Log.w(TAG, "Error closing socket: ${e.message}")
            } finally {
                bluetoothSocket = null
            }
            
            Log.d(TAG, "Disconnected and cleaned up resources")
            
            // Only call onDisconnected if we were actually connected
            if (wasConnected) {
                listener?.onDisconnected()
            }
        } catch (e: IOException) {
            Log.e(TAG, "IO error during disconnect: ${e.message}", e)
            // Ensure resources are cleared even if error occurs
            inputStream = null
            bluetoothSocket = null
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error during disconnect: ${e.message}", e)
            // Ensure resources are cleared even if error occurs
            inputStream = null
            bluetoothSocket = null
        } finally {
            isDisconnecting = false
        }
    }

    /**
     * Check if currently connected
     */
    override fun isConnected(): Boolean = isConnected

    /**
     * Get paired Bluetooth devices
     */
    override fun getPairedDevices(adapter: BluetoothAdapter): Set<BluetoothDevice> {
        return try {
            adapter.bondedDevices ?: emptySet()
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for getting paired devices", e)
            emptySet()
        }
    }
}
