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
import kotlinx.serialization.builtins.ListSerializer
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import retrofit2.HttpException

private val Context.apiDataStore: DataStore<Preferences> by preferencesDataStore(name = "rtk_api_cache")

/**
 * Wrapper class for RTK API services with caching support
 * Provides methods to interact with RTK API endpoints with automatic caching
 * and error handling
 */
class RtkApiWrapper(private val context: Context) {
    
    companion object {
        private const val TAG = "RtkApiWrapper"
        private const val BASE_URL = com.ngerasimov.rtkar.BuildConfig.API_BASE_URL
        private const val REFERER_URL = com.ngerasimov.rtkar.BuildConfig.API_REFERER_URL
        private const val CACHE_LIFETIME_MS = 24 * 60 * 60 * 1000L // 1 day in milliseconds
        private const val HTTP_TIMEOUT_SECONDS = 30L
        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_FORBIDDEN = 403
        
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
        .connectTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
    
    private val apiService: RtkApiService = retrofit.create(RtkApiService::class.java)
    
    /**
     * Convert exception to ApiError
     */
    private fun Exception.toApiError(): ApiError {
        return when (this) {
            is IOException, is SocketTimeoutException -> ApiError.NetworkError
            is HttpException -> {
                when (code()) {
                    HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> ApiError.Unauthorized
                    else -> ApiError.ServerError(code(), message())
                }
            }
            else -> ApiError.UnknownError(this)
        }
    }
    
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
                    android.util.Log.i(TAG, "Barrier devices loaded from CACHE (${cachedData.size} devices)")
                    return Result.success(cachedData)
                } else {
                    android.util.Log.i(TAG, "No valid cache found for barrier devices, fetching from REMOTE SERVER")
                }
            } else {
                android.util.Log.i(TAG, "Force reload requested, fetching barrier devices from REMOTE SERVER")
            }
            
            // Fetch from API
            android.util.Log.d(TAG, "Fetching barrier devices from remote server...")
            val response = apiService.getBarrierDevices(
                authorization = authToken,
                accept = "application/json",
                referer = REFERER_URL
            )
            
            val devices = response.data.devices
            android.util.Log.i(TAG, "Barrier devices loaded from REMOTE SERVER (${devices.size} devices)")
            
            // Cache the result
            cacheBarrierDevices(devices)
            android.util.Log.d(TAG, "Barrier devices cached successfully")
            
            Result.success(devices)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to fetch barrier devices from remote server", e)
            // If API call fails, try to return cached data
            val cachedData = getCachedBarrierDevices()
            if (cachedData != null) {
                android.util.Log.i(TAG, "API call failed, falling back to CACHE (${cachedData.size} devices)")
                Result.success(cachedData)
            } else {
                android.util.Log.e(TAG, "No cache available and API call failed")
                Result.failure(e.toApiError())
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
                    android.util.Log.i(TAG, "Intercom devices loaded from CACHE (${cachedData.size} devices)")
                    return Result.success(cachedData)
                } else {
                    android.util.Log.i(TAG, "No valid cache found for intercom devices, fetching from REMOTE SERVER")
                }
            } else {
                android.util.Log.i(TAG, "Force reload requested, fetching intercom devices from REMOTE SERVER")
            }
            
            // Fetch from API
            android.util.Log.d(TAG, "Fetching intercom devices from remote server...")
            val response = apiService.getIntercomDevices(
                authorization = authToken,
                accept = "application/json",
                referer = REFERER_URL
            )
            
            val devices = response.data.devices
            android.util.Log.i(TAG, "Intercom devices loaded from REMOTE SERVER (${devices.size} devices)")
            
            // Cache the result
            cacheIntercomDevices(devices)
            android.util.Log.d(TAG, "Intercom devices cached successfully")
            
            Result.success(devices)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to fetch intercom devices from remote server", e)
            // If API call fails, try to return cached data
            val cachedData = getCachedIntercomDevices()
            if (cachedData != null) {
                android.util.Log.i(TAG, "API call failed, falling back to CACHE (${cachedData.size} devices)")
                Result.success(cachedData)
            } else {
                android.util.Log.e(TAG, "No cache available and API call failed")
                Result.failure(e.toApiError())
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
                referer = REFERER_URL
            )
            
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val error = when (response.code()) {
                    HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> ApiError.Unauthorized
                    else -> ApiError.ServerError(response.code(), response.message())
                }
                Result.failure(error)
            }
        } catch (e: Exception) {
            android.util.Log.e("RtkApiWrapper", "Failed to open device $deviceId", e)
            Result.failure(e.toApiError())
        }
    }
    
    /**
     * Get cached barrier devices if cache is still valid
     */
    private suspend fun getCachedBarrierDevices(): List<Device>? {
        val preferences = context.apiDataStore.data.first()
        val cachedJson = preferences[BARRIER_DEVICES_KEY] ?: run {
            android.util.Log.d(TAG, "No barrier devices cache found")
            return null
        }
        val timestamp = preferences[BARRIER_CACHE_TIMESTAMP_KEY] ?: run {
            android.util.Log.d(TAG, "No barrier devices cache timestamp found")
            return null
        }
        
        // Check if cache is still valid (within 1 day)
        val currentTime = System.currentTimeMillis()
        val cacheAge = currentTime - timestamp
        if (cacheAge > CACHE_LIFETIME_MS) {
            android.util.Log.d(TAG, "Barrier devices cache expired (age: ${cacheAge / 1000 / 60} minutes, max: ${CACHE_LIFETIME_MS / 1000 / 60} minutes)")
            return null // Cache expired
        }
        
        return try {
            val devices = json.decodeFromString(ListSerializer(Device.serializer()), cachedJson)
            android.util.Log.d(TAG, "Barrier devices cache is valid (age: ${cacheAge / 1000 / 60} minutes)")
            devices
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to deserialize cached barrier devices", e)
            null
        }
    }
    
    /**
     * Get cached intercom devices if cache is still valid
     */
    private suspend fun getCachedIntercomDevices(): List<Device>? {
        val preferences = context.apiDataStore.data.first()
        val cachedJson = preferences[INTERCOM_DEVICES_KEY] ?: run {
            android.util.Log.d(TAG, "No intercom devices cache found")
            return null
        }
        val timestamp = preferences[INTERCOM_CACHE_TIMESTAMP_KEY] ?: run {
            android.util.Log.d(TAG, "No intercom devices cache timestamp found")
            return null
        }
        
        // Check if cache is still valid (within 1 day)
        val currentTime = System.currentTimeMillis()
        val cacheAge = currentTime - timestamp
        if (cacheAge > CACHE_LIFETIME_MS) {
            android.util.Log.d(TAG, "Intercom devices cache expired (age: ${cacheAge / 1000 / 60} minutes, max: ${CACHE_LIFETIME_MS / 1000 / 60} minutes)")
            return null // Cache expired
        }
        
        return try {
            val devices = json.decodeFromString(ListSerializer(Device.serializer()), cachedJson)
            android.util.Log.d(TAG, "Intercom devices cache is valid (age: ${cacheAge / 1000 / 60} minutes)")
            devices
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to deserialize cached intercom devices", e)
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
            android.util.Log.d(TAG, "Barrier devices cached (${devices.size} devices)")
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
            android.util.Log.d(TAG, "Intercom devices cached (${devices.size} devices)")
        }
    }
}
