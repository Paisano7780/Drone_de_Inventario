package com.paisano.droneinventoryscanner.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice

/**
 * IScannerManager - Interface for scanner connectivity
 * Allows for dependency injection of real Bluetooth or mock implementations
 */
interface IScannerManager {
    
    /**
     * Connection listener interface
     */
    interface ConnectionListener {
        fun onConnected()
        fun onDisconnected()
        fun onDataReceived(data: String)
        fun onError(error: String)
    }
    
    /**
     * Set the connection listener
     */
    fun setConnectionListener(listener: ConnectionListener)
    
    /**
     * Connect to a device
     * @param device The device to connect to (may be null for mock implementations)
     * @param adapter The Bluetooth adapter (optional, may be null for mock implementations)
     * @return true if connection successful, false otherwise
     */
    suspend fun connect(device: BluetoothDevice?, adapter: BluetoothAdapter? = null): Boolean
    
    /**
     * Start reading data from the connected device
     */
    suspend fun startReading()
    
    /**
     * Stop reading data
     */
    fun stopReading()
    
    /**
     * Disconnect from the device
     */
    fun disconnect()
    
    /**
     * Check if currently connected
     */
    fun isConnected(): Boolean
    
    /**
     * Get paired Bluetooth devices (may return empty for mock implementations)
     */
    fun getPairedDevices(adapter: BluetoothAdapter): Set<BluetoothDevice>
}
