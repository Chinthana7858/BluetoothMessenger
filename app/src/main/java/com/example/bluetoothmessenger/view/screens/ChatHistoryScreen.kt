package com.example.bluetoothmessenger.view.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.bluetoothmessenger.view.components.DeviceHistoryItem
import com.example.bluetoothmessenger.viewmodel.ChatHistoryViewModel
import com.example.bluetoothmessenger.viewmodel.ChatHistoryViewModelFactory
import com.example.bluetoothmessenger.viewmodel.ThemeViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHistoryScreen(
    navController: NavController,
    themeViewModel: ThemeViewModel,
    viewModel: ChatHistoryViewModel = viewModel(
        factory = ChatHistoryViewModelFactory(LocalContext.current.applicationContext as android.app.Application)
    )

) {
    val devices by viewModel.devicesWithHistory.collectAsState()
    val deviceLastMessages by viewModel.deviceLastMessages.collectAsState()
    val isDarkTheme by themeViewModel.isDarkTheme.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Bluetooth Chat",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                actions = {
                    IconButton(
                        onClick = { themeViewModel.toggleTheme() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Toggle theme"
                        )
                    }

                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    navController.navigate("bluetoothFinder")
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Find devices"
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            
            if (devices.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No chat history",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Find and connect to Bluetooth devices to start chatting",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { navController.navigate("bluetoothFinder") }
                        ) {
                            Text("Find Devices")
                        }
                    }
                }
            } else {
                // Device list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(devices) { device ->
                        DeviceHistoryItem(
                            device = device,
                            lastMessage = deviceLastMessages[device.deviceAddress],
                            onClick = {
                                device.deviceAddress?.let { address ->
                                    navController.navigate("chat/$address")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

