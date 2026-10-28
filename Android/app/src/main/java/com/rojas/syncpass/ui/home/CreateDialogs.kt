package com.rojas.syncpass.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.rojas.syncpass.core.util.FormatoFechaHora
import com.rojas.syncpass.ui.components.FechaCampo
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.platform.testTag
import com.rojas.syncpass.domain.model.Proyecto

@Composable
fun ProyectoDialog(
    onDismiss: () -> Unit,
    onGuardar: (
        nombre: String,
        descripcion: String,
        areaTematica: String,
        unidadVecinal: String,
        fechaInicio: String,
        fechaFin: String,
    ) -> Unit,
    inicial: Proyecto? = null,
    cerrarAlGuardar: Boolean = true,
    guardando: Boolean = false,
    errorGuardado: String? = null,
) {
    var nombre by remember(inicial?.id) { mutableStateOf(inicial?.nombre.orEmpty()) }
    var descripcion by remember(inicial?.id) { mutableStateOf(inicial?.descripcion.orEmpty()) }
    var area by remember(inicial?.id) { mutableStateOf(inicial?.areaTematica.orEmpty()) }
    var unidad by remember(inicial?.id) { mutableStateOf(inicial?.unidadVecinal.orEmpty()) }
    var inicio by remember(inicial?.id) { mutableStateOf(FormatoFechaHora.digitos(inicial?.fechaInicio.orEmpty(), 8)) }
    var fin by remember(inicial?.id) { mutableStateOf(FormatoFechaHora.digitos(inicial?.fechaFin.orEmpty(), 8)) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!guardando) onDismiss() },
        title = { Text(if (inicial == null) "Nuevo proyecto comunitario" else "Editar proyecto") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = area,
                    onValueChange = { area = it },
                    label = { Text("Área temática") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = unidad,
                    onValueChange = { unidad = it },
                    label = { Text("Unidad vecinal") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                FechaCampo(inicio, { inicio = it }, "Inicio (AAAA-MM-DD)", Modifier.fillMaxWidth())
                FechaCampo(fin, { fin = it }, "Fin (AAAA-MM-DD)", Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                errorGuardado?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (nombre.isBlank()) {
                    Text("El nombre es obligatorio", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!FormatoFechaHora.rangoValido(FormatoFechaHora.fecha(inicio), FormatoFechaHora.fecha(fin))) {
                        error = "Revisa las fechas: deben ser validas y el fin no puede ser anterior al inicio"
                        return@Button
                    }
                    onGuardar(
                        nombre.trim(),
                        descripcion.trim(),
                        area.trim(),
                        unidad.trim(),
                        FormatoFechaHora.fecha(inicio),
                        FormatoFechaHora.fecha(fin),
                    )
                    if (cerrarAlGuardar) onDismiss()
                },
                enabled = nombre.isNotBlank() && !guardando,
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !guardando) { Text("Cancelar") }
        },
    )
}

@Composable
fun EventoDialog(
    proyectos: List<Proyecto>,
    onDismiss: () -> Unit,
    onGuardar: (
        proyectoId: Long,
        nombre: String,
        descripcion: String,
        fechaHora: String,
        cupos: Int,
        recinto: String,
        direccion: String,
        latitud: Double?,
        longitud: Double?,
        radioMetros: Int,
    ) -> Unit,
    recintos: List<com.rojas.syncpass.domain.model.Recinto> = emptyList(),
) {
    var proyecto by remember { mutableStateOf(proyectos.firstOrNull()) }
    var nombre by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf("") }
    var hora by remember { mutableStateOf("") }
    var cupos by remember { mutableStateOf("30") }
    var recinto by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var latitud by remember { mutableStateOf("") }
    var longitud by remember { mutableStateOf("") }
    var radio by remember { mutableStateOf("200") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo evento / taller") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Proyecto", style = MaterialTheme.typography.labelLarge)
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    proyectos.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().selectable(
                                selected = proyecto?.id == item.id,
                                onClick = { proyecto = item }, role = Role.RadioButton)
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = proyecto?.id == item.id, onClick = null)
                            Text(item.nombre, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    if (proyectos.isEmpty()) Text("Crea primero un proyecto")
                }
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre del evento *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                )
                FechaCampo(fecha, { fecha = it }, "Fecha (AAAA-MM-DD) *",
                    Modifier.fillMaxWidth().testTag("evento-fecha"))
                FechaCampo(hora, { hora = it }, "Hora (HH:MM) *",
                    Modifier.fillMaxWidth().testTag("evento-hora"), hora = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = cupos,
                        onValueChange = { cupos = it.filter(Char::isDigit) },
                        label = { Text("Cupos (0 = abierto)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = radio,
                        onValueChange = { radio = it.filter(Char::isDigit) },
                        label = { Text("Radio (m)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
                if (recintos.isNotEmpty()) {
                    Text("Usar un recinto guardado", style = MaterialTheme.typography.labelLarge)
                    Column(Modifier.heightIn(max = 140.dp).verticalScroll(rememberScrollState())) {
                        recintos.forEach { item ->
                            TextButton(onClick = {
                                recinto = item.nombre; direccion = item.direccion
                                latitud = item.latitud?.toString().orEmpty()
                                longitud = item.longitud?.toString().orEmpty(); radio = item.radioMetros.toString()
                            }) { Text(item.nombre) }
                        }
                    }
                }
                OutlinedTextField(
                    value = recinto,
                    onValueChange = { recinto = it },
                    label = { Text("Recinto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = direccion,
                    onValueChange = { direccion = it },
                    label = { Text("Dirección") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latitud,
                        onValueChange = { latitud = it },
                        label = { Text("Latitud") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = longitud,
                        onValueChange = { longitud = it },
                        label = { Text("Longitud") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val proyectoId = proyecto?.id
                    val iso = FormatoFechaHora.combinar(FormatoFechaHora.fecha(fecha), FormatoFechaHora.hora(hora))
                    when {
                        proyectoId == null -> error = "Selecciona un proyecto"
                        nombre.isBlank() -> error = "El nombre es obligatorio"
                        iso == null -> error = "Ingresa una fecha valida (AAAA-MM-DD) y una hora entre 00:00 y 23:59"
                        else -> {
                            onGuardar(
                                proyectoId,
                                nombre.trim(),
                                descripcion.trim(),
                                iso,
                                cupos.toIntOrNull() ?: 0,
                                recinto.trim(),
                                direccion.trim(),
                                latitud.toDoubleOrNull(),
                                longitud.toDoubleOrNull(),
                                radio.toIntOrNull() ?: 200,
                            )
                            onDismiss()
                        }
                    }
                },
                enabled = proyectos.isNotEmpty(),
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
