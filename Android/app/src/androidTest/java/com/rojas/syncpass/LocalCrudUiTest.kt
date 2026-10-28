package com.rojas.syncpass

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rojas.syncpass.data.local.*
import com.rojas.syncpass.data.remote.SyncPassApi
import com.rojas.syncpass.data.session.SessionStore
import com.rojas.syncpass.data.repository.SyncPassRepository
import com.rojas.syncpass.ui.gestion.GestionScreen
import com.rojas.syncpass.ui.theme.SyncPassTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.lang.reflect.Proxy

class LocalCrudUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun localCrudCanBeCompletedWithoutAnyApiCall() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, SyncPassDatabase::class.java).build()
        try {
            runBlocking { db.dao().guardarPerfil(PerfilEntity(usuarioId = 1, nombre = "Ana", email = "ana@example.com",
                roles = "DOCENTE", qrContenido = "ESTUDIANTE_1")) }
            val api = Proxy.newProxyInstance(SyncPassApi::class.java.classLoader, arrayOf(SyncPassApi::class.java)) { _, _, _ ->
                throw AssertionError("El CRUD local no debe llamar a la API")
            } as SyncPassApi
            val repo = SyncPassRepository(api, db.dao(), SessionStore(context), db.recintoDao())
            compose.setContent { SyncPassTheme { GestionScreen(repo, {}) } }
            compose.onNodeWithText("Recintos · Local").performClick()
            compose.onNodeWithText("Nuevo recinto").performClick()
            compose.onNodeWithText("Nombre *").performTextInput("Sede vecinal")
            compose.onNodeWithText("Direccion *").performTextInput("Calle 100")
            compose.onNodeWithText("Guardar").performClick()
            compose.onNodeWithText("Sede vecinal").assertIsDisplayed()
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
        } finally { db.close() }
    }
}
