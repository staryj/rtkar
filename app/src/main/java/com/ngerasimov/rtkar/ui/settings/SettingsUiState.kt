package com.ngerasimov.rtkar.ui.settings

import com.ngerasimov.rtkar.data.api.Device

/**
 * Sealed class representing the UI state of the Settings screen
 */
sealed class SettingsUiState {
    /**
     * Initial state - loading devices
     */
    object Loading : SettingsUiState()
    
    /**
     * Success state with devices loaded
     */
    data class Success(
        val barrierDevices: List<Device>,
        val intercomDevices: List<Device>,
        val selectedDeviceIds: Set<String>,
        val error: String? = null
    ) : SettingsUiState() {
        val hasBarriers: Boolean get() = barrierDevices.isNotEmpty()
        val hasIntercoms: Boolean get() = intercomDevices.isNotEmpty()
        val isEmpty: Boolean get() = !hasBarriers && !hasIntercoms
    }
    
    /**
     * Error state
     */
    data class Error(
        val message: String
    ) : SettingsUiState()
}
