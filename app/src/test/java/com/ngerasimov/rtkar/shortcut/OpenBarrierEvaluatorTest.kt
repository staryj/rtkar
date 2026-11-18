package com.ngerasimov.rtkar.shortcut

import com.ngerasimov.rtkar.data.Entity
import com.ngerasimov.rtkar.data.EntityType
import org.junit.Assert.assertEquals
import org.junit.Test

class OpenBarrierEvaluatorTest {

    private val barriers = listOf(
        Entity(EntityType.BARRIER, "Северный", "barrier-1"),
        Entity(EntityType.DOOR, "Подъезд", "door-1")
    )

    @Test
    fun `opens a selected barrier when the token is present`() {
        val decision = OpenBarrierEvaluator.decide(
            deviceId = "barrier-1",
            entities = barriers,
            token = "token"
        )

        assertEquals(OpenBarrierDecision.Open("barrier-1", "Северный"), decision)
    }

    @Test
    fun `uses the shortcut title when the stored title is blank`() {
        val decision = OpenBarrierEvaluator.decide(
            deviceId = "barrier-1",
            requestedTitle = "С ярлыка",
            entities = listOf(Entity(EntityType.BARRIER, "", "barrier-1")),
            token = "token"
        )

        assertEquals(OpenBarrierDecision.Open("barrier-1", "С ярлыка"), decision)
    }

    @Test
    fun `rejects a missing device id`() {
        val decision = OpenBarrierEvaluator.decide(
            deviceId = null,
            entities = barriers,
            token = "token"
        )

        assertEquals(OpenBarrierDecision.Reject(OpenBarrierRejectReason.MissingId), decision)
    }

    @Test
    fun `rejects a barrier that is no longer selected`() {
        val decision = OpenBarrierEvaluator.decide(
            deviceId = "other",
            entities = barriers,
            token = "token"
        )

        assertEquals(OpenBarrierDecision.Reject(OpenBarrierRejectReason.NotSelected), decision)
    }

    @Test
    fun `rejects a selected door`() {
        val decision = OpenBarrierEvaluator.decide(
            deviceId = "door-1",
            entities = barriers,
            token = "token"
        )

        assertEquals(OpenBarrierDecision.Reject(OpenBarrierRejectReason.NotSelected), decision)
    }

    @Test
    fun `rejects a missing token`() {
        val decision = OpenBarrierEvaluator.decide(
            deviceId = "barrier-1",
            entities = barriers,
            token = "  "
        )

        assertEquals(OpenBarrierDecision.Reject(OpenBarrierRejectReason.MissingToken), decision)
    }
}
