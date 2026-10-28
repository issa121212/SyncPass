package com.duoc.cursos.service;
import com.duoc.cursos.integration.CognitoAuthGateway;
import com.duoc.cursos.integration.CognitoProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest
@AutoConfigureMockMvc
class SecurityBoundaryIntegrationTest {
    @Autowired MockMvc mvc;
    @MockitoBean CognitoAuthGateway gateway;
    @MockitoBean CognitoProfileService profiles;
    @Test void publicSignupRejectsRoleInjectionBeforeCallingProvider() throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"ana@example.com\",\"password\":\"long-password\",\"nombre\":\"Ana\",\"roles\":[\"ADMIN\"]}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(gateway);
    }
    @Test void participantCanReadOwnQrAndCannotReadAttendanceLists() throws Exception {
        var participant = jwt().jwt(j -> j.issuer("https://cognito-idp.us-east-1.amazonaws.com/us-east-1_test")
            .subject("qr-participant").claim("cognito:groups",List.of("PARTICIPANTE")))
            .authorities(new SimpleGrantedAuthority("ROLE_PARTICIPANTE"));
        mvc.perform(get("/api/usuarios/me/qr").with(participant)).andExpect(status().isOk())
            .andExpect(jsonPath("$.contenido").isNotEmpty());
        mvc.perform(get("/api/eventos/1/asistentes").with(participant)).andExpect(status().isForbidden());
    }
    @Test void anonymousBusinessRequestsAreRejected() throws Exception {
        mvc.perform(get("/api/proyectos")).andExpect(status().isUnauthorized());
    }
}
