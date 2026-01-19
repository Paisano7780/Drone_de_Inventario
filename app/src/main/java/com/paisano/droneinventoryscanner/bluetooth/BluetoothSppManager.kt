package com.paisano.droneinventoryscanner.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
            // Close existing connection if any
            disconnect()

            Log.d(TAG, "Connecting to device: ${device.name} (${device.address})")

            // Create socket
            bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            
            // Connect
            bluetoothSocket?.connect()
            
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
     */
    fun disconnect() {
        try {
            isReading = false
            isConnected = false
            
            inputStream?.close()
            inputStream = null
            
            bluetoothSocket?.close()
            bluetoothSocket = null
            
            Log.d(TAG, "Disconnected")
            listener?.onDisconnected()
        } catch (e: IOException) {
            Log.e(TAG, "Error disconnecting: ${e.message}", e)
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
