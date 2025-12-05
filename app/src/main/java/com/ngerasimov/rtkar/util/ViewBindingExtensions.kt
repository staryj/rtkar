package com.ngerasimov.rtkar.util

import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

/**
 * Extension functions for safe view binding in fragments
 */

/**
 * Execute a block with the binding if it's not null
 * @param binding The nullable binding
 * @param block The block to execute with the binding
 */
inline fun <T : ViewBinding> T?.withBinding(block: T.() -> Unit) {
    this?.block()
}

/**
 * Get binding or return null if fragment view is not available
 */
fun <T : ViewBinding> Fragment.getBindingSafely(binding: T?): T? {
    return if (isAdded && view != null) binding else null
}
