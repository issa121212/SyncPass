package com.rojas.syncpass.ui.event

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rojas.syncpass.core.util.FormatoFechaHora
import com.rojas.syncpass.domain.model.*
import com.rojas.syncpass.ui.components.FechaCampo

/** Small steps keep the primary actions visible; scrolling remains available for large text/IME. */
@Composable
fun EventoEditorDialog(
    proyectos: List<Proyecto>,
    onDismiss: () -> Unit,
    onGuardar: (EventoInput) -> Unit,
    inicial: Evento? = null,
    recintos: List<Recinto> = emptyList(),
    guardando: Boolean = false,
    errorGuardado: String? = null,
) {
    var paso by rememberSaveable { mutableIntStateOf(0) }
    var proyectoId by rememberSaveable { mutableLongStateOf(inicial?.proyectoId ?: proyectos.firstOrNull()?.id ?: 0) }
    var nombre by rememberSaveable { mutableStateOf(inicial?.nombre.orEmpty()) }
    var descripcion by rememberSaveable { mutableStateOf(inicial?.descripcion.orEmpty()) }
    var fecha by rememberSaveable { mutableStateOf(FormatoFechaHora.digitos(inicial?.fechaHora?.substringBefore('T').orEmpty(), 8)) }
    var hora by rememberSaveable { mutableStateOf(FormatoFechaHora.digitos(inicial?.fechaHora?.substringAfter('T')?.take(5).orEmpty(), 4)) }
    var cupos by rememberSaveable { mutableStateOf((inicial?.cupos ?: 30).toString()) }
    var estado by rememberSaveable { mutableStateOf(inicial?.estado ?: "PROGRAMADO") }
    var recinto by rememberSaveable { mutableStateOf(inicial?.recinto.orEmpty()) }
    var direccion by rememberSaveable { mutableStateOf(inicial?.direccion.orEmpty()) }
    var latitud by rememberSaveable { mutableStateOf(inicial?.latitud?.toString().orEmpty()) }
    var longitud by rememberSaveable { mutableStateOf(inicial?.longitud?.toString().orEmpty()) }
    var radio by rememberSaveable { mutableStateOf((inicial?.radioMetros ?: 200).toString()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val scroll = rememberScrollState()
    LaunchedEffect(paso) { scroll.scrollTo(0) }

    fun validarPaso() {
        when (paso) {
            0 -> {
                require(proyectos.any { it.id == proyectoId }) { "Selecciona un proyecto a tu cargo" }
                require(nombre.isNotBlank() && nombre.trim().length <= 160) { "Nombre obligatorio, máximo 160 caracteres" }
                require(descripcion.length <= 4000) { "Descripción: máximo 4000 caracteres" }
            }
            1 -> {
                require(FormatoFechaHora.combinar(FormatoFechaHora.fecha(fecha), FormatoFechaHora.hora(hora)) != null) { "Revisa la fecha y hora" }
                val cantidad = cupos.toIntOrNull()
                require(cantidad != null && cantidad >= 0) { "Ingresa cupos válidos (0 = sin límite)" }
                require(inicial == null || cantidad == 0 || cantidad >= inicial.inscritos) { "El cupo no puede ser menor que los inscritos" }
            }
        }
    }
    AlertDialog(
        onDismissRequest = { if (!guardando) onDismiss() },
        title = { Text("${if (inicial == null) "Nuevo evento" else "Editar evento"} · ${paso + 1}/3") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 390.dp).verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (paso) {
                    0 -> {
                        Text("Proyecto", style = MaterialTheme.typography.labelMedium)
                        proyectos.forEach { item ->
                            Row(Modifier.fillMaxWidth().selectable(selected = proyectoId == item.id,
                                enabled = !guardando, role = androidx.compose.ui.semantics.Role.RadioButton,
                                onClick = { proyectoId = item.id }), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                RadioButton(selected = proyectoId == item.id, onClick = null, enabled = !guardando)
                                Text(item.nombre, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                        CampoEvento("Nombre *", nombre, { nombre = it }, !guardando)
                        OutlinedTextField(value = descripcion, onValueChange = { descripcion = it },
                            label = { Text("Descripción") }, maxLines = 3, enabled = !guardando,
                            modifier = Modifier.fillMaxWidth())
                    }
                    1 -> {
                        // FechaCampo provides the automatic separators already used by Android.
                        if (!guardando) {
                            FechaCampo(fecha, { fecha = it }, "Fecha (AAAA-MM-DD) *", Modifier.fillMaxWidth())
                            FechaCampo(hora, { hora = it }, "Hora (HH:MM) *", Modifier.fillMaxWidth(), hora = true)
                        } else Text("$fecha · $hora")
                        CampoEvento("Cupos (0 = sin límite)", cupos, { cupos = it }, !guardando, KeyboardType.Number)
                        if (inicial != null) SelectorEvento("Estado", estado.replace('_', ' '), estadosEvento.map { it to it.replace('_', ' ') }, !guardando) { estado = it }
                    }
                    else -> {
                        if (recintos.isNotEmpty()) SelectorEvento("Usar recinto local", "Seleccionar recinto",
                            recintos.map { it.id.toString() to it.nombre }, !guardando) { id ->
                            recintos.firstOrNull { it.id.toString() == id }?.let {
                                recinto = it.nombre; direccion = it.direccion
                                latitud = it.latitud?.toString().orEmpty(); longitud = it.longitud?.toString().orEmpty()
                                radio = it.radioMetros.toString()
                            }
                        }
                        CampoEvento("Recinto (opcional)", recinto, { recinto = it }, !guardando)
                        CampoEvento("Dirección (opcional)", direccion, { direccion = it }, !guardando)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.weight(1f)) { CampoEvento("Latitud", latitud, { latitud = it }, !guardando, KeyboardType.Decimal) }
                            Box(Modifier.weight(1f)) { CampoEvento("Longitud", longitud, { longitud = it }, !guardando, KeyboardType.Decimal) }
                        }
                        CampoEvento("Radio (m)", radio, { radio = it }, !guardando, KeyboardType.Number)
                        Text("Ubicación opcional. Completa ambas coordenadas o deja ambas vacías.", style = MaterialTheme.typography.bodySmall)
                    }
                }
                (error ?: errorGuardado)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss, enabled = !guardando) { Text("Cancelar") }
            }
        },
        confirmButton = {
            Button(enabled = !guardando && proyectos.isNotEmpty(), onClick = {
                error = null
                runCatching {
                    validarPaso()
                    if (paso < 2) { paso++; return@runCatching }
                    val iso = if (inicial != null && FormatoFechaHora.fecha(fecha) == inicial.fechaHora.substringBefore('T') &&
                        FormatoFechaHora.hora(hora) == inicial.fechaHora.substringAfter('T').take(5)) inicial.fechaHora
                        else requireNotNull(FormatoFechaHora.combinar(FormatoFechaHora.fecha(fecha), FormatoFechaHora.hora(hora))) { "Revisa la fecha y hora" }
                    val lat = if (latitud.isBlank()) null else requireNotNull(latitud.toDoubleOrNull()) { "Latitud inválida" }
                    val lon = if (longitud.isBlank()) null else requireNotNull(longitud.toDoubleOrNull()) { "Longitud inválida" }
                    val input = EventoInput(proyectoId, nombre.trim(), descripcion.trim().ifBlank { null }, iso,
                        requireNotNull(cupos.toIntOrNull()) { "Cupos inválidos" }, recinto.trim().ifBlank { null },
                        direccion.trim().ifBlank { null }, lat, lon, requireNotNull(radio.toIntOrNull()) { "Radio inválido" }, estado)
                    input.validar()
                    onGuardar(input)
                }.onFailure { error = it.message ?: "Revisa los datos del evento" }
            }) { Text(if (guardando) "Guardando…" else if (paso < 2) "Siguiente" else "Guardar") }
        },
        dismissButton = {
            if (paso > 0) TextButton(enabled = !guardando, onClick = { paso--; error = null }) { Text("Atrás") }
        },
    )
}

@Composable
private fun CampoEvento(label: String, value: String, change: (String) -> Unit, enabled: Boolean,
    type: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(value = value, onValueChange = change, label = { Text(label) }, singleLine = true,
        enabled = enabled, keyboardOptions = KeyboardOptions(keyboardType = type), modifier = Modifier.fillMaxWidth())
}

@Composable
private fun SelectorEvento(label: String, value: String, options: List<Pair<String, String>>, enabled: Boolean,
    onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Box {
            OutlinedButton(onClick = { expanded = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text(value) }
            DropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
                options.forEach { (id, text) -> DropdownMenuItem(text = { Text(text) }, onClick = { onSelect(id); expanded = false }) }
            }
        }
    }
}
