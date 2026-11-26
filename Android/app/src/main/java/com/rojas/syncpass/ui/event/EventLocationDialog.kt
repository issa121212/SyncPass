package com.rojas.syncpass.ui.event

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rojas.syncpass.domain.model.EventZone
import com.rojas.syncpass.domain.model.GeoPoint

@Composable
fun EventLocationDialog(
    dialogId: String,
    initialPoint: GeoPoint?,
    initialRadius: Int,
    onDismiss: () -> Unit,
    onConfirm: (EventZone) -> Unit,
    vm: EventLocationViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) vm.locate() else vm.permissionDenied()
    }
    LaunchedEffect(dialogId) { vm.open(dialogId, initialPoint, initialRadius) }
    DisposableEffect(vm) { onDispose { vm.cancelLocation() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Zona del evento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
                Column(
                    Modifier.fillMaxWidth().heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Toca el mapa para elegir el centro de la zona")
                    EventZoneMap(state.point, state.radius.toIntOrNull()?.takeIf { it in 1..50_000 } ?: 200, vm::selectPoint)
                    OutlinedButton(enabled = !state.locating, onClick = {
                        if (permissions.any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
                            vm.locate()
                        } else launcher.launch(permissions)
                    }) { Text(if (state.locating) "Obteniendo ubicación…" else "Usar mi ubicación") }
                    state.point?.let { Text("Centro seleccionado: %.5f, %.5f".format(it.latitude, it.longitude)) }
                    state.accuracyMeters?.let { Text("Precisión informada: %.0f m. Revisa el punto antes de confirmar".format(it)) }
                    OutlinedTextField(
                        value = state.radius, onValueChange = vm::setRadius,
                        label = { Text("Radio permitido (m)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
            }
        },
        confirmButton = {
            Button(enabled = !state.locating, onClick = { vm.confirm()?.let(onConfirm) }) { Text("Usar esta zona") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Volver") } },
    )
}
