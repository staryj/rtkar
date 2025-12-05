package com.ngerasimov.rtkar.ui.login

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

/**
 * Helper object for safe navigation in fragments
 */
object NavigationHelper {
    private const val TAG = "NavigationHelper"
    private const val DEFAULT_RETRY_DELAY_MS = 300L
    private const val DEFAULT_MAX_RETRIES = 3
    
    /**
     * Safely navigate to a destination with retry logic
     * @param fragment The fragment to navigate from
     * @param destinationId The destination ID to navigate to
     * @param verifyGraph If true, verifies the destination exists in the graph before navigating
     * @param maxRetries Maximum number of retry attempts
     * @param retryDelayMs Delay between retries in milliseconds
     * @return true if navigation was successful or queued, false otherwise
     */
    fun navigateSafely(
        fragment: Fragment,
        destinationId: Int,
        verifyGraph: Boolean = false,
        maxRetries: Int = DEFAULT_MAX_RETRIES,
        retryDelayMs: Long = DEFAULT_RETRY_DELAY_MS
    ): Boolean {
        if (!fragment.isAdded || fragment.view == null) {
            Log.w(TAG, "Fragment not attached, cannot navigate to destination $destinationId")
            return false
        }
        
        return try {
            val navController = fragment.findNavController()
            
            // Verify graph if requested
            if (verifyGraph) {
                val graph = navController.graph
                val destinationNode = graph.findNode(destinationId)
                if (destinationNode == null) {
                    Log.w(TAG, "Destination $destinationId not found in navigation graph")
                    return false
                }
            }
            
            navController.navigate(destinationId)
            Log.d(TAG, "Successfully navigated to destination $destinationId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to navigate to destination $destinationId", e)
            false
        }
    }
    
    /**
     * Navigate with retry logic
     * @param fragment The fragment to navigate from
     * @param destinationId The destination ID to navigate to
     * @param verifyGraph If true, verifies the destination exists in the graph before navigating
     * @param maxRetries Maximum number of retry attempts
     * @param retryDelayMs Delay between retries in milliseconds
     * @param retryCount Current retry count (internal use)
     */
    fun navigateWithRetry(
        fragment: Fragment,
        destinationId: Int,
        verifyGraph: Boolean = false,
        maxRetries: Int = DEFAULT_MAX_RETRIES,
        retryDelayMs: Long = DEFAULT_RETRY_DELAY_MS,
        retryCount: Int = 0
    ) {
        if (retryCount >= maxRetries) {
            Log.e(TAG, "Max navigation retries ($maxRetries) reached for destination $destinationId")
            return
        }
        
        if (navigateSafely(fragment, destinationId, verifyGraph)) {
            return // Success
        }
        
        // Retry after delay
        fragment.view?.postDelayed({
            if (fragment.isAdded) {
                navigateWithRetry(
                    fragment = fragment,
                    destinationId = destinationId,
                    verifyGraph = verifyGraph,
                    maxRetries = maxRetries,
                    retryDelayMs = retryDelayMs,
                    retryCount = retryCount + 1
                )
            }
        }, retryDelayMs)
    }
}
