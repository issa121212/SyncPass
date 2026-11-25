package com.rojas.syncpass.di

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class AccountScopeRegistryTest {
    @Test
    fun cambiarDeCuentaDescartaLasDependenciasAnteriores() {
        val creadas = mutableListOf<String>()
        val registry = AccountScopeRegistry { key ->
            creadas += key
            Any()
        }
        val primera = registry.get("issuer|ana")
        assertSame(primera, registry.get("issuer|ana"))
        assertNotSame(primera, registry.get("issuer|pedro"))
        assertNotSame(primera, registry.get("issuer|ana"))
        assertEquals(listOf("issuer|ana", "issuer|pedro", "issuer|ana"), creadas)
    }

    @Test
    fun cerrarSesionObligaACrearUnNuevoConjuntoAunqueSeRepitaLaCuenta() {
        val registry = AccountScopeRegistry { Any() }
        val anterior = registry.get("issuer|ana")
        registry.clear()
        assertNotSame(anterior, registry.get("issuer|ana"))
    }

    @Test
    fun solicitudesConcurrentesCompartenUnSoloConjuntoDeLaCuenta() {
        val creaciones = AtomicInteger()
        val registry = AccountScopeRegistry {
            creaciones.incrementAndGet()
            Any()
        }
        val executor = Executors.newFixedThreadPool(4)
        try {
            val futures = executor.invokeAll(List(12) { Callable { registry.get("issuer|ana") } })
            val first = futures.first().get(5, TimeUnit.SECONDS)
            futures.forEach { assertSame(first, it.get(5, TimeUnit.SECONDS)) }
            assertEquals(1, creaciones.get())
        } finally {
            executor.shutdownNow()
        }
    }
}
