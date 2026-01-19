package com.paisano.droneinventoryscanner.service

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ServiceTestRule
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeoutException

/**
 * Instrumented test for ScannerService
 * Tests service binding and basic operations
 */
@RunWith(AndroidJUnit4::class)
class ScannerServiceTest {

    @get:Rule
    val serviceRule = ServiceTestRule()

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        // Clean up
    }

    @Test
    fun testServiceBind() {
        val serviceIntent = Intent(context, ScannerService::class.java)
        
        try {
            val binder = serviceRule.bindService(serviceIntent)
            assertNotNull("Service binder should not be null", binder)
            
            val localBinder = binder as ScannerService.LocalBinder
            val service = localBinder.getService()
            assertNotNull("Service should not be null", service)
            
            val repository = service.getRepository()
            assertNotNull("Repository should not be null", repository)
            assertEquals("Initial scan count should be 0", 0, repository.getScanCount())
        } catch (e: TimeoutException) {
            fail("Service binding timed out")
        }
    }

    @Test
    fun testRepositoryOperations() {
        val serviceIntent = Intent(context, ScannerService::class.java)
        
        try {
            val binder = serviceRule.bindService(serviceIntent)
            val localBinder = binder as ScannerService.LocalBinder
            val service = localBinder.getService()
            val repository = service.getRepository()
            
            // Test adding scans
            val added = repository.addScan("TEST123")
            assertTrue("First scan should be added", added)
            assertEquals("Scan count should be 1", 1, repository.getScanCount())
            
            // Test duplicate detection
            val duplicate = repository.addScan("TEST123")
            assertFalse("Duplicate scan should not be added", duplicate)
            assertEquals("Scan count should still be 1", 1, repository.getScanCount())
            
            // Test getting last scan
            val lastScan = repository.getLastScan()
            assertNotNull("Last scan should not be null", lastScan)
            assertEquals("Last scan code should match", "TEST123", lastScan?.code)
        } catch (e: TimeoutException) {
            fail("Service binding timed out")
        }
    }

    @Test
    fun testServiceListener() {
        val serviceIntent = Intent(context, ScannerService::class.java)
        
        try {
            val binder = serviceRule.bindService(serviceIntent)
            val localBinder = binder as ScannerService.LocalBinder
            val service = localBinder.getService()
            
            var connectionStatusChanged = false
            var scanReceived = false
            
            val listener = object : ScannerService.ServiceListener {
                override fun onConnectionStatusChanged(connected: Boolean) {
                    connectionStatusChanged = true
                }

                override fun onScanReceived(code: String, isDuplicate: Boolean) {
                    scanReceived = true
                }
            }
            
            service.setServiceListener(listener)
            assertNotNull("Listener should be set", listener)
            
            // Note: Connection status and scan events would need actual Bluetooth
            // hardware or mocking to test fully. This test verifies the listener can be set.
        } catch (e: TimeoutException) {
            fail("Service binding timed out")
        }
    }
}
