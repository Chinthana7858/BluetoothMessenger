package com.example.bluetoothmessenger.view.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext
import com.example.bluetoothmessenger.utils.ConnectionState
import com.example.bluetoothmessenger.viewmodel.ChatViewModel
import com.example.bluetoothmessenger.viewmodel.ChatViewModelFactory
import com.example.bluetoothmessenger.view.components.MessageBubble
import kotlinx.coroutines.launch


@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    deviceAddress: String,
    navController: NavController,
    viewModel: ChatViewModel = viewModel(
        factory = ChatViewModelFactory(LocalContext.current.applicationContext as android.app.Application)
    )
) {

    val messages by viewModel.messages.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val connectedDevice by viewModel.connectedDevice.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val isAutoReconnectEnabled by viewModel.isAutoReconnectEnabled.collectAsState()
    

    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }
    
    // Auto-connect when screen starts (happens in background, works even if other user isn't on chat screen)
    LaunchedEffect(deviceAddress) {
        viewModel.connectToDevice(deviceAddress)
    }
    
    // Handle back navigation with option to keep connection alive
    DisposableEffect(Unit) {
        onDispose {
        }
    }
    
    // Error handling
    if (uiState.showError) {
        LaunchedEffect(uiState.errorMessage) {
            // Auto-dismiss error after 3 seconds
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(connectedDevice?.name ?: "Unknown Device")
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ConnectionStatusIndicator(connectionState)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = connectionStatus,
                                style = MaterialTheme.typography.bodySmall,
                                color = getConnectionStatusColor(connectionState)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        navController.popBackStack() 
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Clear messages for current device button
                    IconButton(onClick = { viewModel.clearMessagesForCurrentDevice() }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear chat history"
                        )
                    }
                    
                    // Reconnect button
                    if (connectionState == ConnectionState.ERROR || connectionState == ConnectionState.DISCONNECTED) {
                        IconButton(onClick = { viewModel.connectToDevice(deviceAddress) }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reconnect"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            
            // Auto-reconnect toggle
            AutoReconnectToggle(
                enabled = isAutoReconnectEnabled,
                onToggle = { viewModel.toggleAutoReconnect() }
            )
            
            // Connection progress card (only show when connecting)
            if (connectionState == ConnectionState.CONNECTING) {
                ConnectionProgressCard()
            }
            
            // Error message (if any)
            uiState.errorMessage?.let { errorMsg ->
                if (uiState.showError) {
                    ErrorMessageCard(
                        message = errorMsg,
                        onDismiss = { viewModel.clearError() }
                    )
                }
            }
            
            // Messages List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(message)
                }
                
                // Add some bottom padding
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            // Message Input Section
            MessageInputSection(
                messageText = messageText,
                onMessageTextChange = { messageText = it },
                onSendMessage = {
                    if (messageText.isNotBlank()) {
                        viewModel.sendMessage(messageText.trim())
                        messageText = ""
                    }
                },
                isConnected = connectionState == ConnectionState.CONNECTED
            )
        }
    }
}

@Composable
private fun AutoReconnectToggle(
    enabled: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Auto-Reconnect",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Automatically reconnect when connection is lost",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = { onToggle() }
            )
        }
    }
}

@Composable
private fun ConnectionProgressCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Establishing Connection",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )

            }
        }
    }
}

@Composable
private fun ErrorMessageCard(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Dismiss error",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun ConnectionStatusIndicator(connectionState: ConnectionState) {
    val color = getConnectionStatusColor(connectionState)
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(color, RoundedCornerShape(4.dp))
    )
}

@Composable
private fun getConnectionStatusColor(connectionState: ConnectionState): Color {
    return when (connectionState) {
        ConnectionState.CONNECTED -> Color(0xFF4CAF50) // Green
        ConnectionState.CONNECTING -> Color(0xFFFF9800) // Orange
        ConnectionState.ERROR -> Color(0xFFF44336) // Red
        ConnectionState.DISCONNECTED -> Color(0xFF9E9E9E) // Gray
    }
}

@Composable
private fun MessageInputSection(
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    isConnected: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = onMessageTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { 
                    Text(
                        if (isConnected) "Type a message..." 
                        else "Connect to start messaging"
                    ) 
                },
                enabled = isConnected,
                maxLines = 3,
                shape = RoundedCornerShape(24.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            FloatingActionButton(
                onClick = onSendMessage,
                modifier = Modifier.size(48.dp),
                containerColor = if (isConnected && messageText.isNotBlank()) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (isConnected && messageText.isNotBlank()) 
                        Color.White 
                    else 
                        Color.Gray
                )
            }
        }
    }
}

