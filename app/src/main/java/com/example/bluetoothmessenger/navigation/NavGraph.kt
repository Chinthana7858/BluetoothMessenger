package com.example.bluetoothmessenger.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.bluetoothmessenger.view.screens.BluetoothFinderScreen
import com.example.bluetoothmessenger.view.screens.ChatScreen
import com.example.bluetoothmessenger.view.screens.ChatHistoryScreen
import com.example.bluetoothmessenger.viewmodel.ChatViewModel
import com.example.bluetoothmessenger.viewmodel.ThemeViewModel

@Composable
fun AppNavGraph(navController: NavHostController, themeViewModel: ThemeViewModel) {
    NavHost(navController = navController, startDestination = Screens.ChatHistory.route) {

        composable(route = Screens.ChatHistory.route) {
            ChatHistoryScreen(navController = navController, themeViewModel = themeViewModel)
        }

        composable(route = Screens.Discovery.route) {
            BluetoothFinderScreen(navController = navController)
        }
        
        composable("chat/{deviceAddress}") { backStackEntry ->
            val deviceAddress = backStackEntry.arguments?.getString("deviceAddress") ?: ""
            ChatScreen(deviceAddress = deviceAddress, navController = navController)
        }
        
        // Alternative route names for compatibility
        composable("bluetoothFinder") {
            BluetoothFinderScreen(navController = navController)
        }
    }
}
