package com.rojas.syncpass.data.repository

import com.google.gson.Gson
import com.rojas.syncpass.data.local.*
import com.rojas.syncpass.data.mapper.*
import com.rojas.syncpass.data.remote.*
import com.rojas.syncpass.domain.AppException
import com.rojas.syncpass.domain.ErrorCode
import com.rojas.syncpass.domain.model.*
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.util.UUID

/** Coordina creaciones durables; el llamador serializa con el mismo Mutex de sincronización. */
class OfflineCreations(private val api: SyncPassApi, private val store: CreationStore) {
    private val json = Gson()

    suspend fun project(input: ProyectoInput): Long {
        input.validar()
        val profile = teacher()
        val operation = CreationEntity(clientUuid = UUID.randomUUID().toString(), tipo = "PROYECTO", payload = json.toJson(input.toRequest()))
        return store.enqueueProject(operation, ProyectoEntity(0, input.nombre.trim(), input.descripcion,
            input.areaTematica, input.unidadVecinal, input.fechaInicio, input.fechaFin, requireNotNull(input.estado),
            profile.nombre, profile.email, profile.usuarioId, syncState = "PENDIENTE"))
    }

    suspend fun event(input: EventoInput): Long {
        input.validar()
        val profile = teacher()
        val effectiveProjectId = if (input.proyectoId < 0) store.operation(-input.proyectoId)?.remoteId ?: input.proyectoId else input.proyectoId
        val project = requireNotNull(store.project(effectiveProjectId)) { "El proyecto no está disponible en este dispositivo" }
        require(puedeGestionarProyecto(project.toDomain(), profile.toDomain())) { "No puedes crear eventos para este proyecto" }
        require(project.syncState != "RECHAZADO") { "El proyecto fue rechazado; revisa su motivo antes de crear eventos" }
        val operation = CreationEntity(clientUuid = UUID.randomUUID().toString(), tipo = "EVENTO",
            payload = json.toJson(input.toRequest()), proyectoId = effectiveProjectId)
        return store.enqueueEvent(operation, EventoEntity(id = 0, proyectoId = effectiveProjectId,
            proyectoNombre = project.nombre, nombre = input.nombre.trim(), descripcion = input.descripcion,
            fechaHora = input.fechaHora, cupos = input.cupos, inscritos = 0, presentes = 0,
            recinto = input.recinto, direccion = input.direccion, latitud = input.latitud, longitud = input.longitud,
            radioMetros = input.radioMetros, qrContenido = null, estado = requireNotNull(input.estado),
            inscrito = false, esPresente = false, encuestaRespondida = false, actualizadoEn = null, syncState = "PENDIENTE"))
    }

    private suspend fun teacher(): PerfilEntity {
        val profile = requireNotNull(store.profile()) { "Inicia sesión y descarga tu perfil antes de crear sin conexión" }
        require(profile.roles.split(",").any { it.trim().uppercase() in setOf("DOCENTE", "ADMIN") }) {
            "Solo una cuenta docente o administradora puede crear proyectos y eventos"
        }
        return profile
    }

    suspend fun projectResult(id: Long) {
        val operation = requireNotNull(store.operation(id))
        require(operation.estado != "RECHAZADO") { operation.error ?: "Proyecto rechazado" }
    }

    suspend fun eventResult(id: Long): Evento {
        val operation = requireNotNull(store.operation(id))
        require(operation.estado != "RECHAZADO") { operation.error ?: "Evento rechazado" }
        return requireNotNull(store.event(operation.remoteId ?: -id)).toDomain()
    }

    suspend fun synchronize(): Pair<Int, AppException?> {
        val pending = store.pending().sortedBy { if (it.tipo == "PROYECTO") 0 else 1 }
        if (pending.isEmpty()) return 0 to null
        try {
            if (!api.creationCapabilities().creacionIdempotente) throw AppException(ErrorCode.SERVIDOR, true,
                "El backend todavía no admite creaciones idempotentes; los datos siguen guardados")
        } catch (error: CancellationException) { throw error }
        catch (error: Exception) {
            val appError = if (error is HttpException && error.code() == 404) AppException(ErrorCode.SERVIDOR, true,
                "Actualiza el backend antes de sincronizar creaciones; los datos siguen guardados") else error.toAppError()
            val permanent = !appError.reintentable && appError.codigo != ErrorCode.SESION
            pending.forEach { store.error(it, permanent, appError.message.orEmpty()) }
            return 0 to if (permanent) null else appError
        }
        var completed = 0
        var transient: AppException? = null
        for (snapshot in pending) {
            val item = store.operation(snapshot.id) ?: continue
            if (item.estado != "PENDIENTE") continue
            try {
                when (item.tipo) {
                    "PROYECTO" -> {
                        val request = json.fromJson(item.payload, ProyectoRequest::class.java).copy(clientUuid = item.clientUuid)
                        val remote = api.crearProyecto(request)
                        if (remote.id <= 0) throw AppException(ErrorCode.SERVIDOR, true, "Respuesta de creación incompleta; se reintentará con el mismo UUID")
                        confirmLocally { store.confirmProject(item.id, remote.toEntity()) }
                    }
                    "EVENTO" -> {
                        var projectId = requireNotNull(item.proyectoId)
                        if (projectId < 0) {
                            val parent = requireNotNull(store.operation(-projectId)) { "No se encuentra el proyecto pendiente" }
                            require(parent.estado != "RECHAZADO") { "El proyecto de este evento fue rechazado: ${parent.error.orEmpty()}" }
                            projectId = parent.remoteId ?: continue
                        }
                        val request = json.fromJson(item.payload, EventoRequest::class.java).copy(proyectoId = projectId, clientUuid = item.clientUuid)
                        val remote = api.crearEvento(request)
                        if (remote.id <= 0 || remote.proyectoId != projectId) throw AppException(ErrorCode.SERVIDOR, true, "Respuesta de evento incompatible; se conservará el UUID")
                        confirmLocally { store.confirmEvent(item.id, remote.toEntity()) }
                    }
                    else -> error("Tipo de creación desconocido")
                }
                completed++
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                val appError = error.toAppError()
                val permanent = !appError.reintentable && appError.codigo != ErrorCode.SESION
                store.error(item, permanent, appError.message.orEmpty())
                if (!permanent) transient = appError
                if (appError.codigo == ErrorCode.SESION) break
            }
        }
        return completed to transient
    }

    private suspend fun confirmLocally(block: suspend () -> Unit) {
        try { block() }
        catch (error: CancellationException) { throw error }
        catch (error: Exception) { throw AppException(ErrorCode.SERVIDOR, true,
            "La creación fue enviada; falta completar su confirmación local", error) }
    }
}
