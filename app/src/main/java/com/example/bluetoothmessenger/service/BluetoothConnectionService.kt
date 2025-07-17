package com.example.bluetoothmessenger.service

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.*
import java.io.IOException
import java.util.*


class BluetoothConnectionService {
    
    companion object {
        private val APP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val TAG = "BTConnectionService"
        
        // Connection timeouts and retry settings
        private const val CONNECTION_TIMEOUT = 15000L // 15 seconds per attempt
        private const val RETRY_DELAY = 2000L // 2 seconds between attempts
        private const val MAX_RETRY_ATTEMPTS = 5 // Increased for better success rate
        private const val CLIENT_START_DELAY = 500L // Smaller delay for faster connection
    }
    
    // Socket management
    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null
    private var connectedSocket: BluetoothSocket? = null
    
    // Coroutine management
    private var serverJob: Job? = null
    private var clientJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Callback for connection events
    private var connectionCallback: BluetoothConnectionCallback? = null
    
    // Connection state
    @Volatile
    private var isConnectionActive = false
    
    interface BluetoothConnectionCallback {
        fun onConnectionEstablished(socket: BluetoothSocket, isServer: Boolean)
        fun onConnectionFailed(error: String)
        fun onConnectionLost()
    }
    

    @SuppressLint("MissingPermission")
    fun startAutoConnection(targetDeviceAddress: String, callback: BluetoothConnectionCallback) {
        this.connectionCallback = callback
        
        Log.i(TAG, "Starting enhanced auto-connection to device: $targetDeviceAddress")
        
        // Ensure clean state
        stopConnection()
        isConnectionActive = true
        
        // Always start server mode
        startServerMode()
        
        // Only start client mode if we have a target device
        if (targetDeviceAddress.isNotEmpty()) {
            serviceScope.launch {
                delay(CLIENT_START_DELAY)
                if (isConnectionActive) {
                    startClientMode(targetDeviceAddress)
                }
            }
            
            // Set overall timeout to prevent indefinite attempts (only for client connections)
            serviceScope.launch {
                delay(CONNECTION_TIMEOUT * MAX_RETRY_ATTEMPTS)
                if (isConnectionActive && connectedSocket == null) {
                    Log.w(TAG, "Overall connection timeout reached")
                    handleConnectionFailure("Connection timeout after ${MAX_RETRY_ATTEMPTS} attempts")
                }
            }
        } else {
            Log.d(TAG, "Server-only mode for background listening")
        }
    }

    @SuppressLint("MissingPermission")
    private fun startServerMode() {
        serverJob = serviceScope.launch {
            var retryCount = 0
            
            Log.d(TAG, "Starting server mode...")
            
            while (retryCount < MAX_RETRY_ATTEMPTS && isConnectionActive && !isConnected()) {
                try {
                    Log.d(TAG, "Server attempt ${retryCount + 1}/$MAX_RETRY_ATTEMPTS")
                    
                    val adapter = BluetoothAdapter.getDefaultAdapter()
                    if (adapter == null || !adapter.isEnabled) {
                        Log.e(TAG, "Bluetooth adapter not available or disabled")
                        break
                    }
                    
                    // Create server socket
                    serverSocket = adapter.listenUsingRfcommWithServiceRecord("BluetoothChat", APP_UUID)
                    Log.d(TAG, "Server socket created, listening for connections...")
                    
                    // Wait for incoming connection with timeout
                    val socket = withTimeoutOrNull(CONNECTION_TIMEOUT) {
                        serverSocket?.accept()
                    }
                    
                    if (socket != null && isConnectionActive) {
                        Log.i(TAG, "✅ Server: Connection established with ${socket.remoteDevice?.name}")
                        handleSuccessfulConnection(socket, true)
                        break
                    } else if (isConnectionActive) {
                        Log.d(TAG, "Server: Connection timeout, retrying...")
                    }
                    
                } catch (e: IOException) {
                    Log.w(TAG, "Server attempt ${retryCount + 1} failed: ${e.message}")
                } catch (e: SecurityException) {
                    Log.e(TAG, "Security exception in server mode: ${e.message}")
                    break
                } finally {
                    // Clean up server socket
                    serverSocket?.closeQuietly()
                    serverSocket = null
                }
                
                retryCount++
                if (retryCount < MAX_RETRY_ATTEMPTS && isConnectionActive && !isConnected()) {
                    Log.d(TAG, "Server: Waiting ${RETRY_DELAY}ms before retry...")
                    delay(RETRY_DELAY)
                }
            }

            if (!isConnected() && isConnectionActive && retryCount >= MAX_RETRY_ATTEMPTS) {
                Log.d(TAG, "Server mode completed, restarting for background listening...")
                delay(5000) // Wait 5 seconds before restarting
                if (isConnectionActive) {
                    startServerMode() // Restart server mode for continuous listening
                }
            }
        }
    }
    

