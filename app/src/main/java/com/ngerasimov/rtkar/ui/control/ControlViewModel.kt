package com.ngerasimov.rtkar.ui.control

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ngerasimov.rtkar.R
import com.ngerasimov.rtkar.data.Entity
import com.ngerasimov.rtkar.data.api.ApiError
import com.ngerasimov.rtkar.data.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ViewModel for the Control screen
 * Manages entity list, loading states, and device control actions
 */
class ControlViewModel(
    application: Application,
    private val repository: DeviceRepository
) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "ControlViewModel"
    }
    
    private val _allEntities = MutableStateFlow<List<Entity>>(emptyList())
    val allEntities: StateFlow<List<Entity>> = _allEntities.asStateFlow()
    
    // Track which entities are currently processing requests
    private val _loadingEntityIds = MutableStateFlow<Set<String>>(emptySet())
    val loadingEntityIds: StateFlow<Set<String>> = _loadingEntityIds.asStateFlow()
    
    // Track editing mode state - lazy initialization for better performance
    private val _isEditingMode = MutableStateFlow(false)
    val isEditingMode: StateFlow<Boolean> = _isEditingMode.asStateFlow()
    
    // Track error messages
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    init {
        // Load all stored entities in a single list
        viewModelScope.launch {
            repository.entities.collect { entities ->
                _allEntities.value = entities
                Log.i(TAG, "Entities loaded: ${entities.size} total")
            }
        }
    }
    
    /**
     * Trigger control action for an entity (open device)
     * @param entity The entity to control
     */
    fun triggerControlAction(entity: Entity) {
        viewModelScope.launch {
            // Mark entity as loading
            _loadingEntityIds.value = _loadingEntityIds.value + entity.deviceId
            
            try {
                // Get auth token
                val authToken = repository.authToken.first()
                if (authToken.isNullOrBlank()) {
                    val errorMsg = getApplication<Application>().getString(R.string.error_auth_token_missing)
                    Log.e(TAG, "Auth token is empty, cannot open device")
                    _errorMessage.value = errorMsg
                    return@launch
                }
                
                Log.i(TAG, "Opening device ${entity.deviceId} (${entity.title})")
                
                // Make POST request to open device
                val result = repository.openDevice(authToken, entity.deviceId)
                
                result.onSuccess {
                    Log.i(TAG, "Successfully opened device ${entity.deviceId}")
                    // Clear any previous error
                    _errorMessage.value = null
                }.onFailure { exception ->
                    val errorMsg = when (exception) {
                        is ApiError -> exception.getUserMessage(getApplication())
                        else -> exception.message ?: getApplication<Application>().getString(R.string.error_failed_to_open_device)
                    }
                    Log.e(TAG, "Failed to open device ${entity.deviceId}", exception)
                    _errorMessage.value = errorMsg
                }
            } catch (e: Exception) {
                val errorMsg = when (e) {
                    is ApiError -> e.getUserMessage(getApplication())
                    else -> e.message ?: getApplication<Application>().getString(R.string.error_opening_device)
                }
                Log.e(TAG, "Error triggering control action for ${entity.deviceId}", e)
                _errorMessage.value = errorMsg
            } finally {
                // Remove entity from loading set
                _loadingEntityIds.value = _loadingEntityIds.value - entity.deviceId
            }
        }
    }
    
    /**
     * Check if an entity is currently loading
     */
    fun isLoading(entityId: String): Boolean {
        return _loadingEntityIds.value.contains(entityId)
    }
    
    /**
     * Toggle editing mode
     */
    fun toggleEditingMode() {
        _isEditingMode.value = !_isEditingMode.value
    }
    
    /**
     * Remove an entity from the list
     */
    fun removeEntity(entity: Entity) {
        viewModelScope.launch {
            val currentEntities = _allEntities.value.toMutableList()
            val index = currentEntities.indexOfFirst { 
                it.deviceId == entity.deviceId && it.type == entity.type 
            }
            if (index >= 0) {
                currentEntities.removeAt(index)
                repository.saveEntities(currentEntities)
            }
        }
    }
    
    /**
     * Save the entity order from the provided list
     * Used after drag-and-drop operations complete
     * Note: Uses coroutine because saveEntities is a suspend function
     */
    suspend fun saveEntityOrder(entities: List<Entity>) {
        repository.saveEntities(entities)
    }
    
    /**
     * Clear the error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
}
