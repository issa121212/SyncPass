package com.duoc.cursos.rehearsal;

import org.flywaydb.core.Flyway;
import java.sql.*;
import java.nio.file.*;
import java.util.*;

/** Ensayo de V5 en la copia local ya migrada a V4, nunca en EC2. */
public final class PublicCodesRehearsal {
    private static final String URL = "jdbc:postgresql://127.0.0.1:55432/syncpass_real_rehearsal_20261125";
    private static final List<String> TABLES = List.of("usuarios", "usuario_roles", "proyectos", "eventos",
        "inscripciones", "asistencias", "encuestas", "creaciones_operaciones");

    public static void main(String[] args) throws Exception {
        if (!"YES_LOCAL_PRODUCTION_COPY".equals(System.getenv("SYNCPASS_MIGRATION_REHEARSAL")))
            throw new IllegalStateException("Solo se admite la copia local aislada");
        String password = Objects.requireNonNull(System.getenv("SYNCPASS_REHEARSAL_PASSWORD"));
        if (password.length() < 24) throw new IllegalStateException("Se requiere contraseña local privada");
        var flyway = Flyway.configure().dataSource(URL, "rehearsal", password)
            .locations("filesystem:src/main/resources/db/migration").target("5").load();
        check("4".equals(flyway.info().current().getVersion().toString()), "El ensayo debe partir de V4");
        Map<String, List<String>> original;
        try (var c = DriverManager.getConnection(URL, "rehearsal", password)) { original = snapshot(c); }
        check(flyway.migrate().migrationsExecuted == 1, "Se esperaba únicamente V5");
        flyway.validate();
        try (var c = DriverManager.getConnection(URL, "rehearsal", password); var s = c.createStatement()) {
            check(original.equals(snapshot(c)), "Cambió algún dato anterior a V5");
            var rs = s.executeQuery("SELECT (SELECT count(*) FROM proyectos WHERE codigo_publico !~ '^PRY-[0-9A-F]{12}$') + (SELECT count(*) FROM eventos WHERE codigo_publico !~ '^EVT-[0-9A-F]{12}$')");
            rs.next(); check(rs.getLong(1) == 0, "Formato inválido en códigos históricos");
            rs = s.executeQuery("SELECT (SELECT count(*) FROM codigos_publicos) = ((SELECT count(*) FROM proyectos) + (SELECT count(*) FROM eventos))");
            rs.next(); check(rs.getBoolean(1), "Cada recurso debe tener su reserva única");
            c.setAutoCommit(false);
            rs = s.executeQuery("SELECT codigo_publico FROM proyectos ORDER BY id LIMIT 1");
            check(rs.next(), "Falta catálogo para probar unicidad");
            String code = rs.getString(1);
            try (var reservation = c.prepareStatement("INSERT INTO codigos_publicos(codigo) VALUES (?) ON CONFLICT DO NOTHING")) {
                reservation.setString(1, code);
                check(reservation.executeUpdate() == 0, "La reserva duplicada no debe insertarse");
                reservation.setString(1, "PRY-000000000001");
                check(reservation.executeUpdate() == 1, "Tras colisión la transacción debe permitir otra reserva");
            }
            var savepoint = c.setSavepoint();
            try {
                s.executeUpdate("UPDATE proyectos SET codigo_publico = (SELECT codigo_publico FROM proyectos ORDER BY id LIMIT 1) WHERE id = (SELECT id FROM proyectos ORDER BY id OFFSET 1 LIMIT 1)");
                throw new IllegalStateException("Se admitió un código duplicado");
            } catch (SQLException expected) { check("23505".equals(expected.getSQLState()), "Se esperaba restricción de unicidad"); c.rollback(savepoint); }
            c.rollback();
            check(original.equals(snapshot(c)), "La prueba alteró datos originales");
        }
        check(flyway.migrate().migrationsExecuted == 0, "V5 no debe reaplicarse");
        Path report = Path.of("build", "migration-rehearsal", "codigos-publicos.txt");
        Files.createDirectories(report.getParent());
        Files.writeString(report, "COPIA LOCAL: V4 → V5; campos y relaciones anteriores conservados.\nCódigos existentes válidos y únicos; reserva repetida retorna 0 y permite reintentar.\nRestricción única rechaza duplicado (23505). Segunda migración: 0 cambios.\n");
        System.out.println(Files.readString(report));
    }

    private static Map<String, List<String>> snapshot(Connection c) throws SQLException {
        var result = new LinkedHashMap<String, List<String>>();
        for (String table : TABLES) {
            try (var s = c.createStatement(); var rs = s.executeQuery("SELECT (to_jsonb(t)-'codigo_publico')::text FROM " + table + " t ORDER BY 1")) {
                var rows = new ArrayList<String>(); while (rs.next()) rows.add(rs.getString(1)); result.put(table, rows);
            }
        }
        return result;
    }
    private static void check(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
