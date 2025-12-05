package com.ngerasimov.rtkar.data.api

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import retrofit2.HttpException

private val Context.apiDataStore: DataStore<Preferences> by preferencesDataStore(name = "rtk_api_cache")

/**
 * Device type enum for API endpoints and caching
 */
private enum class DeviceType(val endpoint: String, val cacheKey: String, val timestampKey: String) {
    BARRIER("barrier", "barrier_devices", "barrier_cache_timestamp"),
    INTERCOM("intercom", "intercom_devices", "intercom_cache_timestamp")
}

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
    ): Result<List<Device>> = getDevices(DeviceType.BARRIER, authToken, forceReload)
    
    /**
     * Get intercom devices with caching
     * @param authToken Authentication token
     * @param forceReload If true, bypass cache and fetch from API
     * @return List of intercom devices
     */
    suspend fun getIntercomDevices(
        authToken: String,
        forceReload: Boolean = false
    ): Result<List<Device>> = getDevices(DeviceType.INTERCOM, authToken, forceReload)
    
    /**
     * Generic method to fetch devices with caching
     * @param deviceType Type of device to fetch
     * @param authToken Authentication token
     * @param forceReload If true, bypass cache and fetch from API
     * @return List of devices
     */
    private suspend fun getDevices(
        deviceType: DeviceType,
        authToken: String,
        forceReload: Boolean
    ): Result<List<Device>> {
        return try {
            // Check cache if not forcing reload
            if (!forceReload) {
                val cachedData = getCachedDevices(deviceType)
                if (cachedData != null) {
                    android.util.Log.i(TAG, "${deviceType.name} devices loaded from CACHE (${cachedData.size} devices)")
                    return Result.success(cachedData)
                } else {
                    android.util.Log.i(TAG, "No valid cache found for ${deviceType.name.lowercase()} devices, fetching from REMOTE SERVER")
                }
            } else {
                android.util.Log.i(TAG, "Force reload requested, fetching ${deviceType.name.lowercase()} devices from REMOTE SERVER")
            }
            
            // Fetch from API
            android.util.Log.d(TAG, "Fetching ${deviceType.name.lowercase()} devices from remote server...")
            val devices = fetchDevicesFromApi(deviceType, authToken)
            android.util.Log.i(TAG, "${deviceType.name} devices loaded from REMOTE SERVER (${devices.size} devices)")
            
            // Cache the result
            cacheDevices(deviceType, devices)
            android.util.Log.d(TAG, "${deviceType.name} devices cached successfully")
            
            Result.success(devices)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to fetch ${deviceType.name.lowercase()} devices from remote server", e)
            // If API call fails, try to return cached data
            val cachedData = getCachedDevices(deviceType)
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
     * Fetch devices from API
     * @param deviceType Type of device to fetch
     * @param authToken Authentication token
     * @return List of devices
     */
    private suspend fun fetchDevicesFromApi(
        deviceType: DeviceType,
        authToken: String
    ): List<Device> {
        val request = Request.Builder()
            .url("${BASE_URL}api/v2/app/devices/${deviceType.endpoint}")
            .header("Authorization", authToken)
            .header("Accept", "application/json")
            .header("Referer", REFERER_URL)
            .get()
            .build()
        
        val response = withContext(Dispatchers.IO) {
            okHttpClient.newCall(request).execute()
        }
        
        if (!response.isSuccessful) {
            response.close()
            when (response.code) {
                HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> throw ApiError.Unauthorized
                else -> throw IOException("HTTP ${response.code}: ${response.message}")
            }
        }
        
        val responseBody = response.body ?: throw IOException("Response body is null")
        val responseText = withContext(Dispatchers.IO) {
            responseBody.string()
        }
        val apiResponse = json.decodeFromString(DevicesApiResponse.serializer(), responseText)
        return apiResponse.data.devices
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
            // Use OkHttp directly to avoid converter type extraction issues
            val request = Request.Builder()
                .url("${BASE_URL}api/v2/app/devices/$deviceId/open")
                .header("Authorization", authToken)
                .header("Accept", "application/json")
                .header("Referer", REFERER_URL)
                .post(okhttp3.RequestBody.create("application/json".toMediaType(), ""))
                .build()
            
            val response = withContext(Dispatchers.IO) {
                okHttpClient.newCall(request).execute()
            }
            
            if (response.isSuccessful) {
                response.close()
                Result.success(Unit)
            } else {
                response.close()
                val error = when (response.code) {
                    HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> ApiError.Unauthorized
                    else -> ApiError.ServerError(response.code, response.message)
                }
                Result.failure(error)
            }
        } catch (e: Exception) {
            android.util.Log.e("RtkApiWrapper", "Failed to open device $deviceId", e)
            Result.failure(e.toApiError())
        }
    }
    
    /**
     * Get cached devices if cache is still valid
     * @param deviceType Type of device to retrieve from cache
     * @return Cached devices or null if cache is invalid or expired
     */
    private suspend fun getCachedDevices(deviceType: DeviceType): List<Device>? {
        val preferences = context.apiDataStore.data.first()
        val devicesKey = stringPreferencesKey(deviceType.cacheKey)
        val timestampKey = longPreferencesKey(deviceType.timestampKey)
        
        val cachedJson = preferences[devicesKey] ?: run {
            android.util.Log.d(TAG, "No ${deviceType.name.lowercase()} devices cache found")
            return null
        }
        val timestamp = preferences[timestampKey] ?: run {
            android.util.Log.d(TAG, "No ${deviceType.name.lowercase()} devices cache timestamp found")
            return null
        }
        
        // Check if cache is still valid (within 1 day)
        val currentTime = System.currentTimeMillis()
        val cacheAge = currentTime - timestamp
        if (cacheAge > CACHE_LIFETIME_MS) {
            android.util.Log.d(TAG, "${deviceType.name} devices cache expired (age: ${cacheAge / 1000 / 60} minutes, max: ${CACHE_LIFETIME_MS / 1000 / 60} minutes)")
            return null // Cache expired
        }
        
        return try {
            val devices = json.decodeFromString(ListSerializer(Device.serializer()), cachedJson)
            android.util.Log.d(TAG, "${deviceType.name} devices cache is valid (age: ${cacheAge / 1000 / 60} minutes)")
            devices
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to deserialize cached ${deviceType.name.lowercase()} devices", e)
            null
        }
    }
    
    /**
     * Cache devices with current timestamp
     * @param deviceType Type of device being cached
     * @param devices List of devices to cache
     */
    private suspend fun cacheDevices(deviceType: DeviceType, devices: List<Device>) {
        context.apiDataStore.edit { preferences ->
            val devicesJson = json.encodeToString(ListSerializer(Device.serializer()), devices)
            val devicesKey = stringPreferencesKey(deviceType.cacheKey)
            val timestampKey = longPreferencesKey(deviceType.timestampKey)
            preferences[devicesKey] = devicesJson
            preferences[timestampKey] = System.currentTimeMillis()
            android.util.Log.d(TAG, "${deviceType.name} devices cached (${devices.size} devices)")
        }
    }
}
