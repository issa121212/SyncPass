package com.rojas.syncpass.core.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object FormatoFechaHora {
    fun digitos(valor: String, max: Int) = valor.filter { it in '0'..'9' }.take(max)
    fun fecha(valor: String): String = separar(digitos(valor, 8), listOf(4, 6), '-')
    fun hora(valor: String): String = separar(digitos(valor, 4), listOf(2), ':')
    private fun separar(d: String, cortes: List<Int>, separador: Char) = buildString {
        d.forEachIndexed { i, c -> if (i in cortes) append(separador); append(c) }
    }
    fun fechaValida(valor: String): Boolean = valor.length == 10 &&
        runCatching { LocalDate.parse(valor, DateTimeFormatter.ISO_LOCAL_DATE) }.isSuccess
    fun combinar(fecha: String, hora: String): String? {
        if (!fechaValida(fecha) || !hora.matches(Regex("[0-9]{2}:[0-9]{2}"))) return null
        return runCatching {
            LocalDateTime.of(LocalDate.parse(fecha), LocalTime.parse(hora))
                .format(DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss"))
        }.getOrNull()
    }
    fun rangoValido(inicio: String, fin: String): Boolean =
        (inicio.isBlank() || fechaValida(inicio)) && (fin.isBlank() || fechaValida(fin)) &&
            (inicio.isBlank() || fin.isBlank() || !LocalDate.parse(fin).isBefore(LocalDate.parse(inicio)))
}
