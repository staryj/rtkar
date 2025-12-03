package com.ngerasimov.rtkar.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ngerasimov.rtkar.data.StorageManager
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val storageManager = StorageManager(application)

    private val _loginResult = MutableLiveData<Boolean>()
    val loginResult: LiveData<Boolean> = _loginResult

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