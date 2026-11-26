package com.rojas.syncpass.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.cognitoauth.CognitoTokens
import com.rojas.syncpass.data.session.SessionStore
import com.rojas.syncpass.di.AccountDependencies
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/** El store del área privada vive hasta logout, cambio de cuenta o cierre de la Activity. */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val accounts: AccountDependencies,
) : ViewModel() {
    private val _owner = MutableStateFlow<ViewModelStoreOwner?>(null)
    val owner = _owner.asStateFlow()
    private var accessToken: String? = null

    init {
        if (sessionStore.hasValidSession()) {
            sessionStore.tokens.value?.let(::activate)
        }
    }

    fun open(tokens: CognitoTokens) {
        if (accessToken == tokens.accessToken && _owner.value != null) return
        sessionStore.save(tokens)
        release()
        activate(tokens)
    }

    private fun activate(tokens: CognitoTokens) {
        accessToken = tokens.accessToken
        _owner.value = object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }

    fun idTokenForLogout(): String? = sessionStore.currentIdToken()

    fun close() {
        sessionStore.clear()
        release()
    }

    private fun release(clearAccount: Boolean = true) {
        _owner.value?.viewModelStore?.clear()
        _owner.value = null
        accessToken = null
        if (clearAccount) accounts.clear()
    }

    override fun onCleared() {
        release(clearAccount = false)
        super.onCleared()
    }
}
