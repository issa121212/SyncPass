package com.duoc.cursos.controller;

import com.duoc.cursos.dto.ApiDtos.*;
import com.duoc.cursos.service.EncuestaService;
import com.duoc.cursos.service.EventoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/eventos")
public class EventoController {

    private final EventoService eventoService;
    private final EncuestaService encuestaService;

    public EventoController(EventoService eventoService, EncuestaService encuestaService) {
        this.eventoService = eventoService;
        this.encuestaService = encuestaService;
    }

    @GetMapping
    public List<EventoResponse> listar(@RequestParam(required = false) Long proyectoId) {
        return eventoService.listar(proyectoId);
    }

    @GetMapping("/{id}")
    public EventoResponse obtener(@PathVariable Long id) {
        return eventoService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('DOCENTE','ADMIN')")
    public EventoResponse crear(@Valid @RequestBody EventoRequest request) {
        return eventoService.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCENTE','ADMIN')")
    public EventoResponse actualizar(@PathVariable Long id, @Valid @RequestBody EventoRequest request) {
        return eventoService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('DOCENTE','ADMIN')")
    public void eliminar(@PathVariable Long id) {
        eventoService.eliminar(id);
    }

    @GetMapping("/{id}/qr")
    @PreAuthorize("hasAnyRole('DOCENTE','ADMIN')")
    public QrResponse qr(@PathVariable Long id) {
        return eventoService.qr(id);
    }

    @PostMapping("/inscribirse")
    @PreAuthorize("hasAnyRole('PARTICIPANTE','ESTUDIANTE')")
    public EventoResponse inscribirse(@Valid @RequestBody InscribirseRequest request) {
        return eventoService.inscribirse(request);
    }

    @GetMapping("/{id}/asistentes")
    @PreAuthorize("hasAnyRole('DOCENTE','ADMIN','VOLUNTARIO')")
    public List<AsistenteResponse> asistentes(@PathVariable Long id) {
        return eventoService.asistentes(id);
    }

    @PostMapping("/{id}/asistencias/escanear")
    @PreAuthorize("hasAnyRole('DOCENTE','ADMIN','VOLUNTARIO')")
    public AsistenciaResponse escanear(@PathVariable Long id, @Valid @RequestBody AsistenciaRequest request) {
        return eventoService.escanear(id, request);
    }

    @GetMapping("/{id}/asistencias/me")
    @PreAuthorize("hasAnyRole('PARTICIPANTE','ESTUDIANTE')")
    public AsistenciaResponse miAsistencia(@PathVariable Long id) {
        return eventoService.miAsistencia(id);
    }

    @PutMapping("/{id}/asistencias/me")
    @PreAuthorize("denyAll()")
    public AsistenciaResponse actualizarMiAsistencia(@PathVariable Long id, @Valid @RequestBody AsistenciaRequest request) {
        return eventoService.actualizarMiAsistencia(id, request);
    }

    @PutMapping("/{id}/encuestas/me")
    @PreAuthorize("hasAnyRole('PARTICIPANTE','ESTUDIANTE')")
    public EncuestaResponse responderEncuesta(@PathVariable Long id, @Valid @RequestBody EncuestaRequest request) {
        return encuestaService.guardarPropia(id, request);
    }

    @GetMapping("/{id}/encuestas")
    @PreAuthorize("hasAnyRole('DOCENTE','ADMIN')")
    public List<EncuestaEventoResponse> encuestas(@PathVariable Long id) {
        return encuestaService.listarPorEvento(id);
    }
}
