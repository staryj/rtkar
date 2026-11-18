package com.ngerasimov.rtkar.ui.settings

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ngerasimov.rtkar.R
import com.ngerasimov.rtkar.data.EntityType
import com.ngerasimov.rtkar.data.api.ApiError
import com.ngerasimov.rtkar.data.api.Device
import com.ngerasimov.rtkar.data.repository.DeviceRepository
import com.ngerasimov.rtkar.shortcut.AndroidBarrierShortcuts
import com.ngerasimov.rtkar.shortcut.BarrierShortcutPlanner
import com.ngerasimov.rtkar.shortcut.BarrierShortcutStore
import com.ngerasimov.rtkar.shortcut.ShortcutCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * ViewModel for the Settings screen
 * Manages device list loading, selection, and filtering
 */
class SettingsViewModel(
    application: Application,
    private val repository: DeviceRepository,
    private val shortcuts: BarrierShortcutStore = AndroidBarrierShortcuts(application)
) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "SettingsViewModel"
    }
    
    private val _barrierDevices = MutableStateFlow<List<Device>>(emptyList())
    val barrierDevices: StateFlow<List<Device>> = _barrierDevices.asStateFlow()
    
    private val _intercomDevices = MutableStateFlow<List<Device>>(emptyList())
    val intercomDevices: StateFlow<List<Device>> = _intercomDevices.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isLoadingBarriers = MutableStateFlow(false)
    val isLoadingBarriers: StateFlow<Boolean> = _isLoadingBarriers.asStateFlow()
    
    private val _isLoadingIntercoms = MutableStateFlow(false)
    val isLoadingIntercoms: StateFlow<Boolean> = _isLoadingIntercoms.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _shortcutNotice = MutableStateFlow<String?>(null)
    val shortcutNotice: StateFlow<String?> = _shortcutNotice.asStateFlow()
    
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
                syncBarrierShortcutTitles(filtered)
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
            val wasSelected = repository.getEntitiesSync().any { entity ->
                entity.deviceId == device.id && entity.type == type
            }
            repository.toggleDeviceSelection(
                deviceId = device.id,
                title = device.name_by_company,
                type = type
            )
            val commands = BarrierShortcutPlanner.onToggle(
                type = type,
                deviceId = device.id,
                title = device.name_by_company,
                selected = !wasSelected,
                pinSupported = shortcuts.isPinSupported(),
                alreadyPinned = shortcuts.isPinned(device.id)
            )
            shortcuts.execute(commands)
            if (commands.any { it is ShortcutCommand.PinUnsupported }) {
                _shortcutNotice.value = getApplication<Application>().getString(R.string.shortcut_pin_unsupported)
            }
        }
    }
    
    /**
     * Check if device is selected
     */
    fun isDeviceSelected(deviceId: String): Boolean {
        return _selectedDeviceIds.value.contains(deviceId)
    }
    
    /**
     * Force reload barriers (clear cache and reload)
     */
    fun reloadBarriers() {
        viewModelScope.launch {
            val token = repository.authToken.first()
            if (!token.isNullOrBlank()) {
                _isLoadingBarriers.value = true
                _error.value = null
                
                try {
                    val barrierResult = repository.getBarrierDevices(token, forceReload = true)
                    barrierResult.onSuccess { devices ->
                        val filtered = filterDevices(devices, "barriers")
                        _barrierDevices.value = filtered
                        syncBarrierShortcutTitles(filtered)
                    }.onFailure { exception ->
                        Log.e(TAG, "Failed to reload barriers", exception)
                        val errorMsg = when (exception) {
                            is ApiError -> exception.getUserMessage(getApplication())
                            else -> getApplication<Application>().getString(
                                R.string.error_failed_to_load_barriers,
                                exception.message ?: ""
                            )
                        }
                        _error.value = errorMsg
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reloading barriers", e)
                    val errorMsg = when (e) {
                        is ApiError -> e.getUserMessage(getApplication())
                        else -> getApplication<Application>().getString(
                            R.string.error_failed_to_load_barriers,
                            e.message ?: ""
                        )
                    }
                    _error.value = errorMsg
                } finally {
                    _isLoadingBarriers.value = false
                }
            }
        }
    }
    
    /**
     * Force reload intercoms (clear cache and reload)
     */
    fun reloadIntercoms() {
        viewModelScope.launch {
            val token = repository.authToken.first()
            if (!token.isNullOrBlank()) {
                _isLoadingIntercoms.value = true
                _error.value = null
                
                try {
                    val intercomResult = repository.getIntercomDevices(token, forceReload = true)
                    intercomResult.onSuccess { devices ->
                        val filtered = filterDevices(devices, "intercoms")
                        _intercomDevices.value = filtered
                    }.onFailure { exception ->
                        Log.e(TAG, "Failed to reload intercoms", exception)
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
                    Log.e(TAG, "Error reloading intercoms", e)
                    val errorMsg = when (e) {
                        is ApiError -> e.getUserMessage(getApplication())
                        else -> getApplication<Application>().getString(
                            R.string.error_failed_to_load_intercoms,
                            e.message ?: ""
                        )
                    }
                    _error.value = errorMsg
                } finally {
                    _isLoadingIntercoms.value = false
                }
            }
        }
    }

    private suspend fun syncBarrierShortcutTitles(devices: List<Device>) {
        val entities = repository.getEntitiesSync()
        val sync = BarrierShortcutPlanner.syncTitles(
            entities = entities,
            namesByDeviceId = devices.associate { it.id to it.name_by_company },
            pinnedDeviceIds = shortcuts.pinnedDeviceIds()
        )
        if (sync.entities != entities) {
            repository.saveEntities(sync.entities)
        }
        shortcuts.execute(sync.shortcutUpdates)
    }
}
