package com.rojas.syncpass.ui.navigation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rojas.syncpass.core.util.UbicacionHelper
import com.rojas.syncpass.domain.SyncPassOperations
import com.rojas.syncpass.ui.event.EventoDetalleScreen
import com.rojas.syncpass.ui.event.EventoDetalleViewModel
import com.rojas.syncpass.ui.event.EventoDetalleViewModelFactory
import com.rojas.syncpass.ui.home.HomeScreen
import com.rojas.syncpass.ui.home.HomeViewModel
import com.rojas.syncpass.ui.home.HomeViewModelFactory
import com.rojas.syncpass.ui.scanner.QrScannerScreen
import kotlinx.coroutines.launch

private const val RUTA_HOME = "home"
private const val RUTA_EVENTO = "evento/{id}"
private const val RUTA_SCANNER = "scanner/{id}"

@Composable
fun SyncPassApp(
    repository: SyncPassOperations,
    onLogout: () -> Unit,
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = RUTA_HOME) {
        composable(RUTA_HOME) {
            val vm: HomeViewModel = viewModel(factory = HomeViewModelFactory(repository))
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
            com.rojas.syncpass.ui.gestion.GestionScreen(repository, onBack = { navController.popBackStack() })
        }
        composable(
            route = RUTA_EVENTO,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: return@composable
            val vm: EventoDetalleViewModel = viewModel(
                key = "evento-$id",
                factory = EventoDetalleViewModelFactory(id, repository),
            )
            val state by vm.uiState.collectAsState()
            EventoDetalleScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onRefresh = vm::refrescar,
                onInscribirse = vm::inscribirse,
                onGuardarEncuesta = vm::guardarEncuesta,
                onEscanear = { navController.navigate("scanner/$id") },
                onLimpiarMensaje = vm::limpiarMensaje,
            )
        }

        composable(
            route = RUTA_SCANNER,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: return@composable
            val context = LocalContext.current
            val scanner: com.rojas.syncpass.ui.scanner.ScannerViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST") override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = com.rojas.syncpass.ui.scanner.ScannerViewModel(repository) as T
            })
            val resultado by scanner.resultado.collectAsState()
            androidx.compose.runtime.LaunchedEffect(resultado) {
                resultado?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show(); navController.popBackStack() }
            }
            val ubicacionHelper = UbicacionHelper(context)

            QrScannerScreen(
                titulo = "Escanear participante",
                onScanned = { codigo ->
                    val ubicacion = ubicacionHelper.ultimaUbicacion()
                    scanner.registrar(id, codigo, ubicacion?.latitud, ubicacion?.longitud, ubicacion?.precisionMetros)
                },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
