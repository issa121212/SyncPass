package com.rojas.syncpass.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface SyncPassApi {

    @GET("api/auth/me")
    suspend fun me(): UsuarioResponse

    @GET("api/usuarios/me/qr")
    suspend fun miQr(): QrResponse

    @GET("api/proyectos")
    suspend fun proyectos(): List<ProyectoResponse>

    @GET("api/proyectos/{id}")
    suspend fun proyecto(@Path("id") id: Long): ProyectoResponse

    @PUT("api/proyectos/{id}")
    suspend fun editarProyecto(@Path("id") id: Long, @Body request: ProyectoRequest): ProyectoResponse

    @DELETE("api/proyectos/{id}")
    suspend fun eliminarProyecto(@Path("id") id: Long)

    @POST("api/proyectos")
    suspend fun crearProyecto(@Body request: ProyectoRequest): ProyectoResponse

    @GET("api/eventos")
    suspend fun eventos(@Query("proyectoId") proyectoId: Long? = null): List<EventoResponse>

    @GET("api/eventos/{id}")
    suspend fun evento(@Path("id") id: Long): EventoResponse

    @POST("api/eventos")
    suspend fun crearEvento(@Body request: EventoRequest): EventoResponse

    @PUT("api/eventos/{id}")
    suspend fun editarEvento(@Path("id") id: Long, @Body request: EventoRequest): EventoResponse

    @DELETE("api/eventos/{id}")
    suspend fun eliminarEvento(@Path("id") id: Long)

    @POST("api/eventos/inscribirse")
    suspend fun inscribirse(@Body request: InscribirseRequest): EventoResponse

    @GET("api/eventos/{id}/asistentes")
    suspend fun asistentes(@Path("id") id: Long): List<AsistenteResponse>

    @POST("api/eventos/{id}/asistencias/escanear")
    suspend fun escanear(
        @Path("id") eventoId: Long,
        @Body request: AsistenciaRequest,
    ): AsistenciaResponse

    @GET("api/eventos/{id}/asistencias/me")
    suspend fun miAsistencia(@Path("id") eventoId: Long): AsistenciaResponse

    @PUT("api/eventos/{id}/asistencias/me")
    suspend fun actualizarMiAsistencia(
        @Path("id") eventoId: Long,
        @Body request: AsistenciaRequest,
    ): AsistenciaResponse

    @PUT("api/eventos/{id}/encuestas/me")
    suspend fun responderEncuesta(
        @Path("id") eventoId: Long,
        @Body request: EncuestaRequest,
    ): EncuestaResponse

    @GET("api/eventos/{id}/encuestas")
    suspend fun encuestas(@Path("id") eventoId: Long): List<EncuestaEventoResponse>

    @POST("api/sync/asistencias")
    suspend fun sincronizarAsistencias(@Body items: List<AsistenciaSyncItem>): SyncResult

    @POST("api/sync/encuestas")
    suspend fun sincronizarEncuestas(@Body items: List<EncuestaSyncItem>): SyncResult
}
