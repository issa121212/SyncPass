package com.duoc.cursos.rehearsal;

import org.flywaydb.core.Flyway;
import java.sql.*;
import java.nio.file.*;
import java.util.*;

/** V5 -> V6 e INSERT del contrato anterior, exclusivamente en la copia local. */
public final class PublicCodeCompatibilityRehearsal {
    private static final String URL = "jdbc:postgresql://127.0.0.1:55432/syncpass_real_rehearsal_20261125";
    private static final List<String> TABLES = List.of("usuarios", "usuario_roles", "proyectos", "eventos",
        "inscripciones", "asistencias", "encuestas", "creaciones_operaciones", "codigos_publicos");

    public static void main(String[] args) throws Exception {
        check("YES_LOCAL_PRODUCTION_COPY".equals(System.getenv("SYNCPASS_MIGRATION_REHEARSAL")), "Solo copia local");
        String password = Objects.requireNonNull(System.getenv("SYNCPASS_REHEARSAL_PASSWORD"));
        check(password.length() >= 24, "Se requiere la contraseña privada local");
        var flyway = Flyway.configure().dataSource(URL, "rehearsal", password)
            .locations("filesystem:src/main/resources/db/migration").target("6").load();
        check("5".equals(flyway.info().current().getVersion().toString()), "El ensayo debe partir de V5");
        Map<String, List<String>> original;
        try (var c = DriverManager.getConnection(URL, "rehearsal", password)) { original = snapshot(c); }
        check(flyway.migrate().migrationsExecuted == 1, "Se esperaba únicamente V6");
        flyway.validate();
        try (var c = DriverManager.getConnection(URL, "rehearsal", password); var s = c.createStatement()) {
            check(original.equals(snapshot(c)), "V6 cambió datos anteriores");
            c.setAutoCommit(false);
            var rs = s.executeQuery("""
                INSERT INTO proyectos(nombre,estado,responsable_id,creado_en,actualizado_en)
                SELECT 'Ensayo SQL antiguo','PLANIFICADO',id,now(),now() FROM usuarios ORDER BY id LIMIT 1
                RETURNING id, codigo_publico
                """);
            check(rs.next(), "No se creó el proyecto");
            long project = rs.getLong(1);
            String projectCode = rs.getString(2);
            check(projectCode.matches("PRY-[0-9A-F]{12}"), "Falta código por defecto del proyecto");
            try (var insert = c.prepareStatement("""
                INSERT INTO eventos(proyecto_id,nombre,fecha_hora,cupos,radio_metros,estado,creado_en,actualizado_en)
                VALUES (?,'Ensayo SQL antiguo','2026-12-05T14:30:00',0,200,'PROGRAMADO',now(),now())
                RETURNING codigo_publico
                """)) {
                insert.setLong(1, project);
                rs = insert.executeQuery(); check(rs.next(), "No se creó el evento");
                check(rs.getString(1).matches("EVT-[0-9A-F]{12}"), "Falta código por defecto del evento");
            }
            // Revertir también las reservas: ninguna fila de prueba debe permanecer.
            c.rollback();
            check(original.equals(snapshot(c)), "El rollback debe conservar también las reservas originales");
        }
        check(flyway.migrate().migrationsExecuted == 0, "No se debe reaplicar V6");
        Path report = Path.of("build/migration-rehearsal/compatibilidad-codigos.txt");
        Files.createDirectories(report.getParent());
        Files.writeString(report, "COPIA LOCAL V5 -> V6: datos y códigos anteriores sin cambios.\nINSERT antiguos omitiendo código público reciben códigos PRY/EVT válidos.\nRollback revierte recursos y reservas; segunda migración sin cambios.\n");
        System.out.println(Files.readString(report));
    }

    private static Map<String, List<String>> snapshot(Connection c) throws SQLException {
        var result = new LinkedHashMap<String, List<String>>();
        for (String table : TABLES) {
            try (var s = c.createStatement(); var rs = s.executeQuery("SELECT to_jsonb(t)::text FROM " + table + " t ORDER BY 1")) {
                var rows = new ArrayList<String>(); while (rs.next()) rows.add(rs.getString(1)); result.put(table, rows);
            }
        }
        return result;
    }
    private static void check(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}
