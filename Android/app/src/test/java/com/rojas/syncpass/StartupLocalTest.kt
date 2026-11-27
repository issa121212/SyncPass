package com.rojas.syncpass

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.rojas.syncpass.core.config.CognitoAppConfig
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Actividad real con el grafo Hilt de producción; aplicación de prueba sin tareas programadas. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = HiltLocalTestApplication::class)
class StartupLocalTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun sinSesionLaActividadMuestraLoginYNoDashboard() {
        if (CognitoAppConfig.isConfigured) compose.onNodeWithText("Continuar con Cognito").assertIsDisplayed()
        else compose.onNodeWithText("Falta configuración de Cognito (.env)").assertIsDisplayed()
        compose.onNodeWithText("Administrar proyectos y recintos").assertDoesNotExist()
        compose.onNodeWithText("Nuevo proyecto").assertDoesNotExist()
    }
}

/** Conserva el grafo real; evita arrancar WorkManager al preparar la aplicación simulada. */
class HiltLocalTestApplication : android.app.Application(), dagger.hilt.internal.GeneratedComponentManager<Any> {
    private val component by lazy {
        DaggerSyncPassApplication_HiltComponents_SingletonC.builder()
            .applicationContextModule(dagger.hilt.android.internal.modules.ApplicationContextModule(this)).build()
    }
    override fun generatedComponent(): Any = component
}
