package com.ngerasimov.rtkar.data.api

import kotlinx.serialization.Serializable

/**
 * Response wrapper for RTK API endpoints
 */
@Serializable
data class RtkApiResponse<T>(
    val data: T
)

/**
 * Devices response data
 */
@Serializable
data class DevicesData(
    val devices: List<Device>
)

/**
 * Base device information
 */
@Serializable
data class Device(
    val id: String,
    val device_type: String,
    val serial_number: String,
    val device_group: List<String>,
    val utc_offset_minutes: Int,
    val description: String,
    val is_favorite: Boolean,
    val is_active: Boolean,
    val name_by_company: String,
    val name_by_user: String? = null,
    val capabilities: List<Capability>,
    // Intercom-specific fields
    val camera_id: String? = null,
    val accept_concierge_call: Boolean? = null,
    val inter_codes: List<InterCode>? = null,
    val entrance: Int? = null
)

/**
 * Device capability
 */
@Serializable
data class Capability(
    val name: String,
    val setup: Boolean
)

/**
 * Intercom code (only for intercom devices)
 */
@Serializable
data class InterCode(
    val id: Int,
    val code: String,
    val start_date: String,
    val end_date: String? = null,
    val inter_code_type: String
)
