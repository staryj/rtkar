package com.ngerasimov.rtkar.ui.login

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.ngerasimov.rtkar.data.StorageManager
import kotlinx.coroutines.flow.first

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val storageManager = StorageManager(application)

    /**
     * Get the current authentication token
     */
    suspend fun getAuthToken(): String? {
        return storageManager.authToken.first()
    }
    
    /**
     * Get the list of entities
     */
    suspend fun getEntities(): List<com.ngerasimov.rtkar.data.Entity> {
        return storageManager.entities.first()
    }

    /**
     * Save the authentication token from the Authorization header
     */
    suspend fun saveAuthToken(token: String) {
        storageManager.saveAuthToken(token)
        Log.d("LoginViewModel", "Auth token saved: $token")
    }
}