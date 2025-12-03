package com.ngerasimov.rtkar.data.api

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

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
    
    /**
     * Open a device (barrier or door)
     * @param id Device ID
     * @param authorization Authorization token (Bearer token)
     * @param accept Accept header (application/json)
     * @param referer Referer header (https://key.rt.ru/)
     */
    @POST("api/v2/app/devices/{id}/open")
    suspend fun openDevice(
        @Path("id") id: String,
        @Header("Authorization") authorization: String,
        @Header("Accept") accept: String = "application/json",
        @Header("Referer") referer: String = "https://key.rt.ru/"
    ): retrofit2.Response<Unit>
}