    @SuppressLint("MissingPermission")
    private fun startClientMode(targetDeviceAddress: String) {
        clientJob = serviceScope.launch {
            var retryCount = 0
            
            Log.d(TAG, "Starting client mode...")
            
            while (retryCount < MAX_RETRY_ATTEMPTS && isConnectionActive && !isConnected()) {
                try {
                    Log.d(TAG, "Client attempt ${retryCount + 1}/$MAX_RETRY_ATTEMPTS")
                    
                    val adapter = BluetoothAdapter.getDefaultAdapter()
                    if (adapter == null || !adapter.isEnabled) {
                        Log.e(TAG, "Bluetooth adapter not available or disabled")
                        break
                    }
                    
                    // Get target device
                    val device = adapter.getRemoteDevice(targetDeviceAddress)
                    Log.d(TAG, "Client connecting to: ${device.name ?: "Unknown"} (${device.address})")
                    
                    // Cancel discovery to improve connection performance
                    if (adapter.isDiscovering) {
                        adapter.cancelDiscovery()
                    }
                    
                    // Create client socket
                    clientSocket = device.createRfcommSocketToServiceRecord(APP_UUID)
                    
                    // Attempt connection with timeout
                    val connected = withTimeoutOrNull(CONNECTION_TIMEOUT) {
                        clientSocket?.connect()
                        clientSocket?.isConnected == true
                    }
                    
                    if (connected == true && isConnectionActive) {
                        Log.i(TAG, "✅ Client: Connected successfully to ${device.name}")
                        handleSuccessfulConnection(clientSocket!!, false)
                        break
                    } else {
                        Log.d(TAG, "Client: Connection failed or timeout")
                        clientSocket?.closeQuietly()
                        clientSocket = null
                    }
                    
                } catch (e: IOException) {
                    Log.w(TAG, "Client attempt ${retryCount + 1} failed: ${e.message}")
                    clientSocket?.closeQuietly()
                    clientSocket = null
                } catch (e: SecurityException) {
                    Log.e(TAG, "Security exception in client mode: ${e.message}")
                    break
                }
                
                retryCount++
                if (retryCount < MAX_RETRY_ATTEMPTS && isConnectionActive && !isConnected()) {
                    Log.d(TAG, "Client: Waiting ${RETRY_DELAY}ms before retry...")
                    delay(RETRY_DELAY)
                }
            }
            
            if (!isConnected() && isConnectionActive) {
                Log.d(TAG, "Client mode completed without connection")
                
                // Check if server also failed
                delay(1000) // Give server a bit more time
                if (!isConnected() && isConnectionActive) {
                    handleConnectionFailure("Unable to establish connection in both server and client modes")
                }
            }
        }
    }
    

    private fun handleSuccessfulConnection(socket: BluetoothSocket, isServer: Boolean) {
        if (!isConnectionActive) return
        
        connectedSocket = socket
        
        // Cancel the other connection attempt
        if (isServer) {
            clientJob?.cancel()
            clientSocket?.closeQuietly()
        } else {
            serverJob?.cancel()
            serverSocket?.closeQuietly()
        }
        
        // Notify callback on main thread
        serviceScope.launch(Dispatchers.Main) {
            connectionCallback?.onConnectionEstablished(socket, isServer)
        }
        
        Log.i(TAG, "🎉 Connection established successfully as ${if (isServer) "Server" else "Client"}")
    }

    private fun handleConnectionFailure(error: String) {
        if (!isConnectionActive) return
        
        isConnectionActive = false
        Log.e(TAG, "Connection failed: $error")
        
        // Notify callback on main thread
        serviceScope.launch(Dispatchers.Main) {
            connectionCallback?.onConnectionFailed(error)
        }
    }
    

    fun stopConnection() {
        Log.d(TAG, "Stopping connection service...")
        
        isConnectionActive = false
        
        // Cancel all ongoing jobs
        serverJob?.cancel()
        clientJob?.cancel()
        
        // Close all sockets
        connectedSocket?.closeQuietly()
        clientSocket?.closeQuietly()
        serverSocket?.closeQuietly()
        
        // Clear references
        connectedSocket = null
        clientSocket = null
        serverSocket = null
        
        Log.d(TAG, "Connection service stopped")
    }

    fun isConnected(): Boolean = connectedSocket?.isConnected == true

    fun getConnectedSocket(): BluetoothSocket? = connectedSocket
}


private fun BluetoothSocket?.closeQuietly() {
    try {
        this?.close()
    } catch (e: IOException) {
        // Ignore close errors
    }
}


private fun BluetoothServerSocket?.closeQuietly() {
    try {
        this?.close()
    } catch (e: IOException) {
    }
}
