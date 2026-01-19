package com.paisano.droneinventoryscanner.data.repository

import com.paisano.droneinventoryscanner.data.model.ScanRecord
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Unit tests for ScanRepository
 * Tests duplicate detection and data management
 */
class ScanRepositoryTest {

    private lateinit var repository: ScanRepository

    @Before
    fun setup() {
        repository = ScanRepository()
    }

    @Test
    fun `addScan should add new scan successfully`() {
        val result = repository.addScan("12345")
        assertTrue(result)
        assertEquals(1, repository.getScanCount())
    }

    @Test
    fun `addScan should detect duplicates`() {
        // First scan should succeed
        val result1 = repository.addScan("12345")
        assertTrue(result1)

        // Second scan with same code should fail (duplicate)
        val result2 = repository.addScan("12345")
        assertFalse(result2)

        // Should still only have 1 scan
        assertEquals(1, repository.getScanCount())
    }

    @Test
    fun `addScan should allow different codes`() {
        repository.addScan("12345")
        repository.addScan("67890")
        repository.addScan("ABCDE")

        assertEquals(3, repository.getScanCount())
    }

    @Test
    fun `isDuplicate should detect existing codes`() {
        repository.addScan("12345")
        
        assertTrue(repository.isDuplicate("12345"))
        assertFalse(repository.isDuplicate("67890"))
    }

    @Test
    fun `getLastScan should return most recent scan`() {
        repository.addScan("FIRST")
        repository.addScan("SECOND")
        repository.addScan("THIRD")

        val lastScan = repository.getLastScan()
        assertNotNull(lastScan)
        assertEquals("THIRD", lastScan?.code)
    }

    @Test
    fun `getLastScan should return null when empty`() {
        val lastScan = repository.getLastScan()
        assertNull(lastScan)
    }

    @Test
    fun `getAllScans should return all records in order`() {
        repository.addScan("FIRST")
        repository.addScan("SECOND")
        repository.addScan("THIRD")

        val allScans = repository.getAllScans()
        assertEquals(3, allScans.size)
        assertEquals("FIRST", allScans[0].code)
        assertEquals("SECOND", allScans[1].code)
        assertEquals("THIRD", allScans[2].code)
    }

    @Test
    fun `clearAll should remove all scans`() {
        repository.addScan("12345")
        repository.addScan("67890")
        assertEquals(2, repository.getScanCount())

        repository.clearAll()
        assertEquals(0, repository.getScanCount())
        assertNull(repository.getLastScan())
    }

    @Test
    fun `exportToCsv should create valid CSV file`() {
        // Add some test data
        repository.addScan("CODE001")
        repository.addScan("CODE002")
        repository.addScan("CODE003")

        // Export to temp file
        val tempFile = File.createTempFile("test_export", ".csv")
        tempFile.deleteOnExit()

        val result = repository.exportToCsv(tempFile)
        assertTrue(result)
        assertTrue(tempFile.exists())

        // Verify file contents
        val lines = tempFile.readLines()
        assertEquals(4, lines.size) // Header + 3 data rows
        assertEquals("Timestamp,Code", lines[0])
        assertTrue(lines[1].contains("CODE001"))
        assertTrue(lines[2].contains("CODE002"))
        assertTrue(lines[3].contains("CODE003"))
    }

    @Test
    fun `scan timestamps should be in order`() {
        val startTime = System.currentTimeMillis()
        
        repository.addScan("FIRST")
        Thread.sleep(10) // Small delay to ensure different timestamps
        repository.addScan("SECOND")
        Thread.sleep(10)
        repository.addScan("THIRD")

        val scans = repository.getAllScans()
        assertTrue(scans[0].timestamp <= scans[1].timestamp)
        assertTrue(scans[1].timestamp <= scans[2].timestamp)
        assertTrue(scans[0].timestamp >= startTime)
    }
}
