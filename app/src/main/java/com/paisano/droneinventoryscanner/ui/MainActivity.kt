package com.paisano.droneinventoryscanner.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.paisano.droneinventoryscanner.R
import com.paisano.droneinventoryscanner.databinding.ActivityMainBinding
import com.paisano.droneinventoryscanner.service.ScannerService
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), ScannerService.ServiceListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    
    private var scannerService: ScannerService? = null
    private var serviceBound = false
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ScannerService.LocalBinder
            scannerService = binder.getService()
            scannerService?.setServiceListener(this@MainActivity)
            serviceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            scannerService = null
            serviceBound = false
        }
    }

    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            showDeviceSelectionDialog()
        } else {
            Toast.makeText(this, R.string.permission_required, Toast.LENGTH_LONG).show()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(this, "Notification permission recommended", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        
        setupUI()
        observeViewModel()
        requestNotificationPermission()
    }

    override fun onStart() {
        super.onStart()
        // Bind to service
        val intent = Intent(this, ScannerService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        if (serviceBound) {
            scannerService?.setServiceListener(null)
            unbindService(serviceConnection)
            serviceBound = false
        }
    }

    private fun setupUI() {
        binding.btnConnectScanner.setOnClickListener {
            if (scannerService != null && viewModel.connectionStatus.value == MainViewModel.ConnectionStatus.CONNECTED) {
                disconnectScanner()
            } else {
                checkPermissionsAndConnect()
            }
        }

        binding.btnExportCsv.setOnClickListener {
            exportCsv()
        }
    }

    private fun observeViewModel() {
        viewModel.connectionStatus.observe(this) { status ->
            when (status) {
                MainViewModel.ConnectionStatus.DISCONNECTED -> {
                    binding.tvConnectionStatus.text = getString(R.string.disconnected)
                    binding.tvConnectionStatus.setTextColor(getColor(R.color.status_disconnected))
                    binding.btnConnectScanner.text = getString(R.string.connect_scanner)
                }
                MainViewModel.ConnectionStatus.CONNECTING -> {
                    binding.tvConnectionStatus.text = getString(R.string.connecting)
                    binding.tvConnectionStatus.setTextColor(getColor(R.color.dark_primary))
                }
                MainViewModel.ConnectionStatus.CONNECTED -> {
                    binding.tvConnectionStatus.text = getString(R.string.connected)
                    binding.tvConnectionStatus.setTextColor(getColor(R.color.status_connected))
                    binding.btnConnectScanner.text = getString(R.string.disconnect_scanner)
                }
                null -> {
                    // Handle null case
                }
            }
        }

        viewModel.lastScannedCode.observe(this) { code ->
            binding.tvLastScannedCode.text = code
        }

        viewModel.exportResult.observe(this) { result ->
            when (result) {
                is MainViewModel.ExportResult.Success -> {
                    Toast.makeText(this, R.string.exported_successfully, Toast.LENGTH_SHORT).show()
                }
                is MainViewModel.ExportResult.Error -> {
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun checkPermissionsAndConnect() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            showDeviceSelectionDialog()
        } else {
            bluetoothPermissionLauncher.launch(permissions)
        }
    }

    private fun showDeviceSelectionDialog() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothAdapter = bluetoothManager?.adapter

        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.bluetooth_not_available, Toast.LENGTH_SHORT).show()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val pairedDevices = bluetoothAdapter.bondedDevices

            if (pairedDevices.isEmpty()) {
                Toast.makeText(this, R.string.no_paired_devices, Toast.LENGTH_SHORT).show()
                return
            }

            val deviceNames = pairedDevices.map { "${it.name} (${it.address})" }.toTypedArray()
            val deviceList = pairedDevices.toList()

            AlertDialog.Builder(this)
                .setTitle(R.string.select_device)
                .setItems(deviceNames) { _, which ->
                    connectToDevice(deviceList[which])
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        } catch (e: SecurityException) {
            Toast.makeText(this, R.string.permission_required, Toast.LENGTH_SHORT).show()
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        viewModel.setConnecting()
        
        // Start service if not already running
        val intent = Intent(this, ScannerService::class.java).apply {
            action = ScannerService.ACTION_START
            putExtra(ScannerService.EXTRA_DEVICE_ADDRESS, device.address)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun disconnectScanner() {
        scannerService?.disconnectScanner()
        viewModel.updateConnectionStatus(false)
    }

    private fun exportCsv() {
        val repository = scannerService?.getRepository()
        
        if (repository == null) {
            Toast.makeText(this, "Service not available", Toast.LENGTH_SHORT).show()
            return
        }

        if (repository.getScanCount() == 0) {
            Toast.makeText(this, R.string.no_data_to_export, Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Create directory in Documents/DroneScans
            val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val droneScansDir = File(documentsDir, "DroneScans")
            
            if (!droneScansDir.exists()) {
                droneScansDir.mkdirs()
            }

            // Create filename with timestamp
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            val timestamp = dateFormat.format(Date())
            val filename = "drone_scans_$timestamp.csv"
            val file = File(droneScansDir, filename)

            // Export
            val success = repository.exportToCsv(file)
            
            if (success) {
                viewModel.setExportResult(true)
                Toast.makeText(this, "Exported to: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            } else {
                viewModel.setExportResult(false, getString(R.string.export_failed))
            }
        } catch (e: Exception) {
            viewModel.setExportResult(false, "Error: ${e.message}")
        }
    }

    // ScannerService.ServiceListener implementation

    override fun onConnectionStatusChanged(connected: Boolean) {
        runOnUiThread {
            viewModel.updateConnectionStatus(connected)
        }
    }

    override fun onScanReceived(code: String, isDuplicate: Boolean) {
        runOnUiThread {
            viewModel.updateLastScannedCode(code)
        }
    }
}
