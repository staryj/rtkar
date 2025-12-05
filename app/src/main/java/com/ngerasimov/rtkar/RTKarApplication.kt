package com.ngerasimov.rtkar

import android.app.Application

/**
 * Application class for RTKar
 * Note: Hilt is configured but Application class doesn't use @HiltAndroidApp
 * due to Kotlin 2.0 compatibility issues. Dependencies are injected manually.
 */
class RTKarApplication : Application()
