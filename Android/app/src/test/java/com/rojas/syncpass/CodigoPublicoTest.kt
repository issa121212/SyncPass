package com.rojas.syncpass

import com.google.gson.Gson
import com.rojas.syncpass.data.mapper.*
import com.rojas.syncpass.data.remote.*
import com.rojas.syncpass.domain.model.codigoPublicoVisible
import org.junit.Assert.*
import org.junit.Test

class CodigoPublicoTest {
    @Test fun respuestasConNombresIgualesConservanCodigosDistintosEnTodosLosMappers() {
        val gson = Gson()
        val projects = gson.fromJson("""[
            {"id":5,"nombre":"Reciclaje","estado":"PLANIFICADO","codigoPublico":"PRY-ABCDEF123456"},
            {"id":6,"nombre":"Reciclaje","estado":"PLANIFICADO","codigoPublico":"PRY-ABCDEF654321"}
        ]""", Array<ProyectoResponse>::class.java)
        val domains = projects.map { it.toEntity().toDomain() }
        assertEquals(listOf("Reciclaje", "Reciclaje"), domains.map { it.nombre })
        assertEquals(listOf("PRY-ABCDEF123456", "PRY-ABCDEF654321"), domains.map { it.codigoPublico })
        assertEquals(projects.map { it.id }, domains.map { it.toEntity().id })
        assertEquals(domains, domains.map { it.toEntity().toDomain() })
        val event = gson.fromJson("""{"id":9,"proyectoId":5,"nombre":"Taller","proyectoNombre":"Reciclaje",
            "fechaHora":"2026-12-04T14:30:00","estado":"PROGRAMADO","codigoPublico":"EVT-123456ABCDEF",
            "qrContenido":"EVENTO_9"}""", EventoResponse::class.java).toEntity().toDomain()
        assertEquals("EVT-123456ABCDEF", event.toEntity().toDomain().codigoPublico)
        assertEquals(5L, event.proyectoId)
        assertEquals("EVENTO_9", event.qrContenido)
    }

    @Test fun backendAnteriorYPendientesNoMuestranIdsComoCodigoPublico() {
        val old = Gson().fromJson("""{"id":7,"nombre":"Proyecto anterior","estado":"PLANIFICADO"}""",
            ProyectoResponse::class.java).toEntity().toDomain()
        assertNull(old.codigoPublico)
        assertEquals("Código no disponible", codigoPublicoVisible(old.codigoPublico, old.syncState))
        val pending = old.copy(id = -21, syncState = "PENDIENTE")
        assertEquals("Código pendiente de sincronización", codigoPublicoVisible(pending.codigoPublico, pending.syncState))
        assertEquals("Código no asignado", codigoPublicoVisible(null, "RECHAZADO"))
        assertEquals("PRY-ABCDEF123456", codigoPublicoVisible("PRY-ABCDEF123456", "CONFIRMADO"))
    }
}
