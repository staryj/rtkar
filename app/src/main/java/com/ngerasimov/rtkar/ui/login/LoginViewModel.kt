package com.ngerasimov.rtkar.ui.login

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ngerasimov.rtkar.data.StorageManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val storageManager = StorageManager(application)

    private val _loginResult = MutableLiveData<Boolean>()
    val loginResult: LiveData<Boolean> = _loginResult

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
        _loginResult.value = true
    }

    /**
     * Perform login with username and password
     * For now, this is a simple implementation that accepts any non-empty credentials
     * In a real app, this would validate against a backend service
     */
    suspend fun login(username: String, password: String) {
        // Simple validation - in production, this would call an API
        if (username.isNotEmpty() && password.isNotEmpty()) {
            // Generate a simple token (in production, this would come from the server)
            val token = "auth_token_${username}_${System.currentTimeMillis()}"
            storageManager.saveAuthToken(token)
            _loginResult.value = true
        } else {
            _loginResult.value = false
        }
    }
}