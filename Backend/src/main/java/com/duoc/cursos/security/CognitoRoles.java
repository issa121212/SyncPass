package com.duoc.cursos.security;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Normaliza los grupos de Cognito a los roles internos de SyncPass.
 *
 * Roles soportados: DOCENTE, VOLUNTARIO (recepción/escaneo), PARTICIPANTE (beneficiario),
 * ESTUDIANTE (legacy/compatibilidad) y ADMIN.
 */
public final class CognitoRoles {

    public static final String DOCENTE = "DOCENTE";
    public static final String VOLUNTARIO = "VOLUNTARIO";
    public static final String PARTICIPANTE = "PARTICIPANTE";
    public static final String ESTUDIANTE = "ESTUDIANTE";
    public static final String ADMIN = "ADMIN";

    private CognitoRoles() {
    }

    public static Set<String> normalize(Iterable<String> roles) {
        Set<String> result = new LinkedHashSet<>();
        if (roles == null) {
            return result;
        }
        for (String role : roles) {
            if (role == null) {
                continue;
            }
            String value = role.trim().toLowerCase(Locale.ROOT);
            switch (value) {
                case "profesor", "docente", "teacher", "coordinador", "jefe", "responsable" -> result.add(DOCENTE);
                case "voluntario", "voluntarios", "recepcion", "recepcionista", "recepcionistas", "ayudante", "ayudantes" -> {
                    result.add(VOLUNTARIO);
                }
                case "participante", "participantes", "asistente", "asistentes", "beneficiario", "beneficiarios" -> {
                    result.add(PARTICIPANTE);
                }
                case "estudiante", "alumno", "alumnos", "student", "monitor", "monitores" -> {
                    // Grupo 'estudiante' usado por voluntarios de recepción según nueva lógica
                    result.add(VOLUNTARIO);
                }
                case "admin", "administrador", "superadmin" -> {
                    result.add(ADMIN);
                    result.add(DOCENTE);
                }
                default -> {
                    // Ignorar grupos ajenos al dominio.
                }
            }
        }
        return result;
    }

    public static Set<String> normalizeOrDefault(Iterable<String> roles) {
        Set<String> normalized = normalize(roles);
        if (normalized.isEmpty()) {
            normalized.add(PARTICIPANTE);
        }
        return normalized;
    }
}
