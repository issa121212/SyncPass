package com.duoc.cursos.service;
import com.duoc.cursos.integration.CognitoProfileService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.MediaType;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AttendanceFlowIntegrationTest {
    @Autowired MockMvc mvc;
    @MockitoBean CognitoProfileService profiles;

    RequestPostProcessor account(String subject, String group, String role) {
        return jwt().jwt(j -> j.issuer("https://cognito-idp.us-east-1.amazonaws.com/us-east-1_test").subject(subject).claim("cognito:groups", List.of(group)))
            .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
    long id(String json) {
        var m = java.util.regex.Pattern.compile("\"id\":(\\d+)").matcher(json);
        if (!m.find()) throw new AssertionError(json);
        return Long.parseLong(m.group(1));
    }
    @Test void teacherAndReceptionRegisterParticipantByQrWithCognitoNames() throws Exception {
        when(profiles.profile(any())).thenAnswer(inv -> {
            Jwt j = inv.getArgument(0);
            return new CognitoProfileService.Profile("Nombre " + j.getSubject(), j.getSubject() + "@example.com");
        });
        var teacher = account("docente", "docente", "DOCENTE");
        var participant = account("participante", "participante", "PARTICIPANTE");
        var receiver = account("recepcion", "estudiante", "VOLUNTARIO");
        mvc.perform(get("/api/auth/me").with(teacher)).andExpect(status().isOk());
        String projectJson = mvc.perform(post("/api/proyectos").with(teacher)
            .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Proyecto prueba\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.responsable.nombre").value("Nombre docente"))
            .andReturn().getResponse().getContentAsString();
        long project = id(projectJson);
        String eventJson = mvc.perform(post("/api/eventos").with(teacher).contentType(MediaType.APPLICATION_JSON)
            .content("{\"proyectoId\":" + project + ",\"nombre\":\"Evento prueba\",\"fechaHora\":\"2026-12-04T14:00:00\",\"cupos\":20}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long event = id(eventJson);
        String userJson = mvc.perform(get("/api/auth/me").with(participant)).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        long user = id(userJson);
        mvc.perform(post("/api/eventos/inscribirse").with(participant).contentType(MediaType.APPLICATION_JSON)
            .content("{\"codigoQr\":\"EVENTO_" + event + "\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.esPresente").value(false));
        mvc.perform(put("/api/eventos/" + event + "/asistencias/me").with(participant)
            .contentType(MediaType.APPLICATION_JSON).content("{\"esPresente\":true}"))
            .andExpect(status().isForbidden());
        String scan = "{\"codigoQr\":\"SYNC_PASS:ASISTENCIA:" + event + ":" + user + "\",\"clientUuid\":\"flow-test-" + event + "\"}";
        mvc.perform(post("/api/eventos/" + event + "/asistencias/escanear").with(participant)
            .contentType(MediaType.APPLICATION_JSON).content(scan)).andExpect(status().isForbidden());
        mvc.perform(post("/api/eventos/" + event + "/asistencias/escanear").with(receiver)
            .contentType(MediaType.APPLICATION_JSON).content(scan)).andExpect(status().isOk())
            .andExpect(jsonPath("$.esPresente").value(true));
        mvc.perform(post("/api/eventos/" + event + "/asistencias/escanear").with(receiver)
            .contentType(MediaType.APPLICATION_JSON).content(scan)).andExpect(status().isOk());
        mvc.perform(get("/api/eventos/" + event + "/asistentes").with(receiver)).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].nombre").value("Nombre participante"))
            .andExpect(jsonPath("$[0].presente").value(true))
            .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/eventos/" + event).with(participant)).andExpect(status().isOk())
            .andExpect(jsonPath("$.esPresente").value(true));
    }
}
