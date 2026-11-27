import java.util.Properties
import java.util.Locale
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension

// On a fresh extraction the IDE configures Gradle before Before Launch can copy .env.
// The example contains PUBLIC endpoints/client identifiers, never tokens or secrets.
val envText: String? = providers.fileContents(
    rootProject.layout.projectDirectory.file(".env")
).asText.orElse(providers.fileContents(
    rootProject.layout.projectDirectory.file(".env.example")
).asText).orNull
val envProps = Properties().apply {
    // removePrefix: algunos editores guardan UTF-8 con BOM y romperían la primera clave.
    envText?.removePrefix("\uFEFF")?.let { load(it.reader()) }
}
// Prioridad: .env > variable de entorno del sistema > fallback solo para compilar
val apiBaseUrl: String =
    envProps.getProperty("API_BASE_URL")
        ?: System.getenv("API_BASE_URL")
        ?: "http://10.0.2.2:8080/"

// Cognito Hosted UI (ver cognito-auth/README.md Paso 3). Sin hardcodear en .kt.
fun envOr(key: String, fallback: String): String =
    envProps.getProperty(key) ?: System.getenv(key) ?: fallback

val cognitoRedirectDefault = "com.rojas.syncpass://cognito/callback"
val cognitoIssuer: String = envOr("COGNITO_ISSUER", "")
val cognitoClientId: String = envOr("COGNITO_CLIENT_ID", "")
val cognitoRedirect: String = envOr("COGNITO_REDIRECT_URI", cognitoRedirectDefault)
val cognitoLogout: String = envOr("COGNITO_LOGOUT_URI", cognitoRedirect)
val cognitoScopes: String = envOr("COGNITO_SCOPES", "email openid phone")

plugins {
    alias(libs.plugins.hilt)
    jacoco
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

jacoco {
    toolVersion = "0.8.14"
}

tasks.withType<Test>().configureEach {
    extensions.configure<JacocoTaskExtension> {
        // Robolectric carga las clases de la app sin ubicación de código fuente.
        isIncludeNoLocationClasses = true
    }
}

android {
    sourceSets.getByName("test").kotlin.directories.add("src/sharedTest/java")
    sourceSets.getByName("androidTest").kotlin.directories.add("src/sharedTest/java")
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.jvmArgs(
                    "--add-opens=java.base/java.lang=ALL-UNNAMED",
                    "--add-opens=java.base/java.util=ALL-UNNAMED",
                    "--add-opens=java.base/java.io=ALL-UNNAMED",
                    "--add-opens=java.base/java.net=ALL-UNNAMED",
                    "--add-opens=java.base/java.security=ALL-UNNAMED",
                    "--add-opens=java.base/java.text=ALL-UNNAMED",
                    "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
                    "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
                    "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
                )
            }
        }
    }
    namespace = "com.rojas.syncpass"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.rojas.syncpass"
        minSdk = 24
        targetSdk = 37
        versionCode = 10
        versionName = "1.7.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // AppAuth: scheme del redirect custom (com.rojas.syncpass://cognito/callback).
        manifestPlaceholders["appAuthRedirectScheme"] = "com.rojas.syncpass"

        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "COGNITO_ISSUER", "\"$cognitoIssuer\"")
        buildConfigField("String", "COGNITO_CLIENT_ID", "\"$cognitoClientId\"")
        buildConfigField("String", "COGNITO_REDIRECT_URI", "\"$cognitoRedirect\"")
        buildConfigField("String", "COGNITO_LOGOUT_URI", "\"$cognitoLogout\"")
        buildConfigField("String", "COGNITO_SCOPES", "\"$cognitoScopes\"")
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = providers.gradleProperty("syncpassInstrumentedCoverage").orNull == "true"
            if (providers.gradleProperty("syncpassTestInstall").orNull == "true") {
                applicationIdSuffix = ".validation"
            }
        }
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true // agregado: no existía en la plantilla inicial
    }
}

dependencies {
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.robolectric)
    implementation(libs.maplibre.android)
    implementation(libs.hilt.android)
    implementation(libs.hilt.viewmodel.compose)
    ksp(libs.hilt.compiler)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.18.0")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended) // Visibility/VisibilityOff viven aquí, no en core
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    // Módulo reutilizable de login Cognito (ver cognito-auth/README.md)
    implementation(project(":cognito-auth"))
    implementation(libs.appauth)

    // Persistencia local offline-first
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Navegación y sincronización en segundo plano
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)

    // QR: generación (ZXing) y lectura (CameraX + ML Kit)
    implementation(libs.zxing.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)
}

val verifyDebugUnitTestCoverage = tasks.register("verifyDebugUnitTestCoverage") {
    group = "verification"
    description = "Exige al menos 70 % de líneas en la suite local JUnit, sin cobertura instrumentada."
    dependsOn("createDebugUnitTestCoverageReport")
    val report = layout.buildDirectory.file("reports/coverage/test/debug/report.xml")
    inputs.file(report)
    inputs.property("minimumLineCoveragePercent", 70)
    doLast {
        val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        }
        val root = factory.newDocumentBuilder().parse(report.get().asFile).documentElement
        val counter = (0 until root.childNodes.length).mapNotNull {
            root.childNodes.item(it) as? org.w3c.dom.Element
        }.single { it.tagName == "counter" && it.getAttribute("type") == "LINE" }
        val covered = counter.getAttribute("covered").toLong()
        val total = covered + counter.getAttribute("missed").toLong()
        check(total > 0 && covered * 100 >= total * 70) {
            "Cobertura local insuficiente: $covered/$total líneas. Mínimo requerido: 70 %."
        }
        logger.lifecycle("Cobertura local JUnit: {} % ({}/{} líneas); mínimo: 70 %.",
            String.format(Locale.ROOT, "%.2f", covered * 100.0 / total), covered, total)
    }
}

tasks.named("check") { dependsOn(verifyDebugUnitTestCoverage) }

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

if (providers.gradleProperty("syncpassInstrumentedCoverage").orNull == "true") {
    val unitCoverage = layout.buildDirectory.file("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
    val deviceCoverage = fileTree(layout.buildDirectory.dir("outputs/code_coverage/debugAndroidTest")) {
        include("**/*.ec")
    }
    tasks.register<JacocoReport>("syncPassDebugCoverageReport") {
        group = "verification"
        description = "Combina la cobertura unitaria e instrumentada del módulo Android :app."
        dependsOn("createDebugUnitTestCoverageReport", "createDebugAndroidTestCoverageReport")
        executionData.setFrom(unitCoverage, deviceCoverage)
        classDirectories.setFrom(
            layout.buildDirectory.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes"),
            layout.buildDirectory.dir("intermediates/javac/debug/compileDebugJavaWithJavac/classes"),
        )
        sourceDirectories.setFrom("src/main/java")
        reports {
            html.required.set(true)
            xml.required.set(true)
        }
    }
}
