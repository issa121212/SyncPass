package com.rojas.syncpass

import com.rojas.syncpass.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class ProyectoRulesTest {
    @Test fun permiteFechasIgualesOCamposOpcionalesPeroNoFechasInvertidasOInexistentes() {
        ProyectoInput("Proyecto").validar()
        ProyectoInput("Proyecto", fechaInicio = "2026-11-25", fechaFin = "2026-11-25").validar()
        for (input in listOf(
            ProyectoInput("Proyecto", fechaInicio = "2026-11-26", fechaFin = "2026-11-25"),
            ProyectoInput("Proyecto", fechaInicio = "2026-02-30"),
            ProyectoInput("Proyecto", fechaFin = "mañana"),
        )) assertTrue(runCatching { input.validar() }.isFailure)
    }

    @Test fun limitaTextosYEstadosAntesDePersistirUnaSolicitudOffline() {
        ProyectoInput("n".repeat(160), "d".repeat(4000), "a".repeat(80), "u".repeat(160)).validar()
        val valid = ProyectoInput("Proyecto")
        for (input in listOf(valid.copy(nombre = " "), valid.copy(nombre = "n".repeat(161)),
            valid.copy(descripcion = "d".repeat(4001)), valid.copy(areaTematica = "a".repeat(81)),
            valid.copy(unidadVecinal = "u".repeat(161)), valid.copy(estado = "DESCONOCIDO"))) {
            assertTrue(runCatching { input.validar() }.isFailure)
        }
    }
}
