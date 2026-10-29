package com.duoc.cursos.service;

import com.duoc.cursos.config.CorsProperties;
import com.duoc.cursos.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.DefaultCorsProcessor;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.*;

class WebCorsPolicyTest {
    private boolean preflight(String origin) throws Exception {
        var properties = new CorsProperties();
        properties.setAllowedOrigins("http://localhost:5173");
        properties.setAllowedHeaders("Authorization,Content-Type");
        var source = new SecurityConfig(properties, "issuer", "jwks", "android,web").corsConfigurationSource();
        var request = new MockHttpServletRequest("OPTIONS", "/api/proyectos");
        request.addHeader("Origin", origin);
        request.addHeader("Access-Control-Request-Method", "POST");
        request.addHeader("Access-Control-Request-Headers", "authorization,content-type");
        return new DefaultCorsProcessor().processRequest(source.getCorsConfiguration(request), request, new MockHttpServletResponse());
    }
    @Test void localhostCanUseBearerTokenAndJson() throws Exception {
        assertTrue(preflight("http://localhost:5173"));
    }
    @Test void unlistedOriginIsRejected() throws Exception {
        assertFalse(preflight("https://unknown.example"));
    }
}
