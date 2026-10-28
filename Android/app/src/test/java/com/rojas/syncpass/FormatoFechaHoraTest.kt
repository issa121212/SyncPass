package com.rojas.syncpass

import com.rojas.syncpass.core.util.FormatoFechaHora as F
import org.junit.Assert.*
import org.junit.Test

class FormatoFechaHoraTest {
    @Test fun formatsDateAndTime() { assertEquals("2026-12-04", F.fecha("20261204")); assertEquals("14:00", F.hora("1400")) }
    @Test fun acceptsPastedSeparators() { assertEquals("2026-12-04", F.fecha("2026-12-04")); assertEquals("14:00", F.hora("14:00")) }
    @Test fun combinesFieldsForBackend() { assertEquals("2026-12-04T14:00:00", F.combinar("2026-12-04", "14:00")) }
    @Test fun rejectsImpossibleDates() { assertNull(F.combinar("2026-02-30", "14:00")); assertFalse(F.fechaValida("2026-13-01")) }
    @Test fun validatesLeapYears() { assertTrue(F.fechaValida("2028-02-29")); assertFalse(F.fechaValida("2026-02-29")) }
    @Test fun rejectsInvalidOrIncompleteTime() { assertNull(F.combinar("2026-12-04", "24:00")); assertNull(F.combinar("2026-12-04", "14:60")); assertNull(F.combinar("2026-12-04", "14:0")) }
    @Test fun rejectsReversedProjectDates() { assertFalse(F.rangoValido("2026-12-04", "2026-12-03")); assertTrue(F.rangoValido("", "")) }
    @Test fun handlesPartialInput() { assertEquals("2026-1", F.fecha("20261")); assertEquals("14:0", F.hora("140")); assertEquals("", F.fecha("")) }
}
