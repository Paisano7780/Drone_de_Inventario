package com.paisano.droneinventoryscanner.data.repository

import com.paisano.droneinventoryscanner.data.model.ScanRecord
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * ScanRepository - Manages scan records in memory and persistence
 */
class ScanRepository {

    private val scans = mutableListOf<ScanRecord>()
    private val scanSet = ConcurrentHashMap.newKeySet<String>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    /**
     * Add a new scan record
     * @param code The scanned code
     * @return true if added (not duplicate), false if duplicate
     */
    @Synchronized
    fun addScan(code: String): Boolean {
        // Check if it's a duplicate
        if (scanSet.contains(code)) {
            return false
        }

        val timestamp = System.currentTimeMillis()
        val record = ScanRecord(timestamp, code)
        scans.add(record)
        scanSet.add(code)
        return true
    }

    /**
     * Check if a code is a duplicate
     * @param code The code to check
     * @return true if duplicate, false if new
     */
    fun isDuplicate(code: String): Boolean {
        return scanSet.contains(code)
    }

    /**
     * Get all scans
     * @return List of all scan records
     */
    fun getAllScans(): List<ScanRecord> {
        return scans.toList()
    }

    /**
     * Get the last scanned code
     * @return The last scanned code or null if none
     */
    fun getLastScan(): ScanRecord? {
        return scans.lastOrNull()
    }

    /**
     * Get total number of scans
     * @return Total scan count
     */
    fun getScanCount(): Int {
        return scans.size
    }

    /**
     * Clear all scans
     */
    @Synchronized
    fun clearAll() {
        scans.clear()
        scanSet.clear()
    }

    /**
     * Export scans to CSV file
     * @param outputFile The file to write to
     * @return true if successful, false otherwise
     */
    fun exportToCsv(outputFile: File): Boolean {
        return try {
            outputFile.parentFile?.mkdirs()
            
            FileWriter(outputFile).use { writer ->
                // Write header
                writer.append("Timestamp,Code\n")
                
                // Write data
                scans.forEach { record ->
                    val formattedDate = dateFormat.format(Date(record.timestamp))
                    writer.append("\"$formattedDate\",\"${record.code}\"\n")
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
