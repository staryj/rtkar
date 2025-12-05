package com.ngerasimov.rtkar.data.repository

import com.ngerasimov.rtkar.data.api.Device
import com.ngerasimov.rtkar.data.api.RtkApiWrapper
import com.ngerasimov.rtkar.data.StorageManager
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing device data
 * Abstracts data sources (API and local storage)
 */
class DeviceRepository(
    private val apiWrapper: RtkApiWrapper,
    private val storageManager: StorageManager
) {
    /**
     * Get barrier devices from API with caching
     * @param authToken Authentication token
     * @param forceReload If true, bypass cache and fetch from API
     * @return Result containing list of devices or error
     */
    suspend fun getBarrierDevices(
        authToken: String,
        forceReload: Boolean = false
    ): Result<List<Device>> {
        return apiWrapper.getBarrierDevices(authToken, forceReload)
    }
    
    /**
     * Get intercom devices from API with caching
     * @param authToken Authentication token
     * @param forceReload If true, bypass cache and fetch from API
     * @return Result containing list of devices or error
     */
    suspend fun getIntercomDevices(
        authToken: String,
        forceReload: Boolean = false
    ): Result<List<Device>> {
        return apiWrapper.getIntercomDevices(authToken, forceReload)
    }
    
    /**
     * Open a device (trigger control action)
     * @param authToken Authentication token
     * @param deviceId Device ID to open
     * @return Result indicating success or failure
     */
    suspend fun openDevice(
        authToken: String,
        deviceId: String
    ): Result<Unit> {
        return apiWrapper.openDevice(authToken, deviceId)
    }
    
    /**
     * Get authentication token flow
     */
    val authToken: Flow<String?> = storageManager.authToken
    
    /**
     * Get entities flow
     */
    val entities: Flow<List<com.ngerasimov.rtkar.data.Entity>> = storageManager.entities
    
    /**
     * Save authentication token
     */
    suspend fun saveAuthToken(token: String) {
        storageManager.saveAuthToken(token)
    }
    
    /**
     * Save entities
     */
    suspend fun saveEntities(entities: List<com.ngerasimov.rtkar.data.Entity>) {
        storageManager.saveEntities(entities)
    }
    
    /**
     * Get entities synchronously
     */
    suspend fun getEntitiesSync(): List<com.ngerasimov.rtkar.data.Entity> {
        return storageManager.getEntitiesSync()
    }
    
    /**
     * Toggle device selection
     */
    suspend fun toggleDeviceSelection(
        deviceId: String,
        title: String,
        type: com.ngerasimov.rtkar.data.EntityType
    ) {
        storageManager.toggleDeviceSelection(deviceId, title, type)
    }
    
    /**
     * Clear all stored data
     */
    suspend fun clearAllData() {
        storageManager.clearAllData()
    }
}
