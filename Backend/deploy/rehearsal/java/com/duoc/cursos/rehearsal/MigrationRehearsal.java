package com.duoc.cursos.rehearsal;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import java.nio.file.*;
import java.sql.*;
import java.security.MessageDigest;
import java.util.*;

/** Ensayo con datos ficticios. No acepta URLs ni credenciales de producción. */
public final class MigrationRehearsal {
    private static final String CONTAINER = "syncpass-migration-rehearsal-20261125";
    private static final String ROOT = "jdbc:postgresql://127.0.0.1:55432/";
    private static final String SOURCE = "syncpass_rehearsal";
    private static final String COPY = "syncpass_rehearsal_copy";
    private static final String USER = "rehearsal";
    private static final String PASSWORD = System.getenv().getOrDefault("SYNCPASS_REHEARSAL_PASSWORD", "local-rehearsal-only");
    private static final List<String> TABLES = List.of("usuarios", "usuario_roles", "proyectos", "eventos", "inscripciones", "asistencias", "encuestas");

    public static void main(String[] args) throws Exception {
        if ("YES_LOCAL_PRODUCTION_COPY".equals(System.getenv("SYNCPASS_MIGRATION_REHEARSAL"))) {
            realCopy();
            return;
        }
        if (!"YES_LOCAL_SYNTHETIC_ONLY".equals(System.getenv("SYNCPASS_MIGRATION_REHEARSAL")))
            throw new IllegalStateException("Activa explícitamente el ensayo local ficticio; consulta docs/13-ENSAYO-MIGRACIONES.md");
        try (var connection = connection(SOURCE); var statement = connection.createStatement()) {
            var tables = statement.executeQuery("SELECT count(*) FROM information_schema.tables WHERE table_schema='public'");
            tables.next(); check(tables.getLong(1) == 0, "La base fuente debe estar vacía; no se sobrescriben datos previos");
        }
        flyway(SOURCE, "2").migrate();
        try (var connection = connection(SOURCE); var statement = connection.createStatement()) {
            statement.execute("""
                INSERT INTO usuarios(id,cognito_sub,cognito_issuer,email,nombre,creado_en,actualizado_en) VALUES
                (1,'docente-ficticio','https://issuer.invalid','docente@example.invalid','Docente ficticio',now(),now()),
                (2,'participante-ficticio','https://issuer.invalid','participante@example.invalid','Participante ficticio',now(),now());
                INSERT INTO usuario_roles VALUES (1,'DOCENTE'),(2,'PARTICIPANTE');
                INSERT INTO proyectos(id,nombre,estado,responsable_id,creado_en,actualizado_en)
                    VALUES (1,'Proyecto de ensayo','PLANIFICADO',1,now(),now());
                INSERT INTO eventos(id,proyecto_id,nombre,fecha_hora,cupos,latitud,longitud,radio_metros,estado,creado_en,actualizado_en)
                    VALUES (1,1,'Evento de ensayo','2026-11-25T12:00:00',20,-33.45,-70.66,200,'PROGRAMADO',now(),now());
                INSERT INTO inscripciones(id,evento_id,usuario_id,estado,creado_en) VALUES (1,1,2,'ASISTIO',now());
                INSERT INTO asistencias(id,evento_id,usuario_id,es_presente,marcado_en,origen,client_uuid,creado_en,actualizado_en)
                    VALUES (1,1,2,true,now(),'QR','asistencia-ficticia',now(),now());
                INSERT INTO encuestas(id,asistencia_id,satisfaccion,utilidad,comentarios,enviado_en,creado_en)
                    VALUES (1,1,5,4,'Comentario ficticio',now(),now());
                SELECT setval(pg_get_serial_sequence('usuarios','id'),2);
                SELECT setval(pg_get_serial_sequence('proyectos','id'),1);
                SELECT setval(pg_get_serial_sequence('eventos','id'),1);
                SELECT setval(pg_get_serial_sequence('inscripciones','id'),1);
                SELECT setval(pg_get_serial_sequence('asistencias','id'),1);
                SELECT setval(pg_get_serial_sequence('encuestas','id'),1);
                """);
        }
        var original = snapshot(SOURCE);
        run("docker", "exec", CONTAINER, "pg_dump", "-U", USER, "-d", SOURCE, "--format=custom", "--file=/tmp/syncpass-v2.dump");
        run("docker", "exec", CONTAINER, "pg_restore", "--list", "/tmp/syncpass-v2.dump");
        run("docker", "exec", CONTAINER, "createdb", "-U", USER, COPY);
        run("docker", "exec", CONTAINER, "pg_restore", "-U", USER, "-d", COPY, "--exit-on-error", "--single-transaction", "/tmp/syncpass-v2.dump");
        check(original.equals(snapshot(COPY)), "La restauración debe conservar todas las filas de negocio");
        var migration = flyway(COPY, null);
        var result = migration.migrate();
        check(result.migrationsExecuted == 2, "La copia V2 debe aplicar exactamente V3 y V4");
        migration.validate();
        check(original.equals(snapshot(COPY)), "La migración debe conservar todos los campos anteriores");
        check(flyway(SOURCE, "2").info().current().getVersion().toString().equals("2"), "La fuente debe permanecer en V2");
        try (var connection = connection(COPY); var statement = connection.createStatement()) {
            var rs = statement.executeQuery("SELECT count(*) FROM asistencias WHERE ubicacion_en IS NULL AND validacion_geografica IS NULL AND motivo_geografico IS NULL");
            rs.next(); check(rs.getLong(1) == 1, "La asistencia histórica no debe inventar evidencia geográfica");
            statement.execute("INSERT INTO creaciones_operaciones(usuario_id,tipo,client_uuid,huella,recurso_id) VALUES (1,'PROYECTO','00000000-0000-0000-0000-000000000001',repeat('a',64),1)");
            try {
                statement.execute("INSERT INTO creaciones_operaciones(usuario_id,tipo,client_uuid,huella,recurso_id) VALUES (1,'PROYECTO','00000000-0000-0000-0000-000000000001',repeat('a',64),1)");
                throw new IllegalStateException("La restricción única no impidió el duplicado");
            } catch (SQLException expected) { check("23505".equals(expected.getSQLState()), "Se esperaba la restricción única PostgreSQL"); }
        }
        check(migration.migrate().migrationsExecuted == 0, "Repetir Flyway no debe reaplicar migraciones");
        check(original.equals(snapshot(COPY)), "Las filas y relaciones originales deben conservarse al finalizar");
        Path artifacts = Path.of("build", "migration-rehearsal").toAbsolutePath();
        Files.createDirectories(artifacts);
        Path backup = artifacts.resolve("FICTICIO-syncpass-v2.dump");
        run("docker", "cp", CONTAINER + ":/tmp/syncpass-v2.dump", backup.toString());
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(backup)));
        String report = "ENSAYO LOCAL CON DATOS FICTICIOS; NO ES RESPALDO DE EC2\n" +
            "Fuente: V2; copia restaurada: V4; segunda ejecución: 0 migraciones\n" +
            "Filas y campos originales conservados: " + original.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue().size()).toList() + "\n" +
            "Integridad referencial y unicidad: correctas\nSHA-256 respaldo ficticio: " + hash + "\n";
        Files.writeString(artifacts.resolve("resultado.txt"), report);
        System.out.println(report);
    }

    private static void realCopy() throws Exception {
        check(System.getenv("SYNCPASS_REHEARSAL_PASSWORD") != null && PASSWORD.length() >= 24,
            "La copia real exige una contraseña local privada, distinta de la del ensayo ficticio");
        String database = "syncpass_real_rehearsal_20261125";
        Path backup = Path.of(Objects.requireNonNull(System.getenv("SYNCPASS_REAL_BACKUP"), "Falta la ruta del respaldo privado"));
        check(Files.isRegularFile(backup), "No existe el respaldo privado");
        // createdb aborta si ya existe: nunca se limpia ni sobrescribe una copia previa.
        if (!"YES_EXISTING_LOCAL_V2_COPY".equals(System.getenv("SYNCPASS_RESUME_REHEARSAL"))) {
            run("docker", "exec", CONTAINER, "createdb", "-U", USER, database);
            run("docker", "cp", backup.toAbsolutePath().toString(), CONTAINER + ":/tmp/private-production-copy.dump");
            run("docker", "exec", CONTAINER, "chmod", "0600", "/tmp/private-production-copy.dump");
            run("docker", "exec", CONTAINER, "pg_restore", "-U", USER, "-d", database, "--no-owner", "--no-privileges",
                "--exit-on-error", "--single-transaction", "/tmp/private-production-copy.dump");
        }
        var migration = flyway(database, null);
        check("2".equals(migration.info().current().getVersion().toString()), "La copia real debe partir de V2");
        var before = snapshot(database);
        // migrate valida primero los checksums aplicados y admite las migraciones nuevas pendientes.
        check(migration.migrate().migrationsExecuted == 2, "Deben aplicarse V3 y V4 sobre la copia");
        migration.validate();
        check(before.equals(snapshot(database)), "Deben conservarse todos los campos y filas originales");
        check(migration.migrate().migrationsExecuted == 0, "Flyway no debe reaplicar las migraciones");
        try (var connection = connection(database); var statement = connection.createStatement()) {
            var rs = statement.executeQuery("SELECT count(*) FROM asistencias WHERE ubicacion_en IS NOT NULL OR validacion_geografica IS NOT NULL OR motivo_geografico IS NOT NULL");
            rs.next(); check(rs.getLong(1) == 0, "No se debe inventar evidencia geográfica histórica");
        }
        Path artifacts = Path.of("build", "migration-rehearsal").toAbsolutePath();
        Files.createDirectories(artifacts);
        String report = "COPIA LOCAL DE RESPALDO REAL; PRODUCCIÓN NO FUE MIGRADA\n" +
            "Esquema V2 → V4, checksums válidos; repetición: 0 migraciones\n" +
            "Filas y campos originales conservados: " + before.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue().size()).toList() + "\n" +
            "La copia real permanece exclusivamente en el contenedor de ensayo local.\n";
        Files.writeString(artifacts.resolve("resultado-copia-real.txt"), report);
        System.out.println(report);
    }

    private static Flyway flyway(String db, String target) {
        var config = Flyway.configure().dataSource(ROOT + db, USER, PASSWORD).locations("filesystem:src/main/resources/db/migration");
        if (target != null) config.target(MigrationVersion.fromVersion(target));
        return config.load();
    }
    private static Connection connection(String db) throws SQLException { return DriverManager.getConnection(ROOT + db, USER, PASSWORD); }
    private static Map<String, List<String>> snapshot(String db) throws SQLException {
        var snapshot = new LinkedHashMap<String, List<String>>();
        try (var connection = connection(db); var statement = connection.createStatement()) {
            for (String table : TABLES) {
                String row = table.equals("asistencias") ? "to_jsonb(t)-'ubicacion_en'-'validacion_geografica'-'motivo_geografico'" : "to_jsonb(t)";
                var rs = statement.executeQuery("SELECT (" + row + ")::text FROM " + table + " t ORDER BY 1");
                var rows = new ArrayList<String>(); while (rs.next()) rows.add(rs.getString(1));
                snapshot.put(table, rows);
            }
        }
        return snapshot;
    }
    private static void check(boolean valid, String message) { if (!valid) throw new IllegalStateException(message); }
    private static void run(String... command) throws Exception {
        var process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        if (process.waitFor() != 0) throw new IllegalStateException("Falló la herramienta operativa: " + output);
    }
}
