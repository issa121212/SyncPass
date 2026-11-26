package com.duoc.cursos.service;

import com.duoc.cursos.dto.ApiDtos.AsistenciaRequest;
import com.duoc.cursos.model.Evento;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

/** Política compartida por el registro directo y la sincronización. */
@Component
public class AttendanceLocationPolicy {
    public enum Mode { AUDIT, ENFORCE }
    public record Decision(String status, String reason) {}
    private final Mode mode;

    public AttendanceLocationPolicy(@Value("${app.attendance.geo-mode:AUDIT}") String mode) {
        this.mode = Mode.valueOf(mode.toUpperCase(Locale.ROOT));
    }

    public Decision validate(Evento event, AsistenciaRequest request, Instant now) {
        if (event.getLatitud() == null && event.getLongitud() == null) return new Decision("SIN_ZONA", null);
        String error = violation(event, request, now);
        if (error != null && mode == Mode.ENFORCE) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, error);
        }
        return error == null ? new Decision("VALIDADA", null) : new Decision("AUDITORIA_RECHAZADA", error);
    }

    private String violation(Evento event, AsistenciaRequest request, Instant now) {
        if (!point(event.getLatitud(), event.getLongitud()) || event.getRadioMetros() == null ||
                event.getRadioMetros() < 1 || event.getRadioMetros() > 50000) return "GEO_ZONA_INVALIDA";
        if (request.latitud() == null || request.longitud() == null || request.precisionMetros() == null ||
                request.ubicacionEn() == null || request.marcadoEn() == null) return "GEO_DATOS_AUSENTES";
        if (!point(request.latitud(), request.longitud())) return "GEO_COORDENADAS_INVALIDAS";
        double accuracy = request.precisionMetros();
        if (!Double.isFinite(accuracy) || accuracy <= 0 || accuracy > Math.min(100.0, event.getRadioMetros() / 2.0))
            return "GEO_PRECISION_INSUFICIENTE";
        // La posición debe ser reciente respecto de la captura, no respecto de la sincronización.
        Duration locationAge = Duration.between(request.ubicacionEn(), request.marcadoEn());
        if (locationAge.isNegative() || locationAge.compareTo(Duration.ofMinutes(2)) > 0) return "GEO_UBICACION_ANTIGUA";
        Duration captureAge = Duration.between(request.marcadoEn(), now);
        if (captureAge.compareTo(Duration.ofSeconds(-30)) < 0 || captureAge.compareTo(Duration.ofHours(24)) > 0)
            return "GEO_CAPTURA_FUERA_DE_PLAZO";
        double distance = distance(event.getLatitud(), event.getLongitud(), request.latitud(), request.longitud());
        // El círculo de incertidumbre completo debe estar dentro del recinto; no se amplía el radio.
        if (distance + accuracy > event.getRadioMetros()) return "GEO_FUERA_DE_ZONA";
        return null;
    }

    private boolean point(Double latitude, Double longitude) {
        return latitude != null && longitude != null && Double.isFinite(latitude) && Double.isFinite(longitude) &&
                latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180;
    }

    private double distance(double latitude1, double longitude1, double latitude2, double longitude2) {
        double first = Math.toRadians(latitude1), second = Math.toRadians(latitude2);
        double a = Math.pow(Math.sin((second - first) / 2), 2) + Math.cos(first) * Math.cos(second) *
                Math.pow(Math.sin(Math.toRadians(longitude2 - longitude1) / 2), 2);
        return 6371008.8 * 2 * Math.asin(Math.sqrt(Math.clamp(a, 0.0, 1.0)));
    }
}
