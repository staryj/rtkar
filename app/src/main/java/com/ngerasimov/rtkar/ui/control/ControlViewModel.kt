package com.ngerasimov.rtkar.ui.control

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ngerasimov.rtkar.data.Entity
import com.ngerasimov.rtkar.data.StorageManager
import com.ngerasimov.rtkar.data.api.RtkApiWrapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ControlViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "ControlViewModel"
    }
    
    private val storageManager = StorageManager(application)
    private val apiWrapper = RtkApiWrapper(application)
    
    private val _allEntities = MutableStateFlow<List<Entity>>(emptyList())
    val allEntities: StateFlow<List<Entity>> = _allEntities.asStateFlow()
    
    // Track which entities are currently processing requests
    private val _loadingEntityIds = MutableStateFlow<Set<String>>(emptySet())
    val loadingEntityIds: StateFlow<Set<String>> = _loadingEntityIds.asStateFlow()
    
    // Track editing mode state
    private val _isEditingMode = MutableStateFlow(false)
    val isEditingMode: StateFlow<Boolean> = _isEditingMode.asStateFlow()
    
    init {
        // Load all stored entities in a single list
        viewModelScope.launch {
            storageManager.entities.collect { entities ->
                _allEntities.value = entities
                Log.d(TAG, "Entities loaded: ${entities.size} total")
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
                val authToken = storageManager.authToken.first()
                if (authToken.isNullOrEmpty()) {
                    Log.e(TAG, "Auth token is empty, cannot open device")
                    return@launch
                }
                
                Log.d(TAG, "Opening device ${entity.deviceId} (${entity.title})")
                
                // Make POST request to open device
                val result = apiWrapper.openDevice(authToken, entity.deviceId)
                
                result.onSuccess {
                    Log.d(TAG, "Successfully opened device ${entity.deviceId}")
                }.onFailure { exception ->
                    Log.e(TAG, "Failed to open device ${entity.deviceId}", exception)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error triggering control action for ${entity.deviceId}", e)
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
                storageManager.saveEntities(currentEntities)
            }
        }
    }
    
    /**
     * Reorder entities by moving an item from one position to another
     */
    fun reorderEntities(fromPosition: Int, toPosition: Int) {
        viewModelScope.launch {
            val currentEntities = _allEntities.value.toMutableList()
            if (fromPosition in currentEntities.indices && toPosition in currentEntities.indices) {
                val item = currentEntities.removeAt(fromPosition)
                currentEntities.add(toPosition, item)
                storageManager.saveEntities(currentEntities)
            }
        }
    }
    
    /**
     * Save the entity order from the provided list
     * Used after drag-and-drop operations complete
     */
    fun saveEntityOrder(entities: List<Entity>) {
        viewModelScope.launch {
            storageManager.saveEntities(entities)
        }
    }
}
