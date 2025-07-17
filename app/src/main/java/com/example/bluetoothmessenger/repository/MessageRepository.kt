package com.example.bluetoothmessenger.repository

import android.content.Context
import com.example.bluetoothmessenger.database.ChatDatabase
import com.example.bluetoothmessenger.database.ChatMessageDao
import com.example.bluetoothmessenger.database.DeviceInfo
import com.example.bluetoothmessenger.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first


class MessageRepository(context: Context) {
    
    private val database = ChatDatabase.getDatabase(context)
    private val chatMessageDao: ChatMessageDao = database.chatMessageDao()
    


    fun getMessagesByDevice(deviceAddress: String): Flow<List<ChatMessage>> {
        return chatMessageDao.getMessagesByDevice(deviceAddress)
    }

    fun getAllDevices(): Flow<List<DeviceInfo>> {
        return chatMessageDao.getAllDevices()
    }

    suspend fun saveMessage(message: ChatMessage): Long {
        return chatMessageDao.insertMessage(message)
    }

    suspend fun deleteMessagesByDevice(deviceAddress: String) {
        chatMessageDao.deleteMessagesByDevice(deviceAddress)
    }

    suspend fun getLatestMessageForDevice(deviceAddress: String): ChatMessage? {
        val messages = getMessagesByDevice(deviceAddress).first()
        return messages.lastOrNull()
    }
}
