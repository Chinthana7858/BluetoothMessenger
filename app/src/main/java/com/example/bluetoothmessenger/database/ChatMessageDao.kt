package com.example.bluetoothmessenger.database

import androidx.room.*
import com.example.bluetoothmessenger.model.ChatMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessage>>
    
    @Query("SELECT * FROM chat_messages WHERE deviceAddress = :deviceAddress ORDER BY timestamp ASC")
    fun getMessagesByDevice(deviceAddress: String): Flow<List<ChatMessage>>
    
    @Query("SELECT DISTINCT deviceAddress, deviceName FROM chat_messages WHERE deviceAddress IS NOT NULL")
    fun getAllDevices(): Flow<List<DeviceInfo>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessage>)
    
    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllMessages()
    
    @Query("DELETE FROM chat_messages WHERE deviceAddress = :deviceAddress")
    suspend fun deleteMessagesByDevice(deviceAddress: String)
    
    @Query("SELECT COUNT(*) FROM chat_messages WHERE deviceAddress = :deviceAddress")
    suspend fun getMessageCountByDevice(deviceAddress: String): Int
}

data class DeviceInfo(
    val deviceAddress: String?,
    val deviceName: String?
)
