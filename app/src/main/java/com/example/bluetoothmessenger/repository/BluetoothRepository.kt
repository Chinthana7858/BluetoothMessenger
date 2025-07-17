package com.example.bluetoothmessenger.repository

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import com.example.bluetoothmessenger.model.ChatMessage
import com.example.bluetoothmessenger.service.BluetoothConnectionService
import com.example.bluetoothmessenger.utils.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.io.IOException


class BluetoothRepository {
    
    companion object {
        private const val TAG = "BluetoothRepository"

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: BluetoothRepository? = null
        
        fun getInstance(): BluetoothRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BluetoothRepository().also { INSTANCE = it }
            }
        }
    }
    
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val connectionService = BluetoothConnectionService()
    
    // Database repository for persistent storage
    private var messageRepository: MessageRepository? = null
    
    // Connection state management
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()
    
    // Message management - now using database with in-memory cache
    private val _currentMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _currentMessages.asStateFlow()
    
    // Error handling
    private val _errors = MutableStateFlow<String?>(null)
    val errors = _errors.asStateFlow()
    
    // Connected device info
    private val _connectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val connectedDevice = _connectedDevice.asStateFlow()
    
    // Current device address for filtering messages
    private val _currentDeviceAddress = MutableStateFlow<String?>(null)
    val currentDeviceAddress = _currentDeviceAddress.asStateFlow()
    
    // Auto-reconnection capability
    private var lastConnectedDeviceAddress: String? = null
    private var autoReconnectEnabled = true
    
    // Background service support
    private var currentContext: Context? = null
    private var currentSocket: BluetoothSocket? = null
    

    fun initialize(context: Context) {
        currentContext = context
        messageRepository = MessageRepository(context)
        
        //background listening
        startPersistentBackgroundListening()
    }
    

    fun loadMessagesForDevice(deviceAddress: String) {
        _currentDeviceAddress.value = deviceAddress
        
        messageRepository?.let { repo ->
            repositoryScope.launch {
                repo.getMessagesByDevice(deviceAddress).collect { dbMessages ->
                    _currentMessages.value = dbMessages
                    Log.d(TAG, "Loaded ${dbMessages.size} messages for device $deviceAddress")
                }
            }
        }
    }
    

    private fun startPersistentBackgroundListening() {
        Log.d(TAG, "Starting persistent background listening")
        
        // server mode
        repositoryScope.launch {
            while (true) {
                try {
                    if (_connectionState.value != ConnectionState.CONNECTED) {
                        Log.d(TAG, "Starting background server listening...")
                        
                        connectionService.startAutoConnection(
                            targetDeviceAddress = "",
                            callback = object : BluetoothConnectionService.BluetoothConnectionCallback {
                                override fun onConnectionEstablished(socket: BluetoothSocket, isServer: Boolean) {
                                    repositoryScope.launch {
                                        Log.i(TAG, "Background connection established!")
                                        handleIncomingConnection(socket)
                                    }
                                }
                                
                                override fun onConnectionFailed(error: String) {
                                    Log.d(TAG, "Background listening cycle completed, restarting...")
                                }
                                
                                override fun onConnectionLost() {
                                    Log.w(TAG, "Background connection lost, restarting listener...")
                                }
                            }
                        )
                    }
                    
                    // Wait before next cycle if not connected
                    kotlinx.coroutines.delay(10000)
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error in background listening cycle", e)
                    kotlinx.coroutines.delay(15000)
                }
            }
        }
    }
    

    @SuppressLint("MissingPermission")
    fun handleIncomingConnection(socket: BluetoothSocket) {
        repositoryScope.launch @androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT) {
            Log.i(TAG, "Handling incoming background connection from ${socket.remoteDevice?.name}")
            
            currentSocket = socket
            _connectionState.value = ConnectionState.CONNECTED
            _connectedDevice.value = socket.remoteDevice
            
            // Load existing messages for this device
            socket.remoteDevice?.address?.let { address ->
                loadMessagesForDevice(address)
            }
            
            // Start listening for messages
            startMessageListener(socket)
            
            // Add system message
            addSystemMessage("Connected to ${socket.remoteDevice?.name ?: "Unknown Device"} (background connection)")
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(deviceAddress: String) {
        lastConnectedDeviceAddress = deviceAddress
        _connectionState.value = ConnectionState.CONNECTING
        clearError()
        
        // Load existing message
        loadMessagesForDevice(deviceAddress)
        
        Log.d(TAG, "Initiating connection to device: $deviceAddress")
        
        // Direct connection attempt
        connectionService.startAutoConnection(
            targetDeviceAddress = deviceAddress,
            callback = object : BluetoothConnectionService.BluetoothConnectionCallback {
                override fun onConnectionEstablished(socket: android.bluetooth.BluetoothSocket, isServer: Boolean) {
                    repositoryScope.launch {
                        Log.i(TAG, "Direct connection established as ${if (isServer) "Server" else "Client"}")
                        currentSocket = socket
                        _connectionState.value = ConnectionState.CONNECTED
                        _connectedDevice.value = socket.remoteDevice
                        
                        // Start listening for messages in background
                        startMessageListener(socket)
                        
                        // Add system message
                        addSystemMessage("Connected to ${socket.remoteDevice?.name ?: "Unknown Device"}")
                    }
                }
                
                override fun onConnectionFailed(error: String) {
                    repositoryScope.launch {
                        Log.e(TAG, "Direct connection failed: $error")
                        _connectionState.value = ConnectionState.ERROR
                        _errors.value = error
                        
                        // Attempt auto-reconnection if enabled
                        if (autoReconnectEnabled && lastConnectedDeviceAddress != null) {
                            scheduleReconnection()
                        }
                    }
                }
                
                override fun onConnectionLost() {
                    repositoryScope.launch {
                        Log.w(TAG, "Direct connection lost")
                        _connectionState.value = ConnectionState.DISCONNECTED
                        _connectedDevice.value = null
                        addSystemMessage("Connection lost")
                        
                        // Attempt auto-reconnection if enabled
                        if (autoReconnectEnabled && lastConnectedDeviceAddress != null) {
                            scheduleReconnection()
                        }
                    }
                }
            }
        )
    }

    private fun scheduleReconnection() {
        repositoryScope.launch {
            kotlinx.coroutines.delay(5000) // Wait 5 seconds before retry
            if (_connectionState.value != ConnectionState.CONNECTED && 
                autoReconnectEnabled && 
                lastConnectedDeviceAddress != null) {
                
                Log.d(TAG, "Attempting auto-reconnection...")
                connectToDevice(lastConnectedDeviceAddress!!)
            }
        }
    }
    

    @SuppressLint("MissingPermission")
    private fun startMessageListener(socket: android.bluetooth.BluetoothSocket) {
        repositoryScope.launch {
            val buffer = ByteArray(1024)
            
            try {
                val inputStream = socket.inputStream
                val deviceName = socket.remoteDevice?.name
                val deviceAddress = socket.remoteDevice?.address
                
                while (_connectionState.value == ConnectionState.CONNECTED) {
                    try {
                        val bytes = inputStream.read(buffer)
                        if (bytes > 0) {
                            val messageText = String(buffer, 0, bytes).trim()
                            if (messageText.isNotEmpty()) {
                                val message = ChatMessage(
                                    sender = deviceName ?: "Friend",
                                    message = messageText,
                                    deviceName = deviceName,
                                    deviceAddress = deviceAddress,
                                    isFromMe = false
                                )
                                addMessage(message)
                                Log.d(TAG, "Message received: $messageText")
                            }
                        }
                    } catch (e: IOException) {
                        Log.e(TAG, "Error reading message", e)
                        _connectionState.value = ConnectionState.ERROR
                        _errors.value = "Failed to read message: ${e.message}"
                        break
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Failed to setup message listener", e)
                _connectionState.value = ConnectionState.ERROR
                _errors.value = "Failed to setup message listener: ${e.message}"
            }
        }
    }
    

    @SuppressLint("MissingPermission")
    fun sendMessage(messageText: String): Boolean {
        if (_connectionState.value != ConnectionState.CONNECTED) {
            _errors.value = "Cannot send message: not connected"
            return false
        }
        
        return try {
            // Use current socket if available, otherwise get from connection service
            val socket = currentSocket ?: connectionService.getConnectedSocket()
            val outputStream = socket?.outputStream
            
            if (outputStream != null) {
                outputStream.write(messageText.toByteArray())
                outputStream.flush()
                
                // Add message to local list and database
                val message = ChatMessage(
                    sender = "You",
                    message = messageText,
                    deviceName = socket.remoteDevice?.name,
                    deviceAddress = socket.remoteDevice?.address,
                    isFromMe = true
                )
                addMessage(message)
                
                Log.d(TAG, "Message sent: $messageText")
                true
            } else {
                _errors.value = "Failed to send message: output stream not available"
                false
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to send message", e)
            _errors.value = "Failed to send message: ${e.message}"
            false
        }
    }
    

    fun disconnect() {
        autoReconnectEnabled = false
        lastConnectedDeviceAddress = null
        _connectionState.value = ConnectionState.DISCONNECTED
        _connectedDevice.value = null
        
        // Close current socket
        currentSocket?.let { socket ->
            try {
                socket.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error closing socket", e)
            }
        }
        currentSocket = null

        connectionService.stopConnection()
        
        addSystemMessage("Disconnected")
        Log.d(TAG, "Disconnected from device")
    }
    

    fun setAutoReconnectEnabled(enabled: Boolean) {
        autoReconnectEnabled = enabled
        Log.d(TAG, "Auto-reconnect ${if (enabled) "enabled" else "disabled"}")
    }
    

    private fun addMessage(message: ChatMessage) {
        _currentMessages.value = _currentMessages.value + message
        
        // Save to database asynchronously
        messageRepository?.let { repo ->
            repositoryScope.launch {
                try {
                    repo.saveMessage(message)
                    Log.d(TAG, "Message saved to database")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save message to database", e)
                }
            }
        }
    }
    

    @SuppressLint("MissingPermission")
    private fun addSystemMessage(text: String) {
        val systemMessage = ChatMessage(
            sender = "System",
            message = text,
            deviceName = _connectedDevice.value?.name,
            deviceAddress = _connectedDevice.value?.address,
            isFromMe = false
        )
        addMessage(systemMessage)
    }
    

    fun clearError() {
        _errors.value = null
    }

    fun clearMessagesForCurrentDevice() {
        val deviceAddress = _currentDeviceAddress.value
        if (deviceAddress != null) {
            _currentMessages.value = emptyList()
            
            messageRepository?.let { repo ->
                repositoryScope.launch {
                    try {
                        repo.deleteMessagesByDevice(deviceAddress)
                        Log.d(TAG, "Messages cleared for device $deviceAddress")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to clear messages for device", e)
                    }
                }
            }
        }
    }

}
