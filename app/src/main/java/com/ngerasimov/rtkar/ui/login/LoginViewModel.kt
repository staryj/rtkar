package com.ngerasimov.rtkar.ui.login

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.ngerasimov.rtkar.data.StorageManager
import com.ngerasimov.rtkar.data.api.RtkApiWrapper
import com.ngerasimov.rtkar.data.repository.DeviceRepository
import kotlinx.coroutines.flow.first

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DeviceRepository(
        apiWrapper = RtkApiWrapper(application),
        storageManager = StorageManager(application)
    )

    /**
     * Get the current authentication token
     */
    suspend fun getAuthToken(): String? {
        return repository.authToken.first()
    }
    
    /**
     * Get the list of entities
     */
    suspend fun getEntities(): List<com.ngerasimov.rtkar.data.Entity> {
        return repository.entities.first()
    }

    /**
     * Save the authentication token from the Authorization header
     */
    suspend fun saveAuthToken(token: String) {
        repository.saveAuthToken(token)
        Log.d("LoginViewModel", "Auth token saved successfully")
    }
}