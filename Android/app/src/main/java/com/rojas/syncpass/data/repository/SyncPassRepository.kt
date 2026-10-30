package com.rojas.syncpass.data.repository
import com.rojas.syncpass.domain.model.*
import com.rojas.syncpass.domain.*
import com.rojas.syncpass.data.mapper.*
import com.rojas.syncpass.data.remote.resultado
import com.rojas.syncpass.data.remote.toAppError
import com.rojas.syncpass.data.local.EventoEntity
import com.rojas.syncpass.data.local.PendienteEntity
import com.rojas.syncpass.data.local.PerfilEntity
import com.rojas.syncpass.data.local.ProyectoEntity
import com.rojas.syncpass.data.local.SyncPassDao
import com.rojas.syncpass.data.remote.AsistenciaRequest
import com.rojas.syncpass.data.remote.AsistenciaSyncItem
import com.rojas.syncpass.data.remote.AsistenteResponse
import com.rojas.syncpass.data.remote.EncuestaEventoResponse
import com.rojas.syncpass.data.remote.EncuestaRequest
import com.rojas.syncpass.data.remote.EncuestaSyncItem
import com.rojas.syncpass.data.remote.EventoRequest
import com.rojas.syncpass.data.remote.EventoResponse
import com.rojas.syncpass.data.remote.InscribirseRequest
import com.rojas.syncpass.data.remote.ProyectoRequest
import com.rojas.syncpass.data.remote.ProyectoResponse
import com.rojas.syncpass.data.remote.SyncPassApi
import com.rojas.syncpass.data.session.SessionStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import java.io.IOException
import java.time.Instant
import java.util.UUID
class SyncPassRepository(
    private val api: SyncPassApi,
    private val dao: SyncPassDao,
    private val sessionStore: SessionStore,
    recintoDao: com.rojas.syncpass.data.local.RecintoDao,
) : SyncPassOperations,
    ProyectosRepository by ApiProyectosRepository(api, dao),
    RecintosRepository by RoomRecintosRepository(recintoDao) {
    private val syncMutex = Mutex()
    override val eventos = dao.observarEventos().map { list -> list.map { it.toDomain() } }
    override val perfil: Flow<Perfil?> = dao.observarPerfil().map { cached ->
        cached?.copy(nombre = sessionStore.cognitoName().orEmpty())?.toDomain()
    }
    override val rechazadas = dao.observarRechazadas().map { list -> list.map {
        OperacionRechazada(it.tipo, it.eventoId, it.error ?: "Operacion rechazada")
    } }
    override val pendientes: Flow<Int> = dao.observarPendientes()
    override fun observarEvento(id: Long): Flow<Evento?> = dao.observarEvento(id).map { it?.toDomain() }
    /**
     * Descarga el catálogo y el perfil. Si el backend no responde, la app sigue
     * mostrando el catálogo cacheado en Room (modo offline-first).
     */
    override suspend fun refrescar(): Result<Unit> = syncMutex.withLock { resultado {
        val usuario = api.me()
        val qr = if (usuario.roles.any { it.equals("PARTICIPANTE", true) }) api.miQr().contenido else ""
        val proyectos = api.proyectos()
        val eventos = api.eventos()
        dao.guardarPerfil(
            PerfilEntity(
                usuarioId = usuario.id,
                nombre = sessionStore.cognitoName().orEmpty(),
                email = usuario.email,
                roles = usuario.roles.joinToString(","),
                qrContenido = qr,
            )
        )
        dao.reemplazarProyectos(proyectos.map { it.toEntity() })
        dao.reemplazarEventos(eventos.map { it.toEntity() })
    } }
    override suspend fun refrescarEvento(id: Long): Result<Evento> = syncMutex.withLock { resultado {
        val evento = api.evento(id).toEntity()
        dao.guardarEventoConPendientes(evento)
        evento.toDomain()
    } }
    override suspend fun inscribirse(evento: Evento): Result<Unit> = resultado {
        val codigo = evento.qrContenido ?: "EVENTO_${evento.id}"
        try {
            api.inscribirse(InscribirseRequest(codigo))
            dao.marcarInscrito(evento.id)
        } catch (e: Exception) {
            // Solo se encola si el fallo es de red o temporal. Un 4xx es una
            // regla de negocio (sin cupo, evento cancelado, sin rol, etc.) y
            // reintentarlo indefinidamente no lo va a resolver.
            if (e is CancellationException || !e.esFalloReintentable()) throw e
            dao.encolar(
                PendienteEntity(
                    clientUuid = UUID.randomUUID().toString(),
                    tipo = PendienteEntity.TIPO_INSCRIPCION,
                    eventoId = evento.id,
                    codigoQr = codigo,
                    marcadoEn = Instant.now().toString(),
                )
            )
            dao.marcarInscrito(evento.id)
        }
    }
    /**
     * El docente escanea el QR personal del estudiante. Se guarda localmente
     * antes de intentar la red: si falla, queda en la cola de sincronización.
     */
    override suspend fun registrarAsistenciaEscaneo(
        eventoId: Long,
        codigoQr: String,
        latitud: Double?,
        longitud: Double?,
        precisionMetros: Double?,
    ): Result<Boolean> = resultado {
        val uuid = UUID.randomUUID().toString()
        dao.encolar(
            PendienteEntity(
                clientUuid = uuid,
                tipo = PendienteEntity.TIPO_ASISTENCIA_ESCANEO,
                eventoId = eventoId,
                codigoQr = codigoQr,
                marcadoEn = Instant.now().toString(),
                latitud = latitud,
                longitud = longitud,
                precisionMetros = precisionMetros,
            )
        )
        sincronizar()
        dao.contarPendiente(uuid) == 0
    }
    override suspend fun guardarEncuesta(
        eventoId: Long,
        satisfaccion: Int,
        utilidad: Int,
        comentarios: String?,
    ): Result<Unit> = resultado {
        val uuid = UUID.randomUUID().toString()
        dao.encolar(
            PendienteEntity(
                clientUuid = uuid,
                tipo = PendienteEntity.TIPO_ENCUESTA,
                eventoId = eventoId,
                codigoQr = null,
                marcadoEn = Instant.now().toString(),
                satisfaccion = satisfaccion,
                utilidad = utilidad,
                comentarios = comentarios,
            )
        )
        dao.marcarEncuesta(eventoId)
        sincronizar()
    }
    override suspend fun crearEvento(request: EventoInput): Result<Evento> = syncMutex.withLock { resultado {
        request.validar()
        val evento = api.crearEvento(request.toRequest())
        dao.guardarEvento(evento.toEntity())
        evento.toEntity().toDomain()
    } }
    override suspend fun editarEvento(id: Long, request: EventoInput): Result<Evento> = syncMutex.withLock { resultado {
        request.validar()
        val evento = api.editarEvento(id, request.toRequest()).toEntity()
        dao.guardarEventoConPendientes(evento)
        evento.toDomain()
    } }
    override suspend fun eliminarEvento(id: Long): Result<Unit> = syncMutex.withLock { resultado {
        require(dao.pendientesDelEvento(id) == 0) { "Sincroniza las operaciones pendientes antes de eliminar el evento" }
        api.eliminarEvento(id)
        dao.eliminarEvento(id)
    } }
    override suspend fun asistentes(eventoId: Long): Result<List<Asistente>> =
        resultado { api.asistentes(eventoId).map { it.toDomain() } }
    override suspend fun encuestas(eventoId: Long): Result<List<EncuestaEvento>> =
        resultado { api.encuestas(eventoId).map { it.toDomain() } }
    /**
     * Procesa la cola offline. Los errores de red dejan las operaciones
     * pendientes para el próximo intento; los errores de negocio quedan
     * marcados con un mensaje visible para el usuario.
     */
    override suspend fun sincronizar(): Result<Int> = syncMutex.withLock {
        resultado {
            val pendientes = dao.pendientes().sortedBy {
                when (it.tipo) {
                    PendienteEntity.TIPO_INSCRIPCION -> 0
                    PendienteEntity.TIPO_ENCUESTA -> 2
                    else -> 1
                }
            }
            var procesados = 0
            var transitorio: AppException? = null
            val eventosBloqueados = mutableSetOf<Long>()
            for (p in pendientes) {
                if (p.eventoId in eventosBloqueados) continue
                try {
                    when (p.tipo) {
                        PendienteEntity.TIPO_INSCRIPCION ->
                            api.inscribirse(InscribirseRequest(p.codigoQr.orEmpty()))
                        PendienteEntity.TIPO_ASISTENCIA_ESCANEO -> api.escanear(
                            p.eventoId, AsistenciaRequest(codigoQr = p.codigoQr,
                                marcadoEn = p.marcadoEn, latitud = p.latitud,
                                longitud = p.longitud, precisionMetros = p.precisionMetros,
                                clientUuid = p.clientUuid))
                        PendienteEntity.TIPO_ASISTENCIA_PROPIA,
                        PendienteEntity.TIPO_AUSENCIA_PROPIA -> error(
                            "La automarcación ya no está permitida; presenta tu QR a recepción")
                        PendienteEntity.TIPO_ENCUESTA -> api.responderEncuesta(
                            p.eventoId, EncuestaRequest(
                                satisfaccion = requireNotNull(p.satisfaccion),
                                utilidad = requireNotNull(p.utilidad),
                                comentarios = p.comentarios, enviadoEn = p.marcadoEn))
                        else -> error("Operación desconocida: ${p.tipo}")
                    }
                    dao.marcarSincronizado(p.clientUuid)
                    procesados++
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    val error = e.toAppError()
                    if (error.reintentable || error.codigo == ErrorCode.SESION) {
                        dao.marcarError(p.clientUuid, error.message.orEmpty())
                        transitorio = error
                    } else dao.marcarRechazado(p.clientUuid, error.message.orEmpty())
                    eventosBloqueados += p.eventoId
                }
            }
            dao.limpiarSincronizados()
            transitorio?.let { throw it }
            procesados
        }.onFailure { if (it is CancellationException) throw it }
    }
    override fun cerrarSesionLocal() = sessionStore.clear()
    /**
     * Un error es reintentable si no hubo respuesta (red) o si el servidor
     * respondió con un fallo transitorio. Los 4xx restantes son definitivos.
     */
    private fun Throwable.esFalloReintentable(): Boolean = when (this) {
        is IOException -> true
        is HttpException -> code() == 408 || code() == 429 || code() >= 500
        else -> false
    }
}
