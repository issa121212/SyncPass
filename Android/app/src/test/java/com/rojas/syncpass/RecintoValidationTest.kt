package com.rojas.syncpass

import com.rojas.syncpass.domain.model.Recinto
import org.junit.Assert.*
import org.junit.Test

class RecintoValidationTest {
    @Test fun validBusinessLocation() { Recinto(nombre = "Sede comunitaria", direccion = "Calle 100", latitud = -33.4, longitud = -70.6).validar() }
    @Test fun requiresNameAndAddress() {
        assertThrows(IllegalArgumentException::class.java) { Recinto(nombre = "", direccion = "Calle").validar() }
        assertThrows(IllegalArgumentException::class.java) { Recinto(nombre = "Sede", direccion = "").validar() }
    }
    @Test fun coordinatesMustBeValidAndPaired() {
        assertThrows(IllegalArgumentException::class.java) { Recinto(nombre = "Sede", direccion = "Calle", latitud = 91.0, longitud = 0.0).validar() }
        assertThrows(IllegalArgumentException::class.java) { Recinto(nombre = "Sede", direccion = "Calle", latitud = 0.0).validar() }
        assertThrows(IllegalArgumentException::class.java) { Recinto(nombre = "Sede", direccion = "Calle", latitud = Double.NaN, longitud = 0.0).validar() }
    }
    @Test fun radiusMustBePositive() {
        assertThrows(IllegalArgumentException::class.java) { Recinto(nombre = "Sede", direccion = "Calle", radioMetros = 0).validar() }
    }
}
