package com.rojas.syncpass.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object FechaUtils {

    private val isoConMillis = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val isoLocal = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

    fun mostrar(valor: String?): String {
        if (valor.isNullOrBlank()) return "-"
        val fecha = parsear(valor) ?: return valor
        return SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(fecha)
    }

    fun aIsoLocal(valor: String): String? {
        val limpio = valor.trim()
        val formatos = listOf(
            "yyyy-MM-dd HH:mm",
            "dd-MM-yyyy HH:mm",
            "dd/MM/yyyy HH:mm",
        )
        for (formato in formatos) {
            runCatching {
                val sdf = SimpleDateFormat(formato, Locale.getDefault())
                val fecha = sdf.parse(limpio) ?: return@runCatching
                return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(fecha)
            }
        }
        return runCatching {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            sdf.parse(limpio)
            limpio
        }.getOrNull()
    }

    private fun parsear(valor: String): Date? {
        for (formato in listOf(isoConMillis, iso, isoLocal)) {
            runCatching {
                formato.parse(valor)?.let { return it }
            }
        }
        return null
    }
}
