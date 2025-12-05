package com.ngerasimov.rtkar.ui.control

import com.ngerasimov.rtkar.data.Entity

/**
 * Sealed class representing the UI state of the Control screen
 */
sealed class ControlUiState {
    /**
     * Initial state - loading entities
     */
    object Loading : ControlUiState()
    
    /**
     * Success state with entities loaded
     */
    data class Success(
        val entities: List<Entity>,
        val loadingEntityIds: Set<String>,
        val isEditingMode: Boolean,
        val errorMessage: String? = null
    ) : ControlUiState()
    
    /**
     * Error state
     */
    data class Error(
        val message: String
    ) : ControlUiState()
    
    /**
     * Empty state - no entities selected
     */
    object Empty : ControlUiState()
}
