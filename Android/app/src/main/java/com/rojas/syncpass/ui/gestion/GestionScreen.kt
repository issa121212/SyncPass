package com.rojas.syncpass.ui.gestion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rojas.syncpass.domain.model.*
import com.rojas.syncpass.domain.model.ProyectoInput
import com.rojas.syncpass.domain.SyncPassOperations
import com.rojas.syncpass.ui.home.ProyectoDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionScreen(repository: SyncPassOperations, onBack: () -> Unit) {
    val vm: GestionViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = GestionViewModel(repository) as T
    })
    val proyectos by vm.proyectos.collectAsState()
    val recintos by vm.recintos.collectAsState()
    val perfil by vm.perfil.collectAsState()
    val estado by vm.estado.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    var proyectoForm by remember { mutableStateOf(false) }
    var proyectoEditar by remember { mutableStateOf<Proyecto?>(null) }
    var proyectoDetalle by remember { mutableStateOf<Proyecto?>(null) }
    var proyectoEliminar by remember { mutableStateOf<Proyecto?>(null) }
    var recintoForm by remember { mutableStateOf(false) }
    var recintoEditar by remember { mutableStateOf<Recinto?>(null) }
    var recintoDetalle by remember { mutableStateOf<Recinto?>(null) }
    var recintoEliminar by remember { mutableStateOf<Recinto?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val roles = perfil?.roles?.split(",").orEmpty()
    val docente = roles.any { it.equals("DOCENTE", true) || it.equals("ADMIN", true) }
    val admin = roles.any { it.equals("ADMIN", true) }
    LaunchedEffect(estado.mensaje) { estado.mensaje?.let { snackbar.showSnackbar(it); vm.limpiarMensaje() } }
    Scaffold(topBar = { TopAppBar(title = { Text("Administrar") }, navigationIcon = {
        TextButton(onClick = onBack) { Text("Volver") }
    }) }, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        if (!docente) {
            Text("Esta seccion requiere una cuenta docente.", Modifier.padding(padding).padding(16.dp))
        } else Column(Modifier.padding(padding).fillMaxSize()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("Proyectos · API") })
                FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("Recintos · Local") })
            }
            Button(onClick = {
                if (tab == 0) { proyectoEditar = null; proyectoForm = true }
                else { recintoEditar = null; recintoForm = true }
            }, enabled = !estado.guardando, modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(if (tab == 0) "Nuevo proyecto" else "Nuevo recinto")
            }
            if (estado.guardando) LinearProgressIndicator(Modifier.fillMaxWidth())
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Text(if (tab == 0) "Los cambios se confirman en el servidor. Requieren conexion."
                    else "Catalogo de recintos de esta cuenta y dispositivo. Funciona sin conexion.") }
                if (tab == 0) {
                    if (proyectos.isEmpty()) item { Text("No hay proyectos") }
                    items(proyectos, key = { "proyecto-${it.id}" }) { p ->
                        val puede = admin || p.responsableId == perfil?.usuarioId
                        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                            Text(p.nombre, style = MaterialTheme.typography.titleMedium)
                            Text(p.descripcion.orEmpty())
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { vm.consultarProyecto(p.id) { proyectoDetalle = it } }, enabled = !estado.guardando) { Text("Ver detalle") }
                                if (puede) {
                                    TextButton(onClick = { proyectoEditar = p; proyectoForm = true }, enabled = !estado.guardando) { Text("Editar") }
                                    TextButton(onClick = { proyectoEliminar = p }, enabled = !estado.guardando) { Text("Eliminar") }
                                }
                            }
                        } }
                    }
                } else {
                    if (recintos.isEmpty()) item { Text("No hay recintos guardados") }
                    items(recintos, key = { "recinto-${it.id}" }) { p ->
                        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                            Text(p.nombre, style = MaterialTheme.typography.titleMedium); Text(p.direccion)
                            Row {
                                TextButton(onClick = { recintoDetalle = p }) { Text("Ver detalle") }
                                TextButton(onClick = { recintoEditar = p; recintoForm = true }) { Text("Editar") }
                                TextButton(onClick = { recintoEliminar = p }) { Text("Eliminar") }
                            }
                        } }
                    }
                }
            }
        }
    }
    if (proyectoForm) ProyectoDialog(
        inicial = proyectoEditar, cerrarAlGuardar = false, guardando = estado.guardando,
        errorGuardado = estado.mensaje?.takeIf { estado.error },
        onDismiss = { proyectoForm = false },
        onGuardar = { n, d, a, u, i, f -> vm.guardarProyecto(proyectoEditar?.id, ProyectoInput(n,
            d.ifBlank { null }, a.ifBlank { null }, u.ifBlank { null }, i.ifBlank { null }, f.ifBlank { null },
            proyectoEditar?.estado ?: "PLANIFICADO")) { proyectoForm = false } })
    proyectoDetalle?.let { p -> DetalleDialog(p.nombre, listOf(
        "Descripcion" to p.descripcion.orEmpty(), "Area" to p.areaTematica.orEmpty(),
        "Unidad vecinal" to p.unidadVecinal.orEmpty(), "Inicio" to p.fechaInicio.orEmpty(),
        "Fin" to p.fechaFin.orEmpty(), "Estado" to p.estado,
        "Responsable" to (p.responsableNombre ?: "Nombre no disponible"))) { proyectoDetalle = null } }
    proyectoEliminar?.let { p -> ConfirmarEliminar(p.nombre, estado.guardando,
        onDismiss = { proyectoEliminar = null }, onConfirm = { vm.eliminarProyecto(p.id) { proyectoEliminar = null } }, error = estado.mensaje?.takeIf { estado.error }) }
    if (recintoForm) RecintoDialog(recintoEditar, estado.guardando, { recintoForm = false }) { item ->
        vm.guardarRecinto(item) { recintoForm = false }
    }
    recintoDetalle?.let { p -> DetalleDialog(p.nombre, listOf("Direccion" to p.direccion,
        "Latitud" to (p.latitud?.toString() ?: "Sin coordenadas"),
        "Longitud" to (p.longitud?.toString() ?: "Sin coordenadas"), "Radio" to "${p.radioMetros} m")) { recintoDetalle = null } }
    recintoEliminar?.let { p -> ConfirmarEliminar(p.nombre, estado.guardando,
        { recintoEliminar = null }, { vm.eliminarRecinto(p.id) { recintoEliminar = null } }, error = estado.mensaje?.takeIf { estado.error }) }
}

