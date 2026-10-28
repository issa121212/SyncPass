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
class ProyectoCrudIntegrationTest {
    @Autowired MockMvc mvc;
    @MockitoBean CognitoProfileService profiles;
    RequestPostProcessor user(String subject, String role) {
        return jwt().jwt(j -> j.issuer("https://cognito-idp.us-east-1.amazonaws.com/us-east-1_test").subject(subject).claim("cognito:groups", List.of(role)))
            .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
    long id(String json) {
        var m = java.util.regex.Pattern.compile("\"id\":(\\d+)").matcher(json);
        if (!m.find()) throw new AssertionError(json);
        return Long.parseLong(m.group(1));
    }
    void profiles() {
        when(profiles.profile(any())).thenAnswer(inv -> {
            Jwt j = inv.getArgument(0);
            return new CognitoProfileService.Profile("Nombre " + j.getSubject(), j.getSubject() + "@example.com");
        });
    }
    long create(String name) throws Exception {
        return id(mvc.perform(post("/api/proyectos").with(user("crud-owner", "DOCENTE"))
            .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"" + name + "\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }
    @Test void fullCrudAndOwnershipAreEnforcedByApi() throws Exception {
        profiles(); long id = create("Proyecto CRUD API");
        mvc.perform(get("/api/proyectos/" + id).with(user("crud-owner","DOCENTE")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Proyecto CRUD API"));
        mvc.perform(put("/api/proyectos/" + id).with(user("another-teacher","DOCENTE"))
            .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"No permitido\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(put("/api/proyectos/" + id).with(user("crud-owner","DOCENTE"))
            .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Proyecto editado\",\"descripcion\":\"Cambio por API\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Proyecto editado"));
        mvc.perform(delete("/api/proyectos/" + id).with(user("another-teacher","DOCENTE")))
            .andExpect(status().isForbidden());
        mvc.perform(delete("/api/proyectos/" + id).with(user("crud-owner","DOCENTE")))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/proyectos/" + id).with(user("crud-owner","DOCENTE")))
            .andExpect(status().isNotFound());
    }
    @Test void projectWithEventsCannotBeDeleted() throws Exception {
        profiles(); long id = create("Con historial");
        mvc.perform(post("/api/eventos").with(user("crud-owner","DOCENTE"))
            .contentType(MediaType.APPLICATION_JSON).content("{\"proyectoId\":" + id + ",\"nombre\":\"Evento\",\"fechaHora\":\"2026-12-04T14:00:00\"}"))
            .andExpect(status().isCreated());
        mvc.perform(delete("/api/proyectos/" + id).with(user("crud-owner","DOCENTE")))
            .andExpect(status().isConflict());
        mvc.perform(get("/api/proyectos/" + id).with(user("crud-owner","DOCENTE")))
            .andExpect(status().isOk());
    }
    @Test void participantCannotCreateProjects() throws Exception {
        mvc.perform(post("/api/proyectos").with(user("crud-participant","PARTICIPANTE"))
            .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"No permitido\"}"))
            .andExpect(status().isForbidden());
    }
}
