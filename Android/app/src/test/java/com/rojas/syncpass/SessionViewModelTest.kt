package com.rojas.syncpass

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import com.cognitoauth.CognitoTokens
import com.rojas.syncpass.data.session.SessionStore
import com.rojas.syncpass.di.AccountDependencies
import com.rojas.syncpass.ui.login.SessionViewModel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.*

class SessionViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val session = mock(SessionStore::class.java)
    private val accounts = mock(AccountDependencies::class.java)
    private val tokens = CognitoTokens(accessToken = "token-ana", idToken = null, refreshToken = null, expiresIn = 3600)

    private class ScreenViewModel : ViewModel() {
        var released = false
        override fun onCleared() { released = true }
    }

    @Test
    fun recomponerConLaMismaAutenticacionConservaLosViewModels() {
        val vm = SessionViewModel(session, accounts)
        val root = ViewModelStore().apply { put("session", vm) }
        try {
            vm.open(tokens)
            val owner = requireNotNull(vm.owner.value)
            val screen = ScreenViewModel()
            owner.viewModelStore.put("screen", screen)
            vm.open(tokens)
            assertSame(owner, vm.owner.value)
            assertFalse(screen.released)
            verify(session, times(1)).save(tokens)
        } finally { root.clear() }
    }

    @Test
    fun cambiarDeCuentaLiberaLasPantallasYConstruyeOtroStore() {
        val vm = SessionViewModel(session, accounts)
        val root = ViewModelStore().apply { put("session", vm) }
        try {
            vm.open(tokens)
            val previous = requireNotNull(vm.owner.value)
            val screen = ScreenViewModel()
            previous.viewModelStore.put("screen", screen)
            vm.open(tokens.copy(accessToken = "token-pedro"))
            assertTrue(screen.released)
            assertNotSame(previous, vm.owner.value)
            verify(accounts, times(2)).clear()
        } finally { root.clear() }
    }

    @Test
    fun logoutLiberaLasPantallasYLaSiguienteSesionTieneOtroStore() {
        val vm = SessionViewModel(session, accounts)
        val root = ViewModelStore().apply { put("session", vm) }
        try {
            vm.open(tokens)
            val previous = requireNotNull(vm.owner.value)
            val screen = ScreenViewModel()
            previous.viewModelStore.put("screen", screen)
            vm.close()
            assertNull(vm.owner.value)
            assertTrue(screen.released)
            verify(session).clear()
            vm.open(tokens)
            assertNotSame(previous, vm.owner.value)
        } finally { root.clear() }
    }
}
