package com.example.bluetoothmessenger.model

import android.Manifest
import android.bluetooth.BluetoothDevice
import androidx.annotation.RequiresPermission

data class Device(
    val name: String,
    val address: String
) {
    companion object {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        fun from(device: BluetoothDevice): Device {
            return Device(device.name ?: "Unnamed", device.address)
        }
    }
}
