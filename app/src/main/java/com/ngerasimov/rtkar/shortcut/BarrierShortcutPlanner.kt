package com.ngerasimov.rtkar.shortcut

import com.ngerasimov.rtkar.data.Entity
import com.ngerasimov.rtkar.data.EntityType

sealed class ShortcutCommand {
    data class RequestPin(val deviceId: String, val title: String) : ShortcutCommand()
    data class Update(val deviceId: String, val title: String) : ShortcutCommand()
    data class Enable(val deviceId: String) : ShortcutCommand()
    data class Disable(val deviceId: String) : ShortcutCommand()
    data object PinUnsupported : ShortcutCommand()
}

data class TitleSync(
    val entities: List<Entity>,
    val shortcutUpdates: List<ShortcutCommand.Update>
)

object BarrierShortcutIds {
    private val unsafeCharacters = Regex("[^A-Za-z0-9._-]")

    fun of(deviceId: String): String = "barrier." + unsafeCharacters.replace(deviceId, "_")
}

object BarrierShortcutPlanner {
    fun onToggle(
        type: EntityType,
        deviceId: String,
        title: String,
        selected: Boolean,
        pinSupported: Boolean,
        alreadyPinned: Boolean
    ): List<ShortcutCommand> {
        if (type != EntityType.BARRIER) return emptyList()
        if (!selected) return listOf(ShortcutCommand.Disable(deviceId))
        if (!pinSupported) return listOf(ShortcutCommand.PinUnsupported)
        if (!alreadyPinned) return listOf(ShortcutCommand.RequestPin(deviceId, title))
        return listOf(
            ShortcutCommand.Enable(deviceId),
            ShortcutCommand.Update(deviceId, title)
        )
    }

    fun onLoggedOut(pinnedDeviceIds: Set<String>): List<ShortcutCommand> {
        return pinnedDeviceIds.sorted().map { ShortcutCommand.Disable(it) }
    }

    fun onAuthorized(
        selectedBarriers: List<Entity>,
        pinnedDeviceIds: Set<String>
    ): List<ShortcutCommand> {
        return selectedBarriers.mapNotNull { entity ->
            if (entity.type == EntityType.BARRIER && entity.deviceId in pinnedDeviceIds) {
                ShortcutCommand.Enable(entity.deviceId)
            } else {
                null
            }
        }
    }

    fun syncTitles(
        entities: List<Entity>,
        namesByDeviceId: Map<String, String>,
        pinnedDeviceIds: Set<String>
    ): TitleSync {
        val updated = entities.map { entity ->
            val newName = namesByDeviceId[entity.deviceId]
            if (entity.type == EntityType.BARRIER && newName != null && newName != entity.title) {
                entity.copy(title = newName)
            } else {
                entity
            }
        }
        val shortcutUpdates = updated.mapNotNull { entity ->
            val previous = entities.find { it.type == entity.type && it.deviceId == entity.deviceId }
            if (
                entity.type == EntityType.BARRIER &&
                entity.deviceId in pinnedDeviceIds &&
                previous != null &&
                previous.title != entity.title
            ) {
                ShortcutCommand.Update(entity.deviceId, entity.title)
            } else {
                null
            }
        }
        return TitleSync(updated, shortcutUpdates)
    }
}
