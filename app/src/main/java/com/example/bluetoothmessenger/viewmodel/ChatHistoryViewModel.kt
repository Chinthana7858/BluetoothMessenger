package com.example.bluetoothmessenger.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluetoothmessenger.database.DeviceInfo
import com.example.bluetoothmessenger.repository.MessageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


class ChatHistoryViewModel(application: Application) : AndroidViewModel(application) {
    
    private val messageRepository = MessageRepository(application.applicationContext)

    val devicesWithHistory: StateFlow<List<DeviceInfo>> = messageRepository.getAllDevices()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // State for last messages for each device
    private val _deviceLastMessages = MutableStateFlow<Map<String?, String>>(emptyMap())
    val deviceLastMessages: StateFlow<Map<String?, String>> = _deviceLastMessages.asStateFlow()
    
    init {
        // Load last messages for each device
        viewModelScope.launch {
            devicesWithHistory.collect { devices ->
                val lastMessagesMap = mutableMapOf<String?, String>()
                
                devices.forEach { device ->
                    device.deviceAddress?.let { address ->
                        try {
                            val lastMessage = messageRepository.getLatestMessageForDevice(address)
                            lastMessage?.let { message ->
                                lastMessagesMap[address] = message.message
                            }
                        } catch (e: Exception) {
                            lastMessagesMap[address] = "Error loading message"
                        }
                    }
                }
                
                _deviceLastMessages.value = lastMessagesMap
            }
        }
    }
}
