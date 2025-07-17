package com.example.bluetoothmessenger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.bluetoothmessenger.navigation.AppNavGraph
import com.example.bluetoothmessenger.repository.BluetoothRepository
import com.example.bluetoothmessenger.ui.theme.BluetoothMessengerTheme
import com.example.bluetoothmessenger.viewmodel.ChatViewModel
import com.example.bluetoothmessenger.viewmodel.ThemeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        BluetoothRepository.getInstance().initialize(this)

        setContent {
            val themeViewModel: ThemeViewModel = viewModel()
            val isDarkTheme by themeViewModel.isDarkTheme.collectAsState()
            
            BluetoothMessengerTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                AppNavGraph(navController = navController, themeViewModel = themeViewModel)
            }
        }
    }
}

