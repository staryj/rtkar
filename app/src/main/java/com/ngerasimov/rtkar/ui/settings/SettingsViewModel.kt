package com.ngerasimov.rtkar.ui.settings

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ngerasimov.rtkar.R
import com.ngerasimov.rtkar.data.EntityType
import com.ngerasimov.rtkar.data.StorageManager
import com.ngerasimov.rtkar.data.api.Device
import com.ngerasimov.rtkar.data.api.RtkApiWrapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "SettingsViewModel"
    }
    
    private val storageManager = StorageManager(application)
    private val apiWrapper = RtkApiWrapper(application)
    
    private val _barrierDevices = MutableStateFlow<List<Device>>(emptyList())
    val barrierDevices: StateFlow<List<Device>> = _barrierDevices.asStateFlow()
    
    private val _intercomDevices = MutableStateFlow<List<Device>>(emptyList())
    val intercomDevices: StateFlow<List<Device>> = _intercomDevices.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _selectedDeviceIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedDeviceIds: StateFlow<Set<String>> = _selectedDeviceIds.asStateFlow()
    
    init {
        // Load selected devices
        viewModelScope.launch {
            storageManager.entities.collect { entities ->
                _selectedDeviceIds.value = entities.map { it.deviceId }.toSet()
            }
        }
        
        // Load devices when auth token is available
        viewModelScope.launch {
            storageManager.authToken
                .onEach { token ->
                    Log.d(TAG, "Auth token flow value: ${if (token.isNullOrEmpty()) "empty" else "present (${token.take(10)}...)"}")
                }
                .collect { token ->
                    if (!token.isNullOrEmpty()) {
                        Log.d(TAG, "Loading devices with token")
                        loadDevices(token)
                    } else {
                        Log.d(TAG, "Auth token is empty, cannot load devices")
                    }
                }
        }
    }
    
    private suspend fun loadDevices(authToken: String) {
        Log.d(TAG, "Loading devices with auth token")
        _isLoading.value = true
        _error.value = null
        
        try {
            // Load barriers
            Log.d(TAG, "Fetching barriers...")
            val barrierResult = apiWrapper.getBarrierDevices(authToken)
            barrierResult.onSuccess { devices ->
                Log.d(TAG, "Barriers received: ${devices.size}")
                val filtered = filterDevices(devices, "barriers")
                _barrierDevices.value = filtered
                Log.d(TAG, "Barriers available for display: ${filtered.size}")
            }.onFailure { exception ->
                Log.e(TAG, "Failed to load barriers", exception)
                val errorMsg = getApplication<android.app.Application>().getString(
                    R.string.error_failed_to_load_barriers,
                    exception.message ?: ""
                )
                _error.value = errorMsg
            }
            
            // Load intercoms
            Log.d(TAG, "Fetching intercoms...")
            val intercomResult = apiWrapper.getIntercomDevices(authToken)
            intercomResult.onSuccess { devices ->
                Log.d(TAG, "Intercoms received: ${devices.size}")
                val filtered = filterDevices(devices, "intercoms")
                _intercomDevices.value = filtered
                Log.d(TAG, "Intercoms available for display: ${filtered.size}")
            }.onFailure { exception ->
                Log.e(TAG, "Failed to load intercoms", exception)
                val errorMsg = getApplication<android.app.Application>().getString(
                    R.string.error_failed_to_load_intercoms,
                    exception.message ?: ""
                )
                _error.value = errorMsg
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading devices", e)
            val errorMsg = getApplication<android.app.Application>().getString(
                R.string.error_loading_devices,
                e.message ?: ""
            )
            _error.value = errorMsg
        } finally {
            _isLoading.value = false
            Log.d(TAG, "Loading completed. Barriers: ${_barrierDevices.value.size}, Intercoms: ${_intercomDevices.value.size}")
        }
    }
    
    /**
     * Filter devices to only include active ones with open_door capability
     */
    private fun filterDevices(devices: List<Device>, deviceType: String): List<Device> {
        val totalCount = devices.size
        val filtered = devices.filter { device ->
            device.is_active && device.capabilities.any { capability ->
                capability.name == "open_door" && capability.setup
            }
        }
        Log.d(TAG, "$deviceType: Total received: $totalCount, Filtered (active with open_door): ${filtered.size}")
        return filtered
    }
    
    /**
     * Toggle device selection
     */
    fun toggleDeviceSelection(device: Device, type: EntityType) {
        viewModelScope.launch {
            storageManager.toggleDeviceSelection(
                deviceId = device.id,
                title = device.name_by_company,
                type = type
            )
        }
    }
    
    /**
     * Check if device is selected
     */
    fun isDeviceSelected(deviceId: String): Boolean {
        return _selectedDeviceIds.value.contains(deviceId)
    }
}
