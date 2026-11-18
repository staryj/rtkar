package com.ngerasimov.rtkar.shortcut

import com.ngerasimov.rtkar.data.Entity
import com.ngerasimov.rtkar.data.EntityType

enum class OpenBarrierRejectReason {
    MissingId,
    NotSelected,
    MissingToken
}

sealed class OpenBarrierDecision {
    data class Open(val deviceId: String, val title: String) : OpenBarrierDecision()
    data class Reject(val reason: OpenBarrierRejectReason) : OpenBarrierDecision()
}

object OpenBarrierEvaluator {
    fun decide(
        deviceId: String?,
        entities: List<Entity>,
        token: String?,
        requestedTitle: String? = null
    ): OpenBarrierDecision {
        if (deviceId.isNullOrBlank()) {
            return OpenBarrierDecision.Reject(OpenBarrierRejectReason.MissingId)
        }
        val barrier = entities.find { it.deviceId == deviceId && it.type == EntityType.BARRIER }
            ?: return OpenBarrierDecision.Reject(OpenBarrierRejectReason.NotSelected)
        if (token.isNullOrBlank()) {
            return OpenBarrierDecision.Reject(OpenBarrierRejectReason.MissingToken)
        }
        val title = barrier.title.ifBlank { requestedTitle.orEmpty() }
        return OpenBarrierDecision.Open(deviceId, title)
    }
}
