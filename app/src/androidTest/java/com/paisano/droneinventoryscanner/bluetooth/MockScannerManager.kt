package com.paisano.droneinventoryscanner.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * MockScannerManager - Mock implementation of IScannerManager for testing
 * Simulates scanner behavior without requiring actual Bluetooth hardware
 */
class MockScannerManager : IScannerManager {
    
    companion object {
        private const val TAG = "MockScannerManager"
    }
    
    private var listener: IScannerManager.ConnectionListener? = null
    private var isConnected = false
    
    override fun setConnectionListener(listener: IScannerManager.ConnectionListener) {
        this.listener = listener
        Log.d(TAG, "Listener set")
    }
    
    override suspend fun connect(device: BluetoothDevice?, adapter: BluetoothAdapter?): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "Mock connect called")
        delay(100) // Simulate connection delay
        isConnected = true
        withContext(Dispatchers.Main) {
            listener?.onConnected()
        }
        true
    }
    
    override suspend fun startReading() {
        Log.d(TAG, "Mock startReading called")
        // Mock implementation - does nothing, scans are simulated via simulateScan()
    }
    
    override fun stopReading() {
        Log.d(TAG, "Mock stopReading called")
    }
    
    override fun disconnect() {
        Log.d(TAG, "Mock disconnect called")
        if (isConnected) {
            isConnected = false
            listener?.onDisconnected()
        }
    }
    
    override fun isConnected(): Boolean = isConnected
    
    override fun getPairedDevices(adapter: BluetoothAdapter): Set<BluetoothDevice> {
        Log.d(TAG, "Mock getPairedDevices called")
        return emptySet()
    }
    
    /**
     * Simulate a barcode scan for testing
     * @param code The barcode to simulate
     */
    suspend fun simulateScan(code: String) = withContext(Dispatchers.Main) {
        Log.d(TAG, "Simulating scan: $code")
        if (isConnected) {
            // Simulate the format that the real scanner sends (with newline)
            listener?.onDataReceived("$code\r\n")
        } else {
            Log.w(TAG, "Cannot simulate scan - not connected")
        }
    }
}
