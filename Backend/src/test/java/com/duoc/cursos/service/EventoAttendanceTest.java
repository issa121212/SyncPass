package com.duoc.cursos.service;

import com.duoc.cursos.dto.ApiDtos.AsistenciaRequest;
import com.duoc.cursos.model.*;
import com.duoc.cursos.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventoAttendanceTest {
    EventoRepository events;
    AsistenciaRepository attendances;
    InscripcionRepository registrations;
    UsuarioService users;
    ProyectoService projects;
    EventoService service;
    Evento event;
    Usuario receiver;
    Usuario participant;

    @BeforeEach void setup() {
        events = mock(EventoRepository.class); attendances = mock(AsistenciaRepository.class);
        registrations = mock(InscripcionRepository.class); users = mock(UsuarioService.class);
        projects = mock(ProyectoService.class);
        service = new EventoService(events, registrations, attendances, projects, users, mock(EncuestaService.class));
        receiver = user(2L, "VOLUNTARIO"); participant = user(3L, "PARTICIPANTE");
        event = new Evento(); event.setId(7L); event.setEstado(EstadoEvento.PROGRAMADO); event.setCupos(20);
        var project = new Proyecto(); project.setResponsable(user(1L, "DOCENTE")); event.setProyecto(project);
        when(events.findForAttendance(7L)).thenReturn(Optional.of(event));
        when(users.getCurrentUsuario()).thenReturn(receiver);
        when(users.requireById(3L)).thenReturn(participant);
        when(attendances.save(any())).thenAnswer(i -> i.getArgument(0));
    }
    Usuario user(Long id, String role) {
        var u = new Usuario(); u.setId(id); u.setRoles(Set.of(role)); u.setNombre("Nombre Cognito"); return u;
    }
    AsistenciaRequest request(String qr) { return new AsistenciaRequest(qr, true, null, null, null, null, "uuid-1"); }

    @Test void volunteerCanScanParticipantWithoutOwningProject() {
        var result = service.escanear(7L, request("SYNC_PASS:ASISTENCIA:7:3"));
        assertTrue(result.esPresente());
        verify(projects, never()).requireOwnerOrAdmin(any());
        verify(registrations).save(any());
    }
    @Test void participantCannotScan() {
        receiver.setRoles(Set.of("PARTICIPANTE"));
        assertEquals(403, assertThrows(ResponseStatusException.class,
            () -> service.escanear(7L, request("SYNC_PASS:ASISTENCIA:7:3"))).getStatusCode().value());
        verify(attendances, never()).save(any());
    }
    @Test void ownAttendanceCannotBeWrittenEvenThroughService() {
        assertEquals(403, assertThrows(ResponseStatusException.class,
            () -> service.actualizarMiAsistencia(7L, request(null))).getStatusCode().value());
        verifyNoInteractions(attendances, registrations);
    }
    @Test void qrFromAnotherEventIsRejected() {
        assertEquals(422, assertThrows(ResponseStatusException.class,
            () -> service.escanear(7L, request("SYNC_PASS:ASISTENCIA:8:3"))).getStatusCode().value());
        verify(attendances, never()).save(any());
    }
    @Test void receiverCannotScanThemself() {
        receiver.setId(3L);
        assertEquals(403, assertThrows(ResponseStatusException.class,
            () -> service.escanear(7L, request("SYNC_PASS:ASISTENCIA:7:3"))).getStatusCode().value());
    }
    @Test void cancelledEventDoesNotAcceptAttendance() {
        event.setEstado(EstadoEvento.CANCELADO);
        assertEquals(422, assertThrows(ResponseStatusException.class,
            () -> service.escanear(7L, request("SYNC_PASS:ASISTENCIA:7:3"))).getStatusCode().value());
    }
    @Test void repeatedScanDoesNotModifyExistingAttendance() {
        var attendance = new Asistencia(); attendance.setEvento(event); attendance.setUsuario(participant);
        attendance.setEsPresente(true);
        when(attendances.findByEventoIdAndUsuarioId(7L, 3L)).thenReturn(Optional.of(attendance));
        assertTrue(service.escanear(7L, request("SYNC_PASS:ASISTENCIA:7:3")).esPresente());
        verify(attendances, never()).save(any()); verify(registrations, never()).save(any());
    }
    @Test void clientUuidCannotReturnAnotherPersonsAttendance() {
        var attendance = new Asistencia(); attendance.setEvento(event); attendance.setUsuario(user(9L,"PARTICIPANTE"));
        when(attendances.findByClientUuid("uuid-1")).thenReturn(Optional.of(attendance));
        assertEquals(409, assertThrows(ResponseStatusException.class,
            () -> service.escanear(7L, request("SYNC_PASS:ASISTENCIA:7:3"))).getStatusCode().value());
    }
    @Test void scannerTargetMustBeParticipant() {
        participant.setRoles(Set.of("VOLUNTARIO"));
        assertEquals(422, assertThrows(ResponseStatusException.class,
            () -> service.escanear(7L, request("SYNC_PASS:ASISTENCIA:7:3"))).getStatusCode().value());
    }
}
