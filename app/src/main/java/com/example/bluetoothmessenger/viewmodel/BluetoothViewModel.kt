package com.example.bluetoothmessenger.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluetoothmessenger.model.Device
import com.example.bluetoothmessenger.utils.BluetoothUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class BluetoothViewModel(application: Application) : AndroidViewModel(application) {

    @SuppressLint("StaticFieldLeak")
    private val context = getApplication<Application>().applicationContext

    private val _pairedDevices = MutableStateFlow<List<Device>>(emptyList())
    val pairedDevices = _pairedDevices.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<Device>>(emptyList())
    val discoveredDevices = _discoveredDevices.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering = _isDiscovering.asStateFlow()

    private val _discoverableIntent = MutableStateFlow<Intent?>(null)
    val discoverableIntent = _discoverableIntent.asStateFlow()

    private val _isDiscoverable = MutableStateFlow(false)
    val isDiscoverable = _isDiscoverable.asStateFlow()

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun addDiscoveredDevice(device: BluetoothDevice) {
        val name = device.name ?: "Unknown Device"
        val newDevice = Device(name, device.address)

        if (_pairedDevices.value.none { it.address == newDevice.address } &&
            _discoveredDevices.value.none { it.address == newDevice.address }
        ) {
            _discoveredDevices.value = _discoveredDevices.value + newDevice
        }
    }

    fun clearDiscoveredDevices() {
        _discoveredDevices.value = emptyList()
    }

    fun handleDiscoveryRequest(context: Context, permissionLauncher: (Array<String>) -> Unit) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)

        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (!isGpsEnabled) {
            Toast.makeText(context, "Please turn on GPS", Toast.LENGTH_LONG).show()
            context.startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher(permissions)
        } else {
            val granted = ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (granted) {
                startDiscovery()
            } else {
                permissionLauncher(permissions)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        if (!BluetoothUtils.isBluetoothSupported() || !BluetoothUtils.isBluetoothEnabled()) {
            _isDiscovering.value = false
            return
        }

        BluetoothUtils.cancelDiscovery()
        _pairedDevices.value = BluetoothUtils.getBondedDevices().map { Device.from(it) }
        clearDiscoveredDevices()

        _isDiscovering.value = true
        val started = BluetoothUtils.startDiscovery()
        if (!started) {
            _isDiscovering.value = false
        } else {
            viewModelScope.launch {
                delay(20_000)
                if (_isDiscovering.value) {
                    BluetoothUtils.cancelDiscovery()
                    _isDiscovering.value = false
                }
            }
        }
    }

    fun handleDiscoverableResult(resultCode: Int) {
        if (resultCode > 0) {
            _isDiscoverable.value = true
            viewModelScope.launch {
                delay((resultCode + 1) * 1000L)
                _isDiscoverable.value = false
            }
        } else {
            _isDiscoverable.value = false
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun requestDiscoverable(duration: Int = 300) {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter.scanMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            _isDiscoverable.value = true
            return
        }
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, duration)
        }
        _discoverableIntent.value = intent
    }

    @SuppressLint("MissingPermission")
    fun pairDevice(device: BluetoothDevice) {
        try {
            val method = device.javaClass.getMethod("createBond")
            method.invoke(device)
            Toast.makeText(context, "Pairing with ${device.name}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Pairing failed", Toast.LENGTH_SHORT).show()
        }
    }
}
