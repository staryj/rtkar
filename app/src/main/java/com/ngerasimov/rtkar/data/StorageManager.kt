package com.ngerasimov.rtkar.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_storage")

class StorageManager(private val context: Context) {

    companion object {
        private val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
        private val ENTITIES_KEY = stringPreferencesKey("entities")
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Get the authentication token
     */
    val authToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[AUTH_TOKEN_KEY]
    }

    /**
     * Get the list of entities
     */
    val entities: Flow<List<Entity>> = context.dataStore.data.map { preferences ->
        val entitiesJson = preferences[ENTITIES_KEY]
        if (entitiesJson.isNullOrEmpty()) {
            emptyList()
        } else {
            try {
                json.decodeFromString<List<Entity>>(entitiesJson)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    /**
     * Save the authentication token
     */
    suspend fun saveAuthToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[AUTH_TOKEN_KEY] = token
        }
    }

    /**
     * Clear the authentication token
     */
    suspend fun clearAuthToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(AUTH_TOKEN_KEY)
        }
    }

    /**
     * Save the list of entities
     */
    suspend fun saveEntities(entities: List<Entity>) {
        context.dataStore.edit { preferences ->
            val entitiesJson = json.encodeToString(entities)
            preferences[ENTITIES_KEY] = entitiesJson
        }
    }

    /**
     * Add a single entity to the list
     */
    suspend fun addEntity(entity: Entity) {
        val currentEntities = getEntitiesSync()
        val updatedEntities = currentEntities + entity
        saveEntities(updatedEntities)
    }

    /**
     * Update an entity at the given index
     */
    suspend fun updateEntity(index: Int, entity: Entity) {
        val currentEntities = getEntitiesSync().toMutableList()
        if (index in currentEntities.indices) {
            currentEntities[index] = entity
            saveEntities(currentEntities)
        }
    }

    /**
     * Remove an entity at the given index
     */
    suspend fun removeEntity(index: Int) {
        val currentEntities = getEntitiesSync().toMutableList()
        if (index in currentEntities.indices) {
            currentEntities.removeAt(index)
            saveEntities(currentEntities)
        }
    }

    /**
     * Clear all entities
     */
    suspend fun clearEntities() {
        context.dataStore.edit { preferences ->
            preferences.remove(ENTITIES_KEY)
        }
    }

    /**
     * Clear all stored data (auth token and entities)
     */
    suspend fun clearAllData() {
        context.dataStore.edit { preferences ->
            preferences.remove(AUTH_TOKEN_KEY)
            preferences.remove(ENTITIES_KEY)
        }
    }

    /**
     * Get entities synchronously (for one-time reads)
     * Note: This should be used sparingly, prefer Flow-based access
     */
    suspend fun getEntitiesSync(): List<Entity> {
        val preferences = context.dataStore.data.first()
        val entitiesJson = preferences[ENTITIES_KEY]
        return if (entitiesJson.isNullOrEmpty()) {
            emptyList()
        } else {
            try {
                json.decodeFromString<List<Entity>>(entitiesJson)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    /**
     * Check if a device is selected (exists in entities)
     */
    suspend fun isDeviceSelected(deviceId: String, type: EntityType): Boolean {
        val entities = getEntitiesSync()
        return entities.any { it.deviceId == deviceId && it.type == type }
    }
    
    /**
     * Toggle device selection - add if not selected, remove if selected
     */
    suspend fun toggleDeviceSelection(deviceId: String, title: String, type: EntityType) {
        val entities = getEntitiesSync().toMutableList()
        val existingIndex = entities.indexOfFirst { it.deviceId == deviceId && it.type == type }
        
        if (existingIndex >= 0) {
            // Remove if exists
            entities.removeAt(existingIndex)
        } else {
            // Add if doesn't exist
            entities.add(Entity(type = type, title = title, deviceId = deviceId))
        }
        
        saveEntities(entities)
    }
}
