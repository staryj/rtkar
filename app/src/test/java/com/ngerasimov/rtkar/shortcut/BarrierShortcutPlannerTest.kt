package com.ngerasimov.rtkar.shortcut

import com.ngerasimov.rtkar.data.Entity
import com.ngerasimov.rtkar.data.EntityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BarrierShortcutPlannerTest {

    @Test
    fun `selecting a barrier requests a home screen pin`() {
        val commands = BarrierShortcutPlanner.onToggle(
            type = EntityType.BARRIER,
            deviceId = "barrier-1",
            title = "Северный",
            selected = true,
            pinSupported = true,
            alreadyPinned = false
        )

        assertEquals(listOf(ShortcutCommand.RequestPin("barrier-1", "Северный")), commands)
    }

    @Test
    fun `selecting a pinned barrier enables and updates it`() {
        val commands = BarrierShortcutPlanner.onToggle(
            type = EntityType.BARRIER,
            deviceId = "barrier-1",
            title = "Северный",
            selected = true,
            pinSupported = true,
            alreadyPinned = true
        )

        assertEquals(
            listOf(
                ShortcutCommand.Enable("barrier-1"),
                ShortcutCommand.Update("barrier-1", "Северный")
            ),
            commands
        )
    }

    @Test
    fun `selecting a barrier reports unsupported pinning without a pin request`() {
        val commands = BarrierShortcutPlanner.onToggle(
            type = EntityType.BARRIER,
            deviceId = "barrier-1",
            title = "Северный",
            selected = true,
            pinSupported = false,
            alreadyPinned = false
        )

        assertEquals(listOf(ShortcutCommand.PinUnsupported), commands)
    }

    @Test
    fun `deselecting a barrier disables its shortcut`() {
        val commands = BarrierShortcutPlanner.onToggle(
            type = EntityType.BARRIER,
            deviceId = "barrier-1",
            title = "Северный",
            selected = false,
            pinSupported = true,
            alreadyPinned = true
        )

        assertEquals(listOf(ShortcutCommand.Disable("barrier-1")), commands)
    }

    @Test
    fun `selecting a door does not change shortcuts`() {
        val commands = BarrierShortcutPlanner.onToggle(
            type = EntityType.DOOR,
            deviceId = "door-1",
            title = "Подъезд",
            selected = true,
            pinSupported = true,
            alreadyPinned = false
        )

        assertTrue(commands.isEmpty())
    }

    @Test
    fun `logout disables every pinned shortcut`() {
        val commands = BarrierShortcutPlanner.onLoggedOut(setOf("a", "b"))

        assertEquals(
            listOf(ShortcutCommand.Disable("a"), ShortcutCommand.Disable("b")),
            commands
        )
    }

    @Test
    fun `login enables pinned shortcuts for selected barriers only`() {
        val commands = BarrierShortcutPlanner.onAuthorized(
            selectedBarriers = listOf(
                Entity(EntityType.BARRIER, "Северный", "a"),
                Entity(EntityType.DOOR, "Подъезд", "door")
            ),
            pinnedDeviceIds = setOf("a", "removed")
        )

        assertEquals(listOf(ShortcutCommand.Enable("a")), commands)
    }

    @Test
    fun `renamed selected barrier updates stored title and pinned shortcut`() {
        val entities = listOf(
            Entity(EntityType.BARRIER, "Старое", "a"),
            Entity(EntityType.DOOR, "Подъезд", "door")
        )

        val sync = BarrierShortcutPlanner.syncTitles(
            entities = entities,
            namesByDeviceId = mapOf("a" to "Новое"),
            pinnedDeviceIds = setOf("a")
        )

        assertEquals(
            listOf(
                Entity(EntityType.BARRIER, "Новое", "a"),
                Entity(EntityType.DOOR, "Подъезд", "door")
            ),
            sync.entities
        )
        assertEquals(listOf(ShortcutCommand.Update("a", "Новое")), sync.shortcutUpdates)
    }

    @Test
    fun `shortcut id stays stable and drops unsafe characters`() {
        assertEquals("barrier.gate_1", BarrierShortcutIds.of("gate/1"))
        assertEquals(BarrierShortcutIds.of("gate/1"), BarrierShortcutIds.of("gate/1"))
    }
}
