package com.rojas.syncpass.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rojas.syncpass.core.util.FechaUtils
import com.rojas.syncpass.domain.model.Evento
import com.rojas.syncpass.domain.model.Proyecto
import com.rojas.syncpass.ui.components.QrDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onRefresh: () -> Unit,
    onSync: () -> Unit,
    onEventClick: (Long) -> Unit,
    onLogout: () -> Unit,
    onCrearProyecto: (
        nombre: String,
        descripcion: String,
        areaTematica: String,
        unidadVecinal: String,
        fechaInicio: String,
        fechaFin: String,
    ) -> Unit,
    onCrearEvento: (
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
    onLimpiarMensaje: () -> Unit,
    onAdministrar: () -> Unit = {},
) {
    val snackbar = remember { SnackbarHostState() }
    var mostrarProyecto by remember { mutableStateOf(false) }
    var mostrarEvento by remember { mutableStateOf(false) }
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
                title = {
                    Column {
                        Text("SyncPass", fontWeight = FontWeight.Bold)
                        state.perfil?.let {
                            if (it.nombre.isNotBlank()) Text(it.nombre, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
                actions = {
                    TextButton(onClick = onRefresh) { Text("Actualizar") }
                    TextButton(onClick = onSync) { Text("Sincronizar") }
                    TextButton(onClick = onLogout) { Text("Salir") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (state.perfil == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Text("Cargando perfil...", modifier = Modifier.padding(top = 12.dp))
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Encabezado(
                    state = state,
                    onQrClick = { mostrarQr = true },
                    onNuevoProyecto = { mostrarProyecto = true },
                    onNuevoEvento = { mostrarEvento = true },
                )
            }

            if (state.rechazadas.isNotEmpty()) {
                item {
                    Text("Operaciones rechazadas", color = MaterialTheme.colorScheme.error)
                    state.rechazadas.forEach { Text("${it.tipo}: ${it.motivo}. Corrige la causa y realiza una captura nueva.") }
                }
            }
            if (state.esDocente) {
                item {
                    OutlinedButton(onClick = onAdministrar, modifier = Modifier.fillMaxWidth()) {
                        Text("Administrar proyectos y recintos")
                    }
                }
            }
            item {
                Text("Eventos y talleres", style = MaterialTheme.typography.titleLarge)
            }

            if (state.eventos.isEmpty()) {
                item {
                    Text(
                        "No hay eventos guardados. Actualiza cuando tengas conexión.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(
                    count = state.eventos.size,
                    key = { index -> "evento_${state.eventos[index].id}" },
                ) { index ->
                    val evento = state.eventos[index]
                    EventoCard(evento = evento, esDocente = state.esDocente, onClick = {
                        onEventClick(evento.id)
                    })
                }
            }

            if (state.proyectos.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("Proyectos comunitarios", style = MaterialTheme.typography.titleLarge)
                }
                items(
                    count = state.proyectos.size,
                    key = { index -> "proyecto_${state.proyectos[index].id}" },
                ) { index ->
                    val proyecto = state.proyectos[index]
                    ProyectoCard(proyecto)
                }
            }
        }
    }

    if (mostrarProyecto) {
        ProyectoDialog(
            onDismiss = { mostrarProyecto = false },
            onGuardar = onCrearProyecto,
        )
    }
    if (mostrarEvento) {
        EventoDialog(
            proyectos = state.proyectos.filter { com.rojas.syncpass.domain.model.puedeGestionarProyecto(it, state.perfil) },
            recintos = state.recintos,
            onDismiss = { mostrarEvento = false },
            onGuardar = onCrearEvento,
        )
    }
    if (mostrarQr) {
        QrDialog(
            contenido = state.perfil?.qrContenido.orEmpty(),
            titulo = "Mi credencial SyncPass",
            onDismiss = { mostrarQr = false },
        )
    }
}

@Composable
private fun Encabezado(
    state: HomeUiState,
    onQrClick: () -> Unit,
    onNuevoProyecto: () -> Unit,
    onNuevoEvento: () -> Unit,
) {
    val perfil = state.perfil ?: return
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (perfil.nombre.isBlank()) "Bienvenido" else "Hola, ${perfil.nombre}", style = MaterialTheme.typography.titleLarge)
            Text(
                when {
                    state.esDocente -> "Docente / Coordinación"
                    state.esVoluntario -> "Voluntario / Recepción"
                    state.esParticipante -> "Participante / Beneficiario"
                    state.esEstudiante -> "Voluntario / Recepción"
                    else -> "Usuario"
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                if (state.pendientes == 0) "Todo sincronizado" else "${state.pendientes} operaciones pendientes",
                style = MaterialTheme.typography.bodySmall,
                color = if (state.pendientes == 0) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.esParticipante || state.esEstudiante) {
                    Button(onClick = onQrClick) { Text("Mi QR") }
                }
                if (state.esDocente) {
                    Button(onClick = onNuevoProyecto) { Text("Nuevo proyecto") }
                    OutlinedButton(onClick = onNuevoEvento) { Text("Nuevo evento") }
                }
            }
        }
    }
}

@Composable
private fun EventoCard(evento: Evento, esDocente: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(evento.nombre, style = MaterialTheme.typography.titleMedium)
                    Text(evento.proyectoNombre, style = MaterialTheme.typography.bodySmall)
                }
                EstadoChip(evento.estado)
            }
            Text(FechaUtils.mostrar(evento.fechaHora), style = MaterialTheme.typography.bodyMedium)
            val lugar = listOfNotNull(evento.recinto, evento.direccion).joinToString(" · ")
            if (lugar.isNotBlank()) {
                Text(lugar, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                if (esDocente) {
                    "Inscritos: ${evento.inscritos} · Presentes: ${evento.presentes}"
                } else {
                    "Cupos: ${if (evento.cupos == 0) "abiertos" else evento.cupos} · Inscritos: ${evento.inscritos}"
                },
                style = MaterialTheme.typography.bodySmall,
            )
            if (!esDocente) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (evento.inscrito) Etiqueta("Inscrito")
                    if (evento.esPresente) Etiqueta("Asistió")
                    if (evento.encuestaRespondida) Etiqueta("Encuesta lista")
                }
            }
        }
    }
}

@Composable
private fun ProyectoCard(proyecto: Proyecto) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(proyecto.nombre, style = MaterialTheme.typography.titleMedium)
            proyecto.areaTematica?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            proyecto.unidadVecinal?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            if (!proyecto.responsableNombre.isNullOrBlank()) {
                Text("Responsable: ${proyecto.responsableNombre}", style = MaterialTheme.typography.bodySmall)
            }
            EstadoChip(proyecto.estado)
        }
    }
}

@Composable
private fun EstadoChip(estado: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            estado,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun Etiqueta(texto: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Text(
            texto,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
