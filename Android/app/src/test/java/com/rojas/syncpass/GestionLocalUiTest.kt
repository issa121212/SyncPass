package com.rojas.syncpass

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.rojas.syncpass.ui.home.*
import com.rojas.syncpass.ui.theme.SyncPassTheme
import com.rojas.syncpass.domain.model.*
import com.rojas.syncpass.data.mapper.*
import androidx.lifecycle.ViewModelStore
import com.rojas.syncpass.domain.SyncPassOperations
import com.rojas.syncpass.ui.gestion.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.mockito.Mockito.mock
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GestionLocalUiTest {
    @get:Rule val compose = createComposeRule()
    private val store = ViewModelStore()
    private class FakeRepository : SyncPassOperations by mock(SyncPassOperations::class.java) {
        override val perfil = MutableStateFlow<Perfil?>(Perfil(usuarioId = 3,nombre = "Ana",email = "ana@example.invalid",roles = "DOCENTE",qrContenido = ""))
        override val proyectos = MutableStateFlow(listOf(com.rojas.syncpass.data.remote.ProyectoResponse(id = 1,nombre = "Huerto",descripcion = "Cultivar",responsable = com.rojas.syncpass.data.remote.ResponsableResponse(id = 3,nombre = "Ana"),codigoPublico = "PRY-ABCDEF123456").toEntity().toDomain()))
        override val recintos = MutableStateFlow<List<Recinto>>(emptyList())
        var projectDeletes = 0
        override suspend fun consultarProyecto(id: Long) = Result.success(proyectos.value.single { it.id == id })
        override suspend fun editarProyecto(id: Long, request: ProyectoInput): Result<Unit> {
            proyectos.value = proyectos.value.map { if(it.id == id) it.copy(nombre = request.nombre) else it }
            return Result.success(Unit)
        }
        override suspend fun eliminarProyecto(id: Long): Result<Unit> {
            projectDeletes++; proyectos.value = proyectos.value.filterNot { it.id == id }; return Result.success(Unit)
        }
        override suspend fun guardarRecinto(item: Recinto): Result<Unit> {
            recintos.value = recintos.value.filterNot { it.id == item.id } + item.copy(id = if(item.id == 0L) 1 else item.id)
            return Result.success(Unit)
        }
        override suspend fun eliminarRecinto(id: Long): Result<Unit> {
            recintos.value = recintos.value.filterNot { it.id == id }; return Result.success(Unit)
        }
    }
    private fun show(repo: FakeRepository) {
        val vm = GestionViewModel(repo).also { store.put("gestion",it) }
        compose.setContent { SyncPassTheme { GestionScreen(vm,{}) } }
    }
    @After fun release() { store.clear() }

    @Test fun docenteConsultaEditaYConfirmaAntesDeEliminarProyectoPropio() {
        val repo = FakeRepository(); show(repo)
        compose.onNodeWithText("Huerto").assertIsDisplayed()
        compose.onNodeWithText("PRY-ABCDEF123456").assertIsDisplayed()
        compose.onNodeWithText("Ver detalle").performClick()
        compose.onNodeWithText("Descripcion: Cultivar").assertIsDisplayed()
        compose.onNodeWithText("Cerrar").performClick()
        compose.onNodeWithText("Editar").performClick()
        compose.onNodeWithText("Nombre *").performTextReplacement("Huerto ampliado")
        compose.onNodeWithText("Guardar").performClick()
        compose.onNodeWithText("Huerto ampliado").assertIsDisplayed()
        compose.onNodeWithText("Eliminar").performClick()
        assertEquals(0,repo.projectDeletes)
        compose.onNodeWithText("Cancelar").performClick()
        assertEquals(0,repo.projectDeletes)
        compose.onNodeWithText("Eliminar").performClick()
        compose.onNodeWithText("Confirmar eliminacion").performClick()
        assertEquals(1,repo.projectDeletes)
        compose.onNodeWithText("No hay proyectos").assertIsDisplayed()
    }

    @Test fun recintoExigeDatosAntesDeGuardarYPermiteConsultaEdicionYEliminacion() {
        val repo = FakeRepository(); show(repo)
        compose.onNodeWithText("Recintos · Local").performClick()
        compose.onNodeWithText("Nuevo recinto").performClick()
        compose.onNodeWithText("Guardar").performClick()
        assertTrue(repo.recintos.value.isEmpty())
        compose.onNodeWithText("Nombre *").performTextInput("Sede")
        compose.onNodeWithText("Direccion *").performTextInput("Calle 100")
        compose.onNodeWithText("Guardar").performClick()
        compose.onNodeWithText("Ver detalle").performClick()
        compose.onNodeWithText("Direccion: Calle 100").assertIsDisplayed()
        compose.onNodeWithText("Cerrar").performClick()
        compose.onNodeWithText("Editar").performClick()
        compose.onNodeWithText("Nombre *").performTextReplacement("Sede editada")
        compose.onNodeWithText("Guardar").performClick()
        compose.onNodeWithText("Sede editada").assertIsDisplayed()
        compose.onNodeWithText("Eliminar").performClick()
        compose.onNodeWithText("Confirmar eliminacion").performClick()
        compose.onNodeWithText("No hay recintos guardados").assertIsDisplayed()
    }

    @Test fun participanteNoAccedeAGestionYDocenteAjenoNoPuedeEditar() {
        val repo = FakeRepository(); repo.perfil.value = repo.perfil.value?.copy(roles = "PARTICIPANTE")
        show(repo)
        compose.onNodeWithText("Esta seccion requiere una cuenta docente.").assertIsDisplayed()
        compose.onNodeWithText("Nuevo proyecto").assertDoesNotExist()
        compose.runOnIdle { repo.perfil.value = repo.perfil.value?.copy(roles = "DOCENTE",usuarioId = 99) }
        compose.onNodeWithText("Huerto").assertIsDisplayed()
        compose.onNodeWithText("Editar").assertDoesNotExist()
        compose.onNodeWithText("Eliminar").assertDoesNotExist()
    }
}
