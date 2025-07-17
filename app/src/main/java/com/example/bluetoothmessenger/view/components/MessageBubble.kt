package com.example.bluetoothmessenger.view.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bluetoothmessenger.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MessageBubble(message: ChatMessage) {
    val isYourMessage = message.isFromMe || message.sender == "You"
    val isSystemMessage = message.sender == "System"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = when {
            isSystemMessage -> Arrangement.Center
            isYourMessage -> Arrangement.End
            else -> Arrangement.Start
        }
    ) {
        Card(
            modifier = Modifier.widthIn(max = if (isSystemMessage) 260.dp else 280.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isSystemMessage -> MaterialTheme.colorScheme.surfaceVariant
                    isYourMessage -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.secondaryContainer
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isSystemMessage) 0.dp else 2.dp),
            shape = RoundedCornerShape(
                topStart = if (isSystemMessage) 8.dp else if (isYourMessage) 16.dp else 4.dp,
                topEnd = if (isSystemMessage) 8.dp else if (isYourMessage) 4.dp else 16.dp,
                bottomStart = 12.dp,
                bottomEnd = 12.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = if (isSystemMessage) 8.dp else 12.dp,
                    vertical = if (isSystemMessage) 4.dp else 8.dp
                )
            ) {
                if (!isSystemMessage && !isYourMessage) {
                    Text(
                        text = message.deviceName ?: message.sender,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = message.message,
                    fontSize = if (isSystemMessage) 10.sp else MaterialTheme.typography.bodyMedium.fontSize,
                    color = when {
                        isSystemMessage -> MaterialTheme.colorScheme.onSurfaceVariant
                        isYourMessage -> Color.White
                        else -> MaterialTheme.colorScheme.onSecondaryContainer
                    },
                    fontWeight = if (isSystemMessage) FontWeight.Normal else FontWeight.Light
                )

                if (!isSystemMessage) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isYourMessage) Arrangement.End else Arrangement.Start
                    ) {
                        Text(
                            text = formatTimestamp(message.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = if (isYourMessage) Color.White.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}


/**
 * Formats timestamp for display in chat bubbles
 */
private fun formatTimestamp(timestamp: Long): String {
    val now = Calendar.getInstance()
    val messageTime = Calendar.getInstance().apply { timeInMillis = timestamp }
    
    return when {
        // If today, show only time
        now.get(Calendar.DAY_OF_YEAR) == messageTime.get(Calendar.DAY_OF_YEAR) &&
        now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) -> {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
        // If yesterday, show "Yesterday HH:mm"
        now.get(Calendar.DAY_OF_YEAR) - messageTime.get(Calendar.DAY_OF_YEAR) == 1 &&
        now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) -> {
            "Yesterday ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))}"
        }
        // If within this year, show "MMM dd, HH:mm"
        now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) -> {
            SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
        // If different year, show full date
        else -> {
            SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
    }
}