@Composable
private fun DetalleDialog(nombre: String, campos: List<Pair<String, String>>, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(nombre) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            campos.forEach { (label, value) -> Text("$label: ${value.ifBlank { "Sin informacion" }}") }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } })
}

@Composable
private fun ConfirmarEliminar(nombre: String, guardando: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit, error: String? = null) {
    AlertDialog(onDismissRequest = { if (!guardando) onDismiss() }, title = { Text("Eliminar $nombre") },
        text = { Column { Text("Se eliminara este registro. ¿Deseas continuar?"); error?.let { Text(it, color = MaterialTheme.colorScheme.error) } } },
        confirmButton = { Button(onClick = onConfirm, enabled = !guardando) { Text("Confirmar eliminacion") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !guardando) { Text("Cancelar") } })
}

@Composable
fun RecintoDialog(inicial: Recinto?, guardando: Boolean, onDismiss: () -> Unit, onGuardar: (Recinto) -> Unit) {
    var nombre by remember(inicial?.id) { mutableStateOf(inicial?.nombre.orEmpty()) }
    var direccion by remember(inicial?.id) { mutableStateOf(inicial?.direccion.orEmpty()) }
    var latitud by remember(inicial?.id) { mutableStateOf(inicial?.latitud?.toString().orEmpty()) }
    var longitud by remember(inicial?.id) { mutableStateOf(inicial?.longitud?.toString().orEmpty()) }
    var radio by remember(inicial?.id) { mutableStateOf((inicial?.radioMetros ?: 200).toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = { if (!guardando) onDismiss() }, title = { Text(if (inicial == null) "Nuevo recinto" else "Editar recinto") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre *") }, singleLine = true)
            OutlinedTextField(direccion, { direccion = it }, label = { Text("Direccion *") }, singleLine = true)
            OutlinedTextField(latitud, { latitud = it }, label = { Text("Latitud (opcional)") }, singleLine = true)
            OutlinedTextField(longitud, { longitud = it }, label = { Text("Longitud (opcional)") }, singleLine = true)
            OutlinedTextField(radio, { radio = it }, label = { Text("Radio en metros *") }, singleLine = true)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } }, confirmButton = { Button(enabled = !guardando, onClick = {
            runCatching {
                val lat = if (latitud.isBlank()) null else requireNotNull(latitud.replace(',', '.').toDoubleOrNull()) { "Latitud invalida" }
                val lon = if (longitud.isBlank()) null else requireNotNull(longitud.replace(',', '.').toDoubleOrNull()) { "Longitud invalida" }
                val item = Recinto(inicial?.id ?: 0, nombre.trim(), direccion.trim(), lat, lon,
                    requireNotNull(radio.toIntOrNull()) { "Radio invalido" })
                item.validar(); onGuardar(item)
            }.onFailure { error = it.message }
        }) { Text("Guardar") } }, dismissButton = { TextButton(onClick = onDismiss, enabled = !guardando) { Text("Cancelar") } })
}
