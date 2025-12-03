package com.ngerasimov.rtkar.data.api

import retrofit2.http.GET
import retrofit2.http.Header

/**
 * Retrofit service interface for RTK API endpoints
 */
interface RtkApiService {
    
    /**
     * Get barrier devices
     * @param authorization Authorization token (Bearer token)
     * @param accept Accept header (application/json)
     * @param referer Referer header (https://key.rt.ru/)
     */
    @GET("api/v2/app/devices/barrier")
    suspend fun getBarrierDevices(
        @Header("Authorization") authorization: String,
        @Header("Accept") accept: String = "application/json",
        @Header("Referer") referer: String = "https://key.rt.ru/"
    ): RtkApiResponse<DevicesData>
    
    /**
     * Get intercom devices
     * @param authorization Authorization token (Bearer token)
     * @param accept Accept header (application/json)
     * @param referer Referer header (https://key.rt.ru/)
     */
    @GET("api/v2/app/devices/intercom")
    suspend fun getIntercomDevices(
        @Header("Authorization") authorization: String,
        @Header("Accept") accept: String = "application/json",
        @Header("Referer") referer: String = "https://key.rt.ru/"
    ): RtkApiResponse<DevicesData>
}
