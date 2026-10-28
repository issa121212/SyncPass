package com.duoc.cursos.dto;

import com.duoc.cursos.model.EstadoEvento;
import com.duoc.cursos.model.EstadoInscripcion;
import com.duoc.cursos.model.EstadoProyecto;
import com.duoc.cursos.model.OrigenAsistencia;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public final class ApiDtos {

    private ApiDtos() {
    }

    public record UsuarioResumen(Long id, String nombre, String email) {
    }

    public record UsuarioResponse(
            Long id,
            String cognitoSub,
            String email,
            String nombre,
            Set<String> roles
    ) {
    }

    public record ActualizarPerfilRequest(
            @NotBlank @Size(max = 160) String nombre
    ) {
    }

    public record QrResponse(String contenido) {
    }

    public record ProyectoRequest(
            @NotBlank @Size(max = 160) String nombre,
            @Size(max = 4000) String descripcion,
            @Size(max = 80) String areaTematica,
            @Size(max = 160) String unidadVecinal,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            EstadoProyecto estado
    ) {
    }

    public record ProyectoResponse(
            Long id,
            String nombre,
            String descripcion,
            String areaTematica,
            String unidadVecinal,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            EstadoProyecto estado,
            UsuarioResumen responsable,
            Instant creadoEn,
            Instant actualizadoEn
    ) {
    }

    public record EventoRequest(
            @NotNull Long proyectoId,
            @NotBlank @Size(max = 160) String nombre,
            @Size(max = 4000) String descripcion,
            @NotNull LocalDateTime fechaHora,
            @Min(0) Integer cupos,
            @Size(max = 160) String recinto,
            @Size(max = 240) String direccion,
            @jakarta.validation.constraints.DecimalMin("-90.0") @jakarta.validation.constraints.DecimalMax("90.0") Double latitud,
            @jakarta.validation.constraints.DecimalMin("-180.0") @jakarta.validation.constraints.DecimalMax("180.0") Double longitud,
            @Min(1) @Max(50000) Integer radioMetros,
            EstadoEvento estado
    ) {
    }

    public record EventoResponse(
            Long id,
            Long proyectoId,
            String proyectoNombre,
            String nombre,
            String descripcion,
            LocalDateTime fechaHora,
            Integer cupos,
            long inscritos,
            long presentes,
            String recinto,
            String direccion,
            Double latitud,
            Double longitud,
            Integer radioMetros,
            String qrContenido,
            EstadoEvento estado,
            Instant creadoEn,
            Instant actualizadoEn,
            Boolean inscrito,
            Boolean esPresente,
            Boolean encuestaRespondida
    ) {
    }

    public record InscripcionResponse(
            Long id,
            Long eventoId,
            UsuarioResumen usuario,
            EstadoInscripcion estado,
            Instant creadoEn
    ) {
    }

    public record InscribirseRequest(
            @NotBlank @Size(max = 256) String codigoQr
    ) {
    }

    public record AsistenciaRequest(
            @Size(max = 256) String codigoQr,
            Boolean esPresente,
            Instant marcadoEn,
            @jakarta.validation.constraints.DecimalMin("-90.0") @jakarta.validation.constraints.DecimalMax("90.0") Double latitud,
            @jakarta.validation.constraints.DecimalMin("-180.0") @jakarta.validation.constraints.DecimalMax("180.0") Double longitud,
            @jakarta.validation.constraints.DecimalMin("0.0") @jakarta.validation.constraints.DecimalMax("100000.0") Double precisionMetros,
            @Size(max = 64) String clientUuid
    ) {
    }

    public record EncuestaRequest(
            @NotNull @Min(1) @Max(5) Integer satisfaccion,
            @NotNull @Min(1) @Max(5) Integer utilidad,
            @Size(max = 2000) String comentarios,
            Instant enviadoEn
    ) {
    }

    public record EncuestaResponse(
            Long id,
            Integer satisfaccion,
            Integer utilidad,
            String comentarios,
            Instant enviadoEn
    ) {
    }

    public record AsistenciaResponse(
            Long id,
            Long eventoId,
            UsuarioResumen estudiante,
            boolean esPresente,
            Instant marcadoEn,
            Double latitud,
            Double longitud,
            Double precisionMetros,
            OrigenAsistencia origen,
            EncuestaResponse encuesta
    ) {
    }

    public record AsistenteResponse(
            Long usuarioId,
            String nombre,
            String email,
            boolean inscrito,
            boolean presente,
            Instant marcadoEn,
            EncuestaResponse encuesta
    ) {
    }

    public record EncuestaEventoResponse(
            Long id,
            Long eventoId,
            UsuarioResumen estudiante,
            Integer satisfaccion,
            Integer utilidad,
            String comentarios,
            Instant enviadoEn
    ) {
    }

    public record AsistenciaSyncItem(
            @Size(max = 64) String clientUuid,
            @NotNull Long eventoId,
            @NotBlank @Size(max = 256) String codigoQr,
            Instant marcadoEn,
            @jakarta.validation.constraints.DecimalMin("-90.0") @jakarta.validation.constraints.DecimalMax("90.0") Double latitud,
            @jakarta.validation.constraints.DecimalMin("-180.0") @jakarta.validation.constraints.DecimalMax("180.0") Double longitud,
            @jakarta.validation.constraints.DecimalMin("0.0") @jakarta.validation.constraints.DecimalMax("100000.0") Double precisionMetros
    ) {
    }

    public record EncuestaSyncItem(
            @NotNull Long eventoId,
            @NotNull @Min(1) @Max(5) Integer satisfaccion,
            @NotNull @Min(1) @Max(5) Integer utilidad,
            @Size(max = 2000) String comentarios,
            Instant enviadoEn
    ) {
    }

    public record SyncResult(
            int procesados,
            int errores,
            List<String> mensajes
    ) {
    }

    public record ErrorResponse(String error, String mensaje, int status, java.util.Map<String, String> detalles) {
        public ErrorResponse { detalles = java.util.Map.copyOf(detalles); }
    }

    public record RegistroRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 64) String password,
            @NotBlank @Size(max = 100) String nombre,
            /* Roles are selected by the application, never by public input. */
            @Size(max = 0) Set<String> roles
    ) {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {
    }

    public record LoginResponse(
            String accessToken,
            String idToken,
            String refreshToken,
            Integer expiresIn
    ) {
    }

    public record SolicitudPasswordRequest(
            @NotBlank @Email String email
    ) {
    }

    public record ConfirmarPasswordRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6, max = 6) String codigo,
            @NotBlank @Size(min = 8, max = 64) String nuevaContrasena
    ) {
    }
}
