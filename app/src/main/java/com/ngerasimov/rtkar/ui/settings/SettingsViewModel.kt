package com.ngerasimov.rtkar.ui.settings

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ngerasimov.rtkar.R
import com.ngerasimov.rtkar.data.EntityType
import com.ngerasimov.rtkar.data.StorageManager
import com.ngerasimov.rtkar.data.api.ApiError
import com.ngerasimov.rtkar.data.api.Device
import com.ngerasimov.rtkar.data.api.RtkApiWrapper
import com.ngerasimov.rtkar.data.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * ViewModel for the Settings screen
 * Manages device list loading, selection, and filtering
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "SettingsViewModel"
    }
    
    private val repository = DeviceRepository(
        apiWrapper = RtkApiWrapper(application),
        storageManager = StorageManager(application)
    )
    
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
            repository.entities.collect { entities ->
                _selectedDeviceIds.value = entities.map { it.deviceId }.toSet()
            }
        }
        
        // Load devices when auth token is available
        viewModelScope.launch {
            repository.authToken
                .onEach { token ->
                    Log.v(TAG, "Auth token flow value: ${if (token.isNullOrBlank()) "empty" else "present"}")
                }
                .collect { token ->
                    if (!token.isNullOrBlank()) {
                        Log.i(TAG, "Loading devices with token")
                        loadDevices(token)
                    } else {
                        Log.w(TAG, "Auth token is empty, cannot load devices")
                    }
                }
        }
    }
    
    private suspend fun loadDevices(authToken: String) {
        Log.i(TAG, "Loading devices with auth token")
        _isLoading.value = true
        _error.value = null
        
        try {
            // Load barriers
            Log.i(TAG, "Fetching barriers...")
            val barrierResult = repository.getBarrierDevices(authToken)
            barrierResult.onSuccess { devices ->
                Log.i(TAG, "Barriers received: ${devices.size}")
                val filtered = filterDevices(devices, "barriers")
                _barrierDevices.value = filtered
                Log.i(TAG, "Barriers available for display: ${filtered.size}")
            }.onFailure { exception ->
                Log.e(TAG, "Failed to load barriers", exception)
                val errorMsg = when (exception) {
                    is ApiError -> exception.getUserMessage(getApplication())
                    else -> getApplication<Application>().getString(
                        R.string.error_failed_to_load_barriers,
                        exception.message ?: ""
                    )
                }
                _error.value = errorMsg
            }
            
            // Load intercoms
            Log.i(TAG, "Fetching intercoms...")
            val intercomResult = repository.getIntercomDevices(authToken)
            intercomResult.onSuccess { devices ->
                Log.i(TAG, "Intercoms received: ${devices.size}")
                val filtered = filterDevices(devices, "intercoms")
                _intercomDevices.value = filtered
                Log.i(TAG, "Intercoms available for display: ${filtered.size}")
            }.onFailure { exception ->
                Log.e(TAG, "Failed to load intercoms", exception)
                val errorMsg = when (exception) {
                    is ApiError -> exception.getUserMessage(getApplication())
                    else -> getApplication<Application>().getString(
                        R.string.error_failed_to_load_intercoms,
                        exception.message ?: ""
                    )
                }
                _error.value = errorMsg
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading devices", e)
            val errorMsg = when (e) {
                is ApiError -> e.getUserMessage(getApplication())
                else -> getApplication<Application>().getString(
                    R.string.error_loading_devices,
                    e.message ?: ""
                )
            }
            _error.value = errorMsg
        } finally {
            _isLoading.value = false
            Log.i(TAG, "Loading completed. Barriers: ${_barrierDevices.value.size}, Intercoms: ${_intercomDevices.value.size}")
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
        Log.v(TAG, "$deviceType: Total received: $totalCount, Filtered (active with open_door): ${filtered.size}")
        return filtered
    }
    
    /**
     * Toggle device selection
     * Note: Uses coroutine because toggleDeviceSelection is a suspend function
     */
    fun toggleDeviceSelection(device: Device, type: EntityType) {
        viewModelScope.launch {
            repository.toggleDeviceSelection(
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
