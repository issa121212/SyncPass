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
import kotlinx.coroutines.flow.MutableStateFlow

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

    @Test
    fun arranqueRestauraSesionVigenteYRecrearNoBorraNiReescribeLosTokens() {
        val saved = MutableStateFlow<CognitoTokens?>(tokens)
        `when`(session.hasValidSession()).thenReturn(true)
        `when`(session.tokens).thenReturn(saved)
        val vm = SessionViewModel(session, accounts)
        val root = ViewModelStore().apply { put("session", vm) }
        val previous = requireNotNull(vm.owner.value)
        val screen = ScreenViewModel().also { previous.viewModelStore.put("screen", it) }
        vm.open(tokens)
        assertSame(previous, vm.owner.value)
        root.clear()
        assertTrue(screen.released)
        val recreated = SessionViewModel(session, accounts)
        val next = ViewModelStore().apply { put("session", recreated) }
        try {
            assertNotNull(recreated.owner.value)
            assertNotSame(previous, recreated.owner.value)
            assertFalse(mockingDetails(session).invocations.any { it.method.name == "save" })
            verify(session, never()).clear()
            verify(accounts, never()).clear()
        } finally { next.clear() }
    }

    @Test
    fun cerrarSesionRestauradaLiberaPantallasYNoReabreAlCrearOtroViewModel() {
        val saved = MutableStateFlow<CognitoTokens?>(tokens.copy(idToken = "id-ana"))
        `when`(session.hasValidSession()).thenAnswer { saved.value != null }
        `when`(session.tokens).thenReturn(saved)
        `when`(session.currentIdToken()).thenAnswer { saved.value?.idToken }
        doAnswer { saved.value = null; null }.`when`(session).clear()
        val vm = SessionViewModel(session, accounts)
        val root = ViewModelStore().apply { put("session", vm) }
        val screen = ScreenViewModel().also { requireNotNull(vm.owner.value).viewModelStore.put("screen", it) }
        assertEquals("id-ana", vm.idTokenForLogout())
        vm.close()
        assertTrue(screen.released)
        assertNull(vm.owner.value)
        assertNull(vm.idTokenForLogout())
        root.clear()
        val restarted = SessionViewModel(session, accounts)
        val next = ViewModelStore().apply { put("session", restarted) }
        try { assertNull(restarted.owner.value) }
        finally { next.clear() }
    }

    @Test
    fun tokenPersistidoVencidoNoAbreElAreaPrivada() {
        `when`(session.hasValidSession()).thenReturn(false)
        `when`(session.tokens).thenReturn(MutableStateFlow<CognitoTokens?>(tokens))
        val vm = SessionViewModel(session, accounts)
        val root = ViewModelStore().apply { put("session", vm) }
        try {
            assertNull(vm.owner.value)
            verify(accounts, never()).current()
            assertFalse(mockingDetails(session).invocations.any { it.method.name == "save" })
        } finally { root.clear() }
    }
}
