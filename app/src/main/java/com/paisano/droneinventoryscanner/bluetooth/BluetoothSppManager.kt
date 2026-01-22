package com.paisano.droneinventoryscanner.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.io.InputStream
import java.util.UUID

/**
 * BluetoothSppManager - Manages Bluetooth SPP (Serial Port Profile) connection
 * Connects to a Bluetooth device and reads data from it
 */
class BluetoothSppManager {

    companion object {
        private const val TAG = "BluetoothSppManager"
        // Standard Serial Port Profile UUID
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        // Connection timeout in milliseconds (increased for RF noise tolerance)
        private const val CONNECTION_TIMEOUT_MS = 30000L // 30 seconds
        // Cooldown period before retry in milliseconds
        private const val RETRY_COOLDOWN_MS = 3000L // 3 seconds
    }

    interface ConnectionListener {
        fun onConnected()
        fun onDisconnected()
        fun onDataReceived(data: String)
        fun onError(error: String)
    }

    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var isConnected = false
    private var listener: ConnectionListener? = null
    private var isReading = false
    private var lastConnectionAttempt = 0L

    /**
     * Set the connection listener
     */
    fun setConnectionListener(listener: ConnectionListener) {
        this.listener = listener
    }

    /**
     * Connect to a Bluetooth device
     * @param device The Bluetooth device to connect to
     * @return true if connection successful, false otherwise
     */
    suspend fun connect(device: BluetoothDevice): Boolean = withContext(Dispatchers.IO) {
        try {
            // Implement smart retry with 3-second cooldown
            val timeSinceLastAttempt = System.currentTimeMillis() - lastConnectionAttempt
            if (timeSinceLastAttempt < RETRY_COOLDOWN_MS) {
                val remainingCooldown = RETRY_COOLDOWN_MS - timeSinceLastAttempt
                Log.d(TAG, "Cooldown period: waiting ${remainingCooldown}ms before retry")
                delay(remainingCooldown)
            }
            lastConnectionAttempt = System.currentTimeMillis()

            // Close existing connection if any
            disconnect()

            Log.d(TAG, "Connecting to device: ${device.name} (${device.address})")

            // Create socket
            bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            
            // Connect with timeout to handle RF noise and interference
            val connectSuccess = try {
                withTimeoutOrNull(CONNECTION_TIMEOUT_MS) {
                    bluetoothSocket?.connect()
                    true
                } ?: false
            } catch (e: IOException) {
                // Connection failed within timeout period
                Log.e(TAG, "Connection failed during timeout: ${e.message}", e)
                false
            }
            
            if (!connectSuccess) {
                Log.e(TAG, "Connection failed or timeout after ${CONNECTION_TIMEOUT_MS}ms")
                disconnect()
                withContext(Dispatchers.Main) {
                    listener?.onError("Connection timeout")
                }
                return@withContext false
            }
            
            // Get input stream
            inputStream = bluetoothSocket?.inputStream
            
            isConnected = true
            
            withContext(Dispatchers.Main) {
                listener?.onConnected()
            }
            
            Log.d(TAG, "Connected successfully")
            true
        } catch (e: IOException) {
            Log.e(TAG, "Connection failed: ${e.message}", e)
            disconnect()
            withContext(Dispatchers.Main) {
                listener?.onError("Connection failed: ${e.message}")
            }
            false
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception: ${e.message}", e)
            withContext(Dispatchers.Main) {
                listener?.onError("Bluetooth permission denied")
            }
            false
        }
    }

    /**
     * Start reading data from the connected device
     */
    suspend fun startReading() = withContext(Dispatchers.IO) {
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
                        val completeData = stringBuilder.toString()
                        stringBuilder.clear()
                        
                        withContext(Dispatchers.Main) {
                            listener?.onDataReceived(completeData)
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
    fun stopReading() {
        isReading = false
    }

    /**
     * Disconnect from the device
     * Force socket cleanup with try-catch to prevent crashes
     */
    fun disconnect() {
        try {
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
            listener?.onDisconnected()
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
        }
    }

    /**
     * Check if currently connected
     */
    fun isConnected(): Boolean = isConnected

    /**
     * Get paired Bluetooth devices
     */
    fun getPairedDevices(adapter: BluetoothAdapter): Set<BluetoothDevice> {
        return try {
            adapter.bondedDevices ?: emptySet()
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for getting paired devices", e)
            emptySet()
        }
    }
}
