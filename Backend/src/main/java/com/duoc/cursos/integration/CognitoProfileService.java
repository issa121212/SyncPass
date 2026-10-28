package com.duoc.cursos.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Attributes come from Cognito userInfo, authenticated by the caller's access token. */
@Service
public class CognitoProfileService {
    public record Profile(String name, String email) {}
    private record Cached(Profile profile, Instant until) {}
    private final RestClient client;
    private final String issuer;
    private volatile String userInfoEndpoint;
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    public CognitoProfileService(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer) {
        this.issuer = issuer.replaceAll("/+$", "");
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3)).build());
        factory.setReadTimeout(Duration.ofSeconds(4));
        this.client = RestClient.builder().requestFactory(factory).build();
    }

    public Profile profile(Jwt jwt) {
        String key;
        try {
            key = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(jwt.getTokenValue().getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) { throw new IllegalStateException(ex); }
        var now = Instant.now();
        var cached = cache.get(key);
        if (cached != null && now.isBefore(cached.until())) return cached.profile();
        cache.entrySet().removeIf(e -> !now.isBefore(e.getValue().until()));
        Profile profile = new Profile(null, null);
        long seconds = 30;
        try {
            if (userInfoEndpoint == null) {
                var discovery = client.get().uri(issuer + "/.well-known/openid-configuration")
                        .retrieve().body(Map.class);
                if (discovery == null || !issuer.equals(discovery.get("issuer")))
                    throw new IllegalStateException("Issuer no coincide");
                String endpoint = (String) discovery.get("userinfo_endpoint");
                if (endpoint == null || !endpoint.startsWith("https://"))
                    throw new IllegalStateException("UserInfo no disponible");
                userInfoEndpoint = endpoint;
            }
            var attributes = client.get().uri(userInfoEndpoint)
                    .headers(h -> h.setBearerAuth(jwt.getTokenValue()))
                    .retrieve().body(Map.class);
            if (attributes == null || !jwt.getSubject().equals(attributes.get("sub")))
                throw new IllegalStateException("El perfil no corresponde a la sesion");
            profile = new Profile(text(attributes.get("name")), text(attributes.get("email")));
            seconds = 300;
        } catch (Exception ex) {
            // Keep existing local attributes during an outage; never use username/sub as a name.
            org.slf4j.LoggerFactory.getLogger(getClass()).warn(
                    "No se pudo actualizar el perfil desde Cognito ({})", ex.getClass().getSimpleName());
        }
        cache.put(key, new Cached(profile, now.plusSeconds(seconds)));
        return profile;
    }

    private String text(Object value) {
        return value instanceof String s && !s.isBlank() ? s.trim() : null;
    }
}
