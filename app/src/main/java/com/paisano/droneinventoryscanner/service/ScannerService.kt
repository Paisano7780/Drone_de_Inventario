package com.paisano.droneinventoryscanner.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.util.Log
import androidx.core.app.NotificationCompat
import com.paisano.droneinventoryscanner.R
import com.paisano.droneinventoryscanner.bluetooth.BluetoothSppManager
import com.paisano.droneinventoryscanner.bluetooth.IScannerManager
import com.paisano.droneinventoryscanner.data.parser.DataParser
import com.paisano.droneinventoryscanner.data.repository.ScanRepository
import com.paisano.droneinventoryscanner.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * ScannerService - Foreground service for Bluetooth scanner
 * Runs in background and maintains connection to scanner
 */
class ScannerService : Service(), IScannerManager.ConnectionListener, OnInitListener {

    companion object {
        private const val TAG = "ScannerService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "scanner_service_channel"
        const val ACTION_START = "com.paisano.droneinventoryscanner.START"
        const val ACTION_STOP = "com.paisano.droneinventoryscanner.STOP"
        const val EXTRA_DEVICE_ADDRESS = "device_address"
        private const val RECONNECT_DELAY_MS = 3000L // 3-second delay as per requirements
        private const val MAX_RECONNECT_ATTEMPTS = 5
        
        // For dependency injection during testing
        var scannerManagerFactory: (() -> IScannerManager)? = null
    }

    interface ServiceListener {
        fun onConnectionStatusChanged(connected: Boolean)
        fun onScanReceived(code: String, isDuplicate: Boolean)
    }

    private val binder = LocalBinder()
    private var listener: ServiceListener? = null
    
    private val bluetoothManager: IScannerManager by lazy {
        scannerManagerFactory?.invoke() ?: BluetoothSppManager()
    }
    private val scanRepository by lazy { ScanRepository(this) }
    private val dataParser by lazy { DataParser() }
    
    private var textToSpeech: TextToSpeech? = null
    private var ttsInitialized = false
    
    private var currentDevice: BluetoothDevice? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    inner class LocalBinder : Binder() {
        fun getService(): ScannerService = this@ScannerService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        
        // Initialize TTS
        textToSpeech = TextToSpeech(this, this)
        
        // Set up Bluetooth manager
        bluetoothManager.setConnectionListener(this)
        
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val deviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS)
                if (deviceAddress != null) {
                    startForegroundService()
                    connectToDevice(deviceAddress)
                }
            }
            ACTION_STOP -> {
                stopService()
            }
        }
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    fun setServiceListener(listener: ServiceListener?) {
        this.listener = listener
    }

    fun getRepository(): ScanRepository = scanRepository
    
    fun getScannerManager(): IScannerManager = bluetoothManager

    fun connectToScanner(device: BluetoothDevice) {
        currentDevice = device
        connectToDevice(device.address)
    }

    fun disconnectScanner() {
        reconnectJob?.cancel()
        bluetoothManager.disconnect()
        currentDevice = null
        reconnectAttempts = 0
    }

    private fun startForegroundService() {
        val notification = createNotification("Scanner Ready")
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun connectToDevice(deviceAddress: String) {
        serviceScope.launch(Dispatchers.IO) {
            try {
                val bluetoothAdapter = getBluetoothAdapter()
                val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
                
                if (device != null) {
                    currentDevice = device
                    val connected = bluetoothManager.connect(device, bluetoothAdapter)
                    
                    if (connected) {
                        reconnectAttempts = 0
                        bluetoothManager.startReading()
                    } else {
                        scheduleReconnect()
                    }
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Permission denied", e)
                onError("Bluetooth permission denied")
            }
        }
    }

    private fun scheduleReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Log.w(TAG, "Max reconnect attempts reached")
            speak("Scanner connection failed")
            return
        }

        reconnectAttempts++
        reconnectJob = serviceScope.launch {
            Log.d(TAG, "Scheduling reconnect attempt $reconnectAttempts")
            delay(RECONNECT_DELAY_MS)
            currentDevice?.let { device ->
                connectToDevice(device.address)
            }
        }
    }

    private fun getBluetoothAdapter(): BluetoothAdapter? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val manager = getSystemService(BluetoothManager::class.java)
            manager?.adapter
        } else {
            @Suppress("DEPRECATION")
            BluetoothAdapter.getDefaultAdapter()
        }
    }

    private fun stopService() {
        bluetoothManager.disconnect()
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        
        bluetoothManager.stopReading()
        bluetoothManager.disconnect()
        
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        
        reconnectJob?.cancel()
    }

    // BluetoothSppManager.ConnectionListener implementation

    override fun onConnected() {
        Log.d(TAG, "Connected to scanner")
        reconnectAttempts = 0
        updateNotification("Scanner Connected")
        speak("Escáner Conectado")
        listener?.onConnectionStatusChanged(true)
    }

    override fun onDisconnected() {
        Log.d(TAG, "Disconnected from scanner")
        updateNotification("Scanner Disconnected")
        speak("Scanner Disconnected")
        listener?.onConnectionStatusChanged(false)
        
        // Auto-reconnect
        if (currentDevice != null) {
            scheduleReconnect()
        }
    }

    override fun onDataReceived(data: String) {
        Log.d(TAG, "Data received: $data")
        
        val cleanedCode = dataParser.parseRawInput(data)
        
        if (cleanedCode != null && dataParser.isValidCode(cleanedCode)) {
            val isDuplicate = scanRepository.isDuplicate(cleanedCode)
            
            if (isDuplicate) {
                speak("Duplicate")
            } else {
                scanRepository.addScan(cleanedCode)
                // Speak last 3 digits
                val last3 = if (cleanedCode.length >= 3) {
                    cleanedCode.takeLast(3)
                } else {
                    cleanedCode
                }
                speak(last3)
            }
            
            listener?.onScanReceived(cleanedCode, isDuplicate)
        }
    }

    override fun onError(error: String) {
        Log.e(TAG, "Error: $error")
        updateNotification("Error: $error")
    }

    // TextToSpeech.OnInitListener implementation

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Set language to Spanish
            val result = textToSpeech?.setLanguage(Locale("es", "ES"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "TTS Spanish language not supported, falling back to default")
                textToSpeech?.setLanguage(Locale.getDefault())
            } else {
                ttsInitialized = true
                Log.d(TAG, "TTS initialized successfully with Spanish")
            }
        } else {
            Log.e(TAG, "TTS initialization failed")
        }
    }

    private fun speak(text: String) {
        if (ttsInitialized) {
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    // Notification management

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(message: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.scanner_service_notification))
            .setContentText(message)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(message: String) {
        val notification = createNotification(message)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }
}
