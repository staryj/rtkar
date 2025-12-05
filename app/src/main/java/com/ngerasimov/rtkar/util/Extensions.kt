package com.ngerasimov.rtkar.util

import android.content.Context
import android.view.View
import androidx.fragment.app.Fragment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Extension functions for common operations
 */

/**
 * Check if a string is not null or blank
 */
fun String?.isNotNullOrBlank(): Boolean = !isNullOrBlank()

/**
 * Get first value from Flow synchronously (use with caution)
 */
suspend fun <T> Flow<T>.firstOrNull(): T? {
    return try {
        first()
    } catch (e: Exception) {
        null
    }
}

/**
 * Show view if condition is true, hide otherwise
 */
fun View.showIf(condition: Boolean) {
    visibility = if (condition) View.VISIBLE else View.GONE
}

/**
 * Hide view if condition is true, show otherwise
 */
fun View.hideIf(condition: Boolean) {
    visibility = if (condition) View.GONE else View.VISIBLE
}

/**
 * Check if fragment is in a valid state for operations
 */
fun Fragment.isValidState(): Boolean {
    return isAdded && view != null && !isDetached
}
