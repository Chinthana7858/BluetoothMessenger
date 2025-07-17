package com.example.bluetoothmessenger.navigation

sealed class Screens(val route: String) {
    object ChatHistory : Screens("chatHistory")
    object Discovery : Screens("discovery")
    object Chat : Screens("chat")
}
