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
    com.rojas.syncpass.ui.event.EventoEditorDialog(
        proyectos = proyectos, recintos = recintos, onDismiss = onDismiss,
        onGuardar = { input ->
            onGuardar(input.proyectoId, input.nombre, input.descripcion.orEmpty(), input.fechaHora,
                input.cupos, input.recinto.orEmpty(), input.direccion.orEmpty(), input.latitud,
                input.longitud, input.radioMetros)
            onDismiss()
        },
    )
}
