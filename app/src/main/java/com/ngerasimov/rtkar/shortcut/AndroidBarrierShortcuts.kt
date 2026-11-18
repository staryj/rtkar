package com.ngerasimov.rtkar.shortcut

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.util.Log
import com.ngerasimov.rtkar.R
import com.ngerasimov.rtkar.ui.shortcut.OpenBarrierActivity

class AndroidBarrierShortcuts(
    private val context: Context
) : BarrierShortcutStore {

    override fun isPinSupported(): Boolean {
        return shortcutManager()?.isRequestPinShortcutSupported == true
    }

    override fun isPinned(deviceId: String): Boolean {
        val shortcutId = BarrierShortcutIds.of(deviceId)
        return pinnedShortcuts().any { it.id == shortcutId }
    }

    override fun pinnedDeviceIds(): Set<String> {
        return pinnedShortcuts().mapNotNull { shortcut ->
            shortcut.intent?.getStringExtra(OpenBarrierActivity.EXTRA_DEVICE_ID)
        }.toSet()
    }

    override fun execute(commands: List<ShortcutCommand>) {
        if (commands.isEmpty()) return
        val manager = shortcutManager() ?: return
        commands.forEach { command ->
            try {
                when (command) {
                    is ShortcutCommand.RequestPin -> {
                        manager.requestPinShortcut(buildShortcut(command.deviceId, command.title), null)
                    }
                    is ShortcutCommand.Update -> {
                        if (isPinned(command.deviceId)) {
                            manager.updateShortcuts(listOf(buildShortcut(command.deviceId, command.title)))
                        }
                    }
                    is ShortcutCommand.Enable -> enable(manager, command.deviceId)
                    is ShortcutCommand.Disable -> disable(manager, command.deviceId)
                    ShortcutCommand.PinUnsupported -> Unit
                }
            } catch (e: RuntimeException) {
                Log.e(TAG, "Shortcut operation failed for $command", e)
            }
        }
    }

    private fun enable(manager: ShortcutManager, deviceId: String) {
        val shortcutId = BarrierShortcutIds.of(deviceId)
        if (manager.pinnedShortcuts.any { it.id == shortcutId }) {
            manager.enableShortcuts(listOf(shortcutId))
        }
    }

    private fun disable(manager: ShortcutManager, deviceId: String) {
        val shortcutId = BarrierShortcutIds.of(deviceId)
        if (manager.pinnedShortcuts.any { it.id == shortcutId }) {
            manager.disableShortcuts(
                listOf(shortcutId),
                context.getString(R.string.shortcut_barrier_disabled)
            )
        }
    }

    private fun buildShortcut(deviceId: String, title: String): ShortcutInfo {
        val label = title.ifBlank { context.getString(R.string.shortcut_barrier_fallback_label) }
        val intent = Intent(context, OpenBarrierActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(OpenBarrierActivity.EXTRA_DEVICE_ID, deviceId)
            putExtra(OpenBarrierActivity.EXTRA_TITLE, label)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return ShortcutInfo.Builder(context, BarrierShortcutIds.of(deviceId))
            .setShortLabel(label)
            .setLongLabel(label)
            .setIcon(Icon.createWithResource(context, R.drawable.ic_barrier_shortcut))
            .setIntent(intent)
            .build()
    }

    private fun pinnedShortcuts() = shortcutManager()?.pinnedShortcuts.orEmpty()

    private fun shortcutManager(): ShortcutManager? {
        return context.getSystemService(ShortcutManager::class.java)
    }

    companion object {
        private const val TAG = "BarrierShortcuts"
    }
}
