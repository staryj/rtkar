package com.ngerasimov.rtkar.shortcut

interface BarrierShortcutStore {
    fun isPinSupported(): Boolean

    fun isPinned(deviceId: String): Boolean

    fun pinnedDeviceIds(): Set<String>

    fun execute(commands: List<ShortcutCommand>)
}
