package com.duoc.cursos.rehearsal;

import com.duoc.cursos.CursosApplication;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import com.sun.net.httpserver.HttpServer;
import org.springframework.boot.builder.SpringApplicationBuilder;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/** Identidad ficticia local; no altera la configuración de seguridad de producción. */
public final class AndroidRehearsalServer {
    private static final String ISSUER = "http://127.0.0.1:18082";
    private static final String CLIENT = "rehearsal-android";

    public static void main(String[] args) throws Exception {
        if (!"YES_LOCAL_PRODUCTION_COPY".equals(System.getenv("SYNCPASS_MIGRATION_REHEARSAL")))
            throw new IllegalStateException("Este servidor requiere la activación explícita del ensayo local");
        String password = Objects.requireNonNull(System.getenv("SYNCPASS_REHEARSAL_PASSWORD"));
        if (password.length() < 24) throw new IllegalStateException("La contraseña local debe ser privada");
        RSAKey key = new RSAKeyGenerator(2048).keyID(UUID.randomUUID().toString()).generate();
        String teacher = token(key, "DOCENTE", "docente");
        String participant = token(key, "PARTICIPANTE", "participante");
        var identity = HttpServer.create(new InetSocketAddress("127.0.0.1", 18082), 0);
        identity.createContext("/jwks", request -> reply(request, new JWKSet(key.toPublicJWK()).toString()));
        identity.createContext("/tokens", request -> reply(request, "{\"teacher\":\"" + teacher + "\",\"participant\":\"" + participant + "\"}"));
        identity.createContext("/.well-known/openid-configuration", request -> reply(request, "{\"issuer\":\"" + ISSUER + "\"}"));
        identity.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> identity.stop(0)));
        try {
            new SpringApplicationBuilder(CursosApplication.class).run(
                "--spring.profiles.active=android-rehearsal",
                "--spring.datasource.url=jdbc:postgresql://127.0.0.1:55432/syncpass_real_rehearsal_20261125",
                "--spring.datasource.username=rehearsal", "--spring.datasource.password=" + password,
                "--spring.jpa.hibernate.ddl-auto=validate", "--spring.jpa.show-sql=false",
                "--server.address=127.0.0.1", "--server.port=18081",
                "--spring.security.oauth2.resourceserver.jwt.issuer-uri=" + ISSUER,
                "--spring.security.oauth2.resourceserver.jwt.jwk-set-uri=" + ISSUER + "/jwks",
                "--aws.cognito.allowed-client-ids=" + CLIENT, "--aws.cognito.client-id=" + CLIENT,
                "--app.attendance.geo-mode=ENFORCE"
            );
        } catch (Exception error) { identity.stop(0); throw error; }
    }

    private static String token(RSAKey key, String role, String account) throws Exception {
        Instant now = Instant.now();
        var claims = new JWTClaimsSet.Builder().issuer(ISSUER)
            .subject("rehearsal-android-" + account + "-20261125")
            .claim("name", "Ensayo " + account).claim("email", account + "@example.invalid")
            .claim("token_use", "access").claim("client_id", CLIENT).claim("cognito:groups", List.of(role))
            .issueTime(Date.from(now)).expirationTime(Date.from(now.plusSeconds(3600))).build();
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims);
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }
    private static void reply(com.sun.net.httpserver.HttpExchange request, String content) throws java.io.IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        request.getResponseHeaders().set("Content-Type", "application/json");
        request.sendResponseHeaders(200, bytes.length);
        try (var out = request.getResponseBody()) { out.write(bytes); }
    }
}
