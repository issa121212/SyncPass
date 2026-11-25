package com.rojas.syncpass.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rojas.syncpass.ui.event.EventoDetalleScreen
import com.rojas.syncpass.ui.event.EventoDetalleViewModel
import com.rojas.syncpass.ui.home.HomeScreen
import com.rojas.syncpass.ui.home.HomeViewModel

private const val RUTA_HOME = "home"
private const val RUTA_EVENTO = "evento/{id}"
private const val RUTA_SCANNER = "scanner/{id}"

@Composable
fun SyncPassApp(
    onLogout: () -> Unit,
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = RUTA_HOME) {
        composable(RUTA_HOME) {
            val vm: HomeViewModel = hiltViewModel()
            val state by vm.uiState.collectAsState()
            HomeScreen(
                state = state,
                onRefresh = vm::refrescar,
                onSync = vm::sincronizar,
                onEventClick = { id -> navController.navigate("evento/$id") },
                onLogout = onLogout,
                onCrearProyecto = vm::crearProyecto,
                onCrearEvento = vm::crearEvento,
                onLimpiarMensaje = vm::limpiarMensaje,
                onAdministrar = { navController.navigate("gestion") },
            )
        }

        composable("gestion") {
            val vm: com.rojas.syncpass.ui.gestion.GestionViewModel = hiltViewModel()
            com.rojas.syncpass.ui.gestion.GestionScreen(vm, onBack = { navController.popBackStack() })
        }
        composable(
            route = RUTA_EVENTO,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: return@composable
            val vm: EventoDetalleViewModel = hiltViewModel()
            val state by vm.uiState.collectAsState()
            EventoDetalleScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onRefresh = vm::refrescar,
                onInscribirse = vm::inscribirse,
                onGuardarEncuesta = vm::guardarEncuesta,
                onEscanear = { navController.navigate("scanner/$id") },
                onLimpiarMensaje = vm::limpiarMensaje,
                onGuardarEvento = vm::guardarEvento,
                onEliminarEvento = vm::eliminarEvento,
                onCancelarEvento = vm::cancelarEvento,
                onLimpiarErrorEdicion = vm::limpiarErrorEdicion,
            )
        }

        composable(
            route = RUTA_SCANNER,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: return@composable
            val scanner: com.rojas.syncpass.ui.scanner.ScannerViewModel = hiltViewModel()
            com.rojas.syncpass.ui.scanner.AttendanceScannerScreen(scanner, onBack = { navController.popBackStack() })
        }
    }
}
