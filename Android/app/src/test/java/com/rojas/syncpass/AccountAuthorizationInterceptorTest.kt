package com.rojas.syncpass

import com.rojas.syncpass.data.remote.AccountAuthorizationInterceptor
import com.rojas.syncpass.data.remote.SessionInvalidIOException
import com.rojas.syncpass.data.session.SessionStore
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*

class AccountAuthorizationInterceptorTest {
    @Test
    fun solicitudUsaUnicamenteElTokenAutorizadoParaSuCuenta() {
        val store = mock(SessionStore::class.java)
        val request = Request.Builder().url("https://example.invalid/api/eventos")
            .header("Authorization", "Bearer anterior").build()
        val response = Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK").build()
        var sent: Request? = null
        val chain = mock(Interceptor.Chain::class.java) { invocation ->
            if (invocation.method.name == "proceed") {
                sent = invocation.getArgument(0)
                response
            } else null
        }
        `when`(store.accessTokenFor("issuer|ana")).thenReturn("token-ana")
        `when`(chain.request()).thenReturn(request)

        AccountAuthorizationInterceptor(store, "issuer|ana").intercept(chain)

        assertEquals("Bearer token-ana", sent?.header("Authorization"))
        assertEquals(request.url, sent?.url)
    }

    @Test
    fun cambioDeCuentaOSesionInvalidaImpideEnviarLaSolicitud() {
        val store = mock(SessionStore::class.java)
        val chain = mock(Interceptor.Chain::class.java)
        `when`(store.accessTokenFor("issuer|ana")).thenReturn(null)
        try {
            AccountAuthorizationInterceptor(store, "issuer|ana").intercept(chain)
            fail("La solicitud debe interrumpirse")
        } catch (_: SessionInvalidIOException) {
            verifyNoInteractions(chain)
        }
    }
}
