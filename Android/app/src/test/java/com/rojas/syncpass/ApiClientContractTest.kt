package com.rojas.syncpass

import com.rojas.syncpass.data.remote.*
import com.rojas.syncpass.data.session.SessionStore
import com.sun.net.httpserver.HttpServer
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.*
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList

/** HTTP real en loopback con respuestas sintéticas; nunca conecta con AWS. */
class ApiClientContractTest {
    @Test fun retrofitConservaContratoAutorizacionYDatosDeCaptura() = runBlocking {
        val received = CopyOnWriteArrayList<Triple<String,String,String>>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1",0),0)
        server.createContext("/") { exchange ->
            val body = exchange.requestBody.bufferedReader().use { it.readText() }
            received.add(Triple(exchange.requestURI.path, exchange.requestHeaders.getFirst("Authorization"), body))
            val response = when(exchange.requestURI.path) {
                "/api/auth/me" -> """{"id":3,"cognitoSub":"test","email":"ana@example.invalid","nombre":"Ana","roles":["DOCENTE"]}"""
                "/api/eventos/7/asistencias/escanear" -> """{"id":8,"eventoId":7,"estudiante":{"id":4,"nombre":"Luis","email":"luis@example.invalid"},"esPresente":true,"marcadoEn":"2026-12-04T14:00:00Z","latitud":-33.45,"longitud":-70.66,"precisionMetros":12.0,"origen":"QR"}"""
                else -> """{"id":7,"proyectoId":1,"nombre":"Taller","fechaHora":"2026-12-04T14:00:00","codigoPublico":"EVT-ABCDEF123456"}"""
            }.toByteArray()
            exchange.responseHeaders.add("Content-Type","application/json")
            exchange.sendResponseHeaders(200,response.size.toLong())
            exchange.responseBody.use { it.write(response) }
        }
        server.start()
        try {
            val session = mock(SessionStore::class.java)
            `when`(session.accessTokenFor("issuer|a")).thenReturn("token-sintetico")
            val api = ApiClient.crear("http://127.0.0.1:${server.address.port}/",session,"issuer|a")
            assertEquals(listOf("DOCENTE"), api.me().roles)
            val event = api.evento(7)
            assertEquals("EVT-ABCDEF123456",event.codigoPublico)
            val request = AsistenciaRequest(codigoQr = "QR_DE_PRUEBA", marcadoEn = "2026-12-04T14:00:00Z", latitud = -33.45,
                longitud = -70.66, precisionMetros = 12.0, clientUuid = "captura-unica", ubicacionEn = "2026-12-04T13:59:59Z")
            val attendance = api.escanear(7,request)
            assertTrue(attendance.esPresente)
            assertEquals("Luis",attendance.estudiante?.nombre)
            assertEquals(12.0,attendance.precisionMetros)
            assertTrue(received.all { it.second == "Bearer token-sintetico" })
            val sent = JsonParser.parseString(received.last().third).asJsonObject
            assertEquals("captura-unica",sent.get("clientUuid").asString)
            assertEquals(-33.45,sent.get("latitud").asDouble,0.0)
            assertEquals("2026-12-04T13:59:59Z",sent.get("ubicacionEn").asString)
            `when`(session.accessTokenFor("issuer|a")).thenReturn(null)
            try { api.evento(7); fail("Cuenta inválida debe bloquear HTTP") } catch (_: SessionInvalidIOException) { }
            assertEquals(3,received.size)
        } finally { server.stop(0) }
    }
}
