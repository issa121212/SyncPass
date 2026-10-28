package com.rojas.syncpass.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cognitoauth.CognitoAuthManager
import com.cognitoauth.CognitoAuthViewModel

/** Factory sin Hilt: construye CognitoAuthViewModel con su manager. */
class CognitoViewModelFactory(
    private val manager: CognitoAuthManager,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(CognitoAuthViewModel::class.java))
        return CognitoAuthViewModel(manager) as T
    }
}
