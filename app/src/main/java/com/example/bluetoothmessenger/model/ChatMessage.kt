package com.example.bluetoothmessenger.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String,
    val message: String,
    val deviceName: String? = null,
    val deviceAddress: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isFromMe: Boolean = false
)
