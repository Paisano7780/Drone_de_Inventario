package com.paisano.droneinventoryscanner.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.paisano.droneinventoryscanner.data.repository.ScanRepository

/**
 * MainViewModel - ViewModel for MainActivity
 */
class MainViewModel : ViewModel() {

    private val _connectionStatus = MutableLiveData<ConnectionStatus>()
    val connectionStatus: LiveData<ConnectionStatus> = _connectionStatus

    private val _lastScannedCode = MutableLiveData<String>()
    val lastScannedCode: LiveData<String> = _lastScannedCode

    private val _exportResult = MutableLiveData<ExportResult>()
    val exportResult: LiveData<ExportResult> = _exportResult

    enum class ConnectionStatus {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    sealed class ExportResult {
        object Success : ExportResult()
        data class Error(val message: String) : ExportResult()
    }

    init {
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
        _lastScannedCode.value = "None"
    }

    fun updateConnectionStatus(connected: Boolean) {
        _connectionStatus.value = if (connected) {
            ConnectionStatus.CONNECTED
        } else {
            ConnectionStatus.DISCONNECTED
        }
    }

    fun setConnecting() {
        _connectionStatus.value = ConnectionStatus.CONNECTING
    }

    fun updateLastScannedCode(code: String) {
        _lastScannedCode.value = code
    }

    fun setExportResult(success: Boolean, message: String = "") {
        _exportResult.value = if (success) {
            ExportResult.Success
        } else {
            ExportResult.Error(message)
        }
    }
}
