package com.paisano.droneinventoryscanner.data.repository

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.paisano.droneinventoryscanner.data.model.ScanRecord
import java.io.File
import java.io.FileWriter
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * ScanRepository - Manages scan records in memory and persistence
 */
class ScanRepository(private val context: Context? = null) {

    private val scans = mutableListOf<ScanRecord>()
    private val scanSet = ConcurrentHashMap.newKeySet<String>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    
    // For duplicate detection with jitter filter
    private var lastScannedCode: String? = null
    private var lastScannedTime: Long = 0
    private val jitterThresholdMs = 2000L // 2 seconds

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
        lastScannedCode = code
        lastScannedTime = timestamp
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
     * Check if code is duplicate with jitter filter
     * @param code The code to check
     * @return DuplicateStatus indicating the type of duplicate or NEW
     */
    fun checkDuplicateWithJitter(code: String): DuplicateStatus {
        val currentTime = System.currentTimeMillis()
        
        // Check if same as last code
        if (code == lastScannedCode) {
            val timeDiff = currentTime - lastScannedTime
            
            // Jitter filter: ignore if within threshold
            if (timeDiff < jitterThresholdMs) {
                return DuplicateStatus.JITTER
            }
            
            // Same code, but after threshold - ask user
            return DuplicateStatus.DUPLICATE_DECISION_NEEDED
        }
        
        // Different code - check if it exists in history
        if (scanSet.contains(code)) {
            return DuplicateStatus.DUPLICATE_DECISION_NEEDED
        }
        
        return DuplicateStatus.NEW
    }
    
    /**
     * Force add a scan even if it's a duplicate
     * Note: This adds to scanSet to maintain consistent duplicate detection
     */
    @Synchronized
    fun forceAddScan(code: String): Boolean {
        val timestamp = System.currentTimeMillis()
        val record = ScanRecord(timestamp, code)
        scans.add(record)
        scanSet.add(code) // Add to scanSet to ensure consistent duplicate detection
        lastScannedCode = code
        lastScannedTime = timestamp
        return true
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
        lastScannedCode = null
        lastScannedTime = 0
    }

    /**
     * Export scans to CSV file in Downloads folder
     * @param filenamePrefix Prefix for filename (Cliente_Sector)
     * @return Pair of success boolean and filename
     */
    fun exportToCsvInDownloads(filenamePrefix: String): Pair<Boolean, String?> {
        return try {
            if (context == null) {
                Log.e("ScanRepository", "Context is null - cannot export to Downloads folder")
                return Pair(false, null)
            }
            
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            val timestamp = dateFormat.format(Date())
            val filename = "${filenamePrefix}_${timestamp}.csv"
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use MediaStore for Android 10+
                exportUsingMediaStore(filename)
            } else {
                // Use direct file access for older versions
                exportUsingFile(filename)
            }
            
            Pair(true, filename)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, null)
        }
    }

    /**
     * Export using MediaStore API (Android 10+)
     */
    private fun exportUsingMediaStore(filename: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || context == null) return
        
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                writeCSVContent(outputStream)
            }
        }
    }
    
    /**
     * Export using direct File API (Android 9 and below)
     */
    private fun exportUsingFile(filename: String) {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        downloadsDir.mkdirs()
        val file = File(downloadsDir, filename)
        
        FileWriter(file).use { writer ->
            writeCSVContent(writer)
        }
    }
    
    /**
     * Write CSV content to OutputStream
     */
    private fun writeCSVContent(outputStream: OutputStream) {
        outputStream.bufferedWriter().use { writer ->
            writeCSVContent(writer)
        }
    }
    
    /**
     * Write CSV content to Appendable (FileWriter or BufferedWriter)
     */
    private fun writeCSVContent(writer: Appendable) {
        // Write header
        writer.append("Timestamp,Code\n")
        
        // Write data
        scans.forEach { record ->
            val formattedDate = dateFormat.format(Date(record.timestamp))
            writer.append("\"$formattedDate\",\"${record.code}\"\n")
        }
    }

    /**
     * Export scans to CSV file (legacy method for backward compatibility)
     * @param outputFile The file to write to
     * @return true if successful, false otherwise
     */
    fun exportToCsv(outputFile: File): Boolean {
        return try {
            outputFile.parentFile?.mkdirs()
            
            FileWriter(outputFile).use { writer ->
                writeCSVContent(writer)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    enum class DuplicateStatus {
        NEW,
        JITTER,
        DUPLICATE_DECISION_NEEDED
    }
}
