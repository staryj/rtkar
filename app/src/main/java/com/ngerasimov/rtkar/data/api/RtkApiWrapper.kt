package com.ngerasimov.rtkar.data.api

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.builtins.ListSerializer
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit

private val Context.apiDataStore: DataStore<Preferences> by preferencesDataStore(name = "rtk_api_cache")

/**
 * Wrapper class for RTK API services with caching support
 */
class RtkApiWrapper(private val context: Context) {
    
    companion object {
        private const val BASE_URL = "https://household.key.rt.ru/"
        private const val CACHE_LIFETIME_MS = 24 * 60 * 60 * 1000L // 1 day in milliseconds
        
        // DataStore keys
        private val BARRIER_DEVICES_KEY = stringPreferencesKey("barrier_devices")
        private val BARRIER_CACHE_TIMESTAMP_KEY = longPreferencesKey("barrier_cache_timestamp")
        private val INTERCOM_DEVICES_KEY = stringPreferencesKey("intercom_devices")
        private val INTERCOM_CACHE_TIMESTAMP_KEY = longPreferencesKey("intercom_cache_timestamp")
    }
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
    
    private val apiService: RtkApiService = retrofit.create(RtkApiService::class.java)
    
    /**
     * Get barrier devices with caching
     * @param authToken Authentication token
     * @param forceReload If true, bypass cache and fetch from API
     * @return List of barrier devices
     */
    suspend fun getBarrierDevices(
        authToken: String,
        forceReload: Boolean = false
    ): Result<List<Device>> {
        return try {
            // Check cache if not forcing reload
            if (!forceReload) {
                val cachedData = getCachedBarrierDevices()
                if (cachedData != null) {
                    return Result.success(cachedData)
                }
            }
            
            // Fetch from API
            val response = apiService.getBarrierDevices(
                authorization = authToken,
                accept = "application/json",
                referer = "https://key.rt.ru/"
            )
            
            val devices = response.data.devices
            
            // Cache the result
            cacheBarrierDevices(devices)
            
            Result.success(devices)
        } catch (e: Exception) {
            // If API call fails, try to return cached data
            val cachedData = getCachedBarrierDevices()
            if (cachedData != null) {
                Result.success(cachedData)
            } else {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get intercom devices with caching
     * @param authToken Authentication token
     * @param forceReload If true, bypass cache and fetch from API
     * @return List of intercom devices
     */
    suspend fun getIntercomDevices(
        authToken: String,
        forceReload: Boolean = false
    ): Result<List<Device>> {
        return try {
            // Check cache if not forcing reload
            if (!forceReload) {
                val cachedData = getCachedIntercomDevices()
                if (cachedData != null) {
                    return Result.success(cachedData)
                }
            }
            
            // Fetch from API
            val response = apiService.getIntercomDevices(
                authorization = authToken,
                accept = "application/json",
                referer = "https://key.rt.ru/"
            )
            
            val devices = response.data.devices
            
            // Cache the result
            cacheIntercomDevices(devices)
            
            Result.success(devices)
        } catch (e: Exception) {
            // If API call fails, try to return cached data
            val cachedData = getCachedIntercomDevices()
            if (cachedData != null) {
                Result.success(cachedData)
            } else {
                Result.failure(e)
            }
        }
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
        return try {
            val response = apiService.openDevice(
                id = deviceId,
                authorization = authToken,
                accept = "application/json",
                referer = "https://key.rt.ru/"
            )
            
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to open device: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get cached barrier devices if cache is still valid
     */
    private suspend fun getCachedBarrierDevices(): List<Device>? {
        val preferences = context.apiDataStore.data.first()
        val cachedJson = preferences[BARRIER_DEVICES_KEY] ?: return null
        val timestamp = preferences[BARRIER_CACHE_TIMESTAMP_KEY] ?: return null
        
        // Check if cache is still valid (within 1 day)
        val currentTime = System.currentTimeMillis()
        if (currentTime - timestamp > CACHE_LIFETIME_MS) {
            return null // Cache expired
        }
        
        return try {
            json.decodeFromString(ListSerializer(Device.serializer()), cachedJson)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get cached intercom devices if cache is still valid
     */
    private suspend fun getCachedIntercomDevices(): List<Device>? {
        val preferences = context.apiDataStore.data.first()
        val cachedJson = preferences[INTERCOM_DEVICES_KEY] ?: return null
        val timestamp = preferences[INTERCOM_CACHE_TIMESTAMP_KEY] ?: return null
        
        // Check if cache is still valid (within 1 day)
        val currentTime = System.currentTimeMillis()
        if (currentTime - timestamp > CACHE_LIFETIME_MS) {
            return null // Cache expired
        }
        
        return try {
            json.decodeFromString(ListSerializer(Device.serializer()), cachedJson)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Cache barrier devices with current timestamp
     */
    private suspend fun cacheBarrierDevices(devices: List<Device>) {
        context.apiDataStore.edit { preferences ->
            val devicesJson = json.encodeToString(ListSerializer(Device.serializer()), devices)
            preferences[BARRIER_DEVICES_KEY] = devicesJson
            preferences[BARRIER_CACHE_TIMESTAMP_KEY] = System.currentTimeMillis()
        }
    }
    
    /**
     * Cache intercom devices with current timestamp
     */
    private suspend fun cacheIntercomDevices(devices: List<Device>) {
        context.apiDataStore.edit { preferences ->
            val devicesJson = json.encodeToString(ListSerializer(Device.serializer()), devices)
            preferences[INTERCOM_DEVICES_KEY] = devicesJson
            preferences[INTERCOM_CACHE_TIMESTAMP_KEY] = System.currentTimeMillis()
        }
    }
}
