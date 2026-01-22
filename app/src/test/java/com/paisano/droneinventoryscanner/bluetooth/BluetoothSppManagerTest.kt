package com.paisano.droneinventoryscanner.bluetooth

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for BluetoothSppManager
 * Tests connection state management and error handling
 */
class BluetoothSppManagerTest {

    private lateinit var bluetoothManager: IScannerManager
    private var connectedCallbackCalled = false
    private var disconnectedCallbackCalled = false
    private var errorCallbackCalled = false
    private var lastError: String? = null

    @Before
    fun setup() {
        bluetoothManager = BluetoothSppManager()
        connectedCallbackCalled = false
        disconnectedCallbackCalled = false
        errorCallbackCalled = false
        lastError = null
    }

    @Test
    fun testInitialState() {
        // Verify initial state
        assertFalse("Should not be connected initially", bluetoothManager.isConnected())
    }

    @Test
    fun testSetConnectionListener() {
        val listener = object : IScannerManager.ConnectionListener {
            override fun onConnected() {
                connectedCallbackCalled = true
            }

            override fun onDisconnected() {
                disconnectedCallbackCalled = true
            }

            override fun onDataReceived(data: String) {
                // Not tested here
            }

            override fun onError(error: String) {
                errorCallbackCalled = true
                lastError = error
            }
        }

        bluetoothManager.setConnectionListener(listener)
        // If we got here without exception, the listener was set successfully
        assertTrue("Listener should be set without error", true)
    }

    @Test
    fun testDisconnectWhenNotConnected() {
        val listener = object : IScannerManager.ConnectionListener {
            override fun onConnected() {
                connectedCallbackCalled = true
            }

            override fun onDisconnected() {
                disconnectedCallbackCalled = true
            }

            override fun onDataReceived(data: String) {
                // Not tested here
            }

            override fun onError(error: String) {
                errorCallbackCalled = true
                lastError = error
            }
        }

        bluetoothManager.setConnectionListener(listener)
        
        // Call disconnect when not connected
        bluetoothManager.disconnect()
        
        // Should not trigger disconnected callback since we weren't connected
        assertFalse("onDisconnected should not be called when not connected", disconnectedCallbackCalled)
        assertFalse("Should still not be connected", bluetoothManager.isConnected())
    }

    @Test
    fun testStopReadingWhenNotReading() {
        // Should not throw exception
        bluetoothManager.stopReading()
        assertTrue("Stop reading should complete without error", true)
    }

    @Test
    fun testSppUuid() {
        // Verify the SPP UUID is correct
        val expectedUuid = "00001101-0000-1000-8000-00805F9B34FB"
        assertEquals("SPP UUID should be standard serial port profile", 
            expectedUuid, 
            BluetoothSppManager.SPP_UUID.toString().uppercase())
    }

    @Test
    fun testMultipleDisconnectCalls() {
        // Multiple disconnect calls should be safe
        bluetoothManager.disconnect()
        bluetoothManager.disconnect()
        bluetoothManager.disconnect()
        
        assertFalse("Should not be connected after multiple disconnects", bluetoothManager.isConnected())
    }

    @Test
    fun testStopReadingMultipleTimes() {
        // Multiple stop reading calls should be safe
        bluetoothManager.stopReading()
        bluetoothManager.stopReading()
        bluetoothManager.stopReading()
        
        assertTrue("Multiple stop reading calls should not cause issues", true)
    }
}
