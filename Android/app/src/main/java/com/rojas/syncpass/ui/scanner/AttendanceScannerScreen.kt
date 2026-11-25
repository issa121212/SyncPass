package com.rojas.syncpass.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AttendanceScannerScreen(vm: ScannerViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) vm.registrarConUbicacion() else vm.permisoDenegado()
    }
    LaunchedEffect(state.phase) {
        when (state.phase) {
            CapturePhase.PERMISSION -> {
                if (permissions.any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) vm.registrarConUbicacion()
                else launcher.launch(permissions)
            }
            CapturePhase.CONFIRMED, CapturePhase.PENDING -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                onBack()
            }
            else -> Unit
        }
    }
    if (state.phase == CapturePhase.READING) {
        QrScannerScreen(titulo = "Escanear participante", onScanned = vm::preparar, onBack = onBack)
    } else {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(state.message ?: "Permiso de ubicación", style = MaterialTheme.typography.bodyLarge)
            if (state.phase in setOf(CapturePhase.ERROR, CapturePhase.REJECTED)) {
                Button(onClick = vm::reintentar) { Text("Reintentar") }
                TextButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri()))
                }) { Text("Ajustar permisos de ubicación") }
                TextButton(onClick = vm::otroQr) { Text("Escanear otro QR") }
            } else CircularProgressIndicator(Modifier.padding(16.dp))
            TextButton(onClick = onBack) { Text("Volver") }
        }
    }
}
