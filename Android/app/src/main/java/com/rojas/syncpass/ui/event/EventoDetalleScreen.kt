package com.rojas.syncpass.ui.event

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.rojas.syncpass.core.util.FechaUtils
import com.rojas.syncpass.core.util.UbicacionHelper
import com.rojas.syncpass.domain.model.Evento
import com.rojas.syncpass.domain.model.Asistente
import com.rojas.syncpass.domain.model.EncuestaEvento

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventoDetalleScreen(
    state: EventoDetalleUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onInscribirse: () -> Unit,
    onGuardarEncuesta: (Int, Int, String) -> Unit,
    onEscanear: () -> Unit,
    onLimpiarMensaje: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    var mostrarEncuesta by remember { mutableStateOf(false) }
    var mostrarQr by remember { mutableStateOf(false) }

    LaunchedEffect(state.mensaje, state.error) {
        val texto = state.error ?: state.mensaje
        if (texto != null) {
            snackbar.showSnackbar(texto)
            onLimpiarMensaje()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.evento?.nombre ?: "Evento") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    TextButton(onClick = onRefresh) { Text("Actualizar") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val evento = state.evento
        if (evento == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Text("Cargando evento...", modifier = Modifier.padding(top = 12.dp))
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(evento.proyectoNombre, style = MaterialTheme.typography.labelLarge)
            Text(evento.nombre, style = MaterialTheme.typography.headlineSmall)
            Text(FechaUtils.mostrar(evento.fechaHora), style = MaterialTheme.typography.bodyLarge)
            val lugar = listOfNotNull(evento.recinto, evento.direccion).joinToString(" · ")
            if (lugar.isNotBlank()) Text(lugar, style = MaterialTheme.typography.bodyMedium)
            evento.descripcion?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }

            HorizontalDivider()

            Text(
                "Inscritos: ${evento.inscritos} · Presentes: ${evento.presentes} · Cupos: ${
                    if (evento.cupos == 0) "abiertos" else evento.cupos
                }${if (evento.cupos > 0) " · Quórum: ${if (evento.presentes >= evento.cupos) "Cumplido" else "${evento.presentes}/${evento.cupos}"}" else ""}",
                style = MaterialTheme.typography.bodyMedium,
            )

            if (state.esDocente || state.esVoluntario || state.esEstudiante) {
                Button(onClick = onEscanear, modifier = Modifier.fillMaxWidth()) {
                    Text("Escanear QR de participante")
                }
                Text("Lista de asistentes", style = MaterialTheme.typography.titleMedium)
                if (state.asistentes.isEmpty()) {
                    Text("Aún no hay asistentes registrados.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    state.asistentes.forEach { AsistenteItem(it) }
                }

                if (state.esDocente) {
                    Text("Encuestas recibidas", style = MaterialTheme.typography.titleMedium)
                    if (state.encuestas.isEmpty()) {
                        Text("Aún no hay encuestas.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        state.encuestas.forEach { EncuestaItem(it) }
                    }
                }
            } else {
                if (!evento.inscrito) {
                    Button(onClick = onInscribirse, modifier = Modifier.fillMaxWidth()) {
                        Text("Inscribirme en el evento")
                    }
                } else if (!evento.esPresente) {
                    Button(
                        onClick = { mostrarQr = true },
                        enabled = !state.cargando,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Mostrar QR para asistencia")
                    }
                } else if (!evento.encuestaRespondida) {
                    Button(
                        onClick = { mostrarEncuesta = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Responder encuesta de satisfacción")
                    }
                } else {
                    Text(
                        "Asistencia y encuesta completadas. ¡Gracias!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                if (evento.inscrito) {
                    Text(
                        "Estado: ${if (evento.esPresente) "Presente" else "Inscrito, sin marcación"}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            if (state.cargando) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
            }
        }
    }

    if (mostrarQr) {
        val perfil = state.perfil
        val evento = state.evento
        if (perfil != null && evento != null) {
            com.rojas.syncpass.ui.components.QrDialog(
                contenido = "SYNC_PASS:ASISTENCIA:${evento.id}:${perfil.usuarioId}",
                titulo = "QR de asistencia · ${evento.nombre}",
                onDismiss = { mostrarQr = false })
        }
    }

    if (mostrarEncuesta) {
        EncuestaDialog(
            onDismiss = { mostrarEncuesta = false },
            onGuardar = { satisfaccion, utilidad, comentarios ->
                onGuardarEncuesta(satisfaccion, utilidad, comentarios)
                mostrarEncuesta = false
            },
        )
    }
}

@Composable
private fun AsistenteItem(asistente: Asistente) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(asistente.nombre, style = MaterialTheme.typography.titleSmall)
            if (!asistente.email.endsWith("@cognito.local")) Text(asistente.email, style = MaterialTheme.typography.bodySmall)
            Text(
                if (asistente.presente) "Presente · ${FechaUtils.mostrar(asistente.marcadoEn)}" else "Inscrito",
                style = MaterialTheme.typography.bodySmall,
            )
            asistente.encuesta?.let {
                Text(
                    "Encuesta: ${it.satisfaccion}/5 satisfacción · ${it.utilidad}/5 utilidad",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun EncuestaItem(encuesta: EncuestaEvento) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(encuesta.estudiante?.nombre ?: "Estudiante", style = MaterialTheme.typography.titleSmall)
            Text(
                "Satisfacción ${encuesta.satisfaccion}/5 · Utilidad ${encuesta.utilidad}/5",
                style = MaterialTheme.typography.bodySmall,
            )
            encuesta.comentarios?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun EncuestaDialog(
    onDismiss: () -> Unit,
    onGuardar: (Int, Int, String) -> Unit,
) {
    var satisfaccion by remember { mutableStateOf(4f) }
    var utilidad by remember { mutableStateOf(4f) }
    var comentarios by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Encuesta de satisfacción") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Satisfacción: ${satisfaccion.toInt()}/5")
                Slider(
                    value = satisfaccion,
                    onValueChange = { satisfaccion = it },
                    valueRange = 1f..5f,
                    steps = 3,
                )
                Text("Utilidad del taller: ${utilidad.toInt()}/5")
                Slider(
                    value = utilidad,
                    onValueChange = { utilidad = it },
                    valueRange = 1f..5f,
                    steps = 3,
                )
                OutlinedTextField(
                    value = comentarios,
                    onValueChange = { comentarios = it },
                    label = { Text("Comentarios (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onGuardar(satisfaccion.toInt(), utilidad.toInt(), comentarios) }) {
                Text("Enviar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
