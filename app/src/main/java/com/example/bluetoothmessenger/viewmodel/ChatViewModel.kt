package com.example.bluetoothmessenger.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluetoothmessenger.repository.BluetoothRepository
import com.example.bluetoothmessenger.utils.ConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BluetoothRepository.getInstance()

    private val _isAutoReconnectEnabled = MutableStateFlow(true)
    val isAutoReconnectEnabled = _isAutoReconnectEnabled.asStateFlow()
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    init {
        repository.initialize(application.applicationContext)
        

        viewModelScope.launch {
            repository.errors.collect { error ->
                error?.let {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = it,
                        showError = true
                    )
                }
            }
        }
    }
    

    val messages = repository.messages.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    val connectionState = repository.connectionState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ConnectionState.DISCONNECTED
    )
    
    val connectedDevice = repository.connectedDevice.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    @SuppressLint("MissingPermission")
    val connectionStatus = combine(
        connectionState,
        connectedDevice
    ) { state, device ->
        when (state) {
            ConnectionState.DISCONNECTED -> "Disconnected"
            ConnectionState.CONNECTING -> "Connecting..."
            ConnectionState.CONNECTED -> "Connected to ${device?.name ?: "Unknown Device"}"
            ConnectionState.ERROR -> "Connection failed"
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Disconnected"
    )
    

    fun connectToDevice(deviceAddress: String) {
        viewModelScope.launch {
            repository.setAutoReconnectEnabled(_isAutoReconnectEnabled.value)
            repository.connectToDevice(deviceAddress)
        }
    }
    

    fun sendMessage(messageText: String) {
        if (messageText.isBlank()) return
        
        viewModelScope.launch {
            val success = repository.sendMessage(messageText.trim())
            if (!success) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to send message",
                    showError = true
                )
            }
        }
    }
    

    fun disconnect() {
        viewModelScope.launch {
            repository.disconnect()
        }
    }
    

    fun toggleAutoReconnect() {
        _isAutoReconnectEnabled.value = !_isAutoReconnectEnabled.value
        repository.setAutoReconnectEnabled(_isAutoReconnectEnabled.value)
    }
    

    fun clearError() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            showError = false
        )
        repository.clearError()
    }


    fun clearMessagesForCurrentDevice() {
        viewModelScope.launch {
            repository.clearMessagesForCurrentDevice()
        }
    }


}

data class ChatUiState(
    val errorMessage: String? = null,
    val showError: Boolean = false,
    val isLoading: Boolean = false
)
