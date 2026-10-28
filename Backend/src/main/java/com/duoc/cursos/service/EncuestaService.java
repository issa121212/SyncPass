package com.duoc.cursos.service;

import com.duoc.cursos.dto.ApiDtos.EncuestaEventoResponse;
import com.duoc.cursos.dto.ApiDtos.EncuestaRequest;
import com.duoc.cursos.dto.ApiDtos.EncuestaResponse;
import com.duoc.cursos.dto.ApiDtos.UsuarioResumen;
import com.duoc.cursos.model.Asistencia;
import com.duoc.cursos.model.Encuesta;
import com.duoc.cursos.model.Evento;
import com.duoc.cursos.model.Usuario;
import com.duoc.cursos.repository.AsistenciaRepository;
import com.duoc.cursos.repository.EncuestaRepository;
import com.duoc.cursos.repository.EventoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class EncuestaService {

    private final EncuestaRepository encuestaRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final EventoRepository eventoRepository;
    private final UsuarioService usuarioService;
    private final ProyectoService proyectoService;

    public EncuestaService(
            EncuestaRepository encuestaRepository,
            AsistenciaRepository asistenciaRepository,
            EventoRepository eventoRepository,
            UsuarioService usuarioService,
            ProyectoService proyectoService
    ) {
        this.encuestaRepository = encuestaRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.eventoRepository = eventoRepository;
        this.usuarioService = usuarioService;
        this.proyectoService = proyectoService;
    }

    @Transactional
    public EncuestaResponse guardarPropia(Long eventoId, EncuestaRequest request) {
        Usuario actual = usuarioService.getCurrentUsuario();
        Asistencia asistencia = asistenciaRepository.findByEventoIdAndUsuarioId(eventoId, actual.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No tienes asistencia registrada para este evento"));

        if (!asistencia.isEsPresente()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Solo puedes responder si estás marcado como presente");
        }

        Encuesta encuesta = encuestaRepository.findByAsistenciaId(asistencia.getId())
                .orElseGet(Encuesta::new);
        encuesta.setAsistencia(asistencia);
        encuesta.setSatisfaccion(request.satisfaccion());
        encuesta.setUtilidad(request.utilidad());
        encuesta.setComentarios(request.comentarios());
        encuesta.setEnviadoEn(request.enviadoEn() == null ? Instant.now() : request.enviadoEn());
        return toResponse(encuestaRepository.save(encuesta));
    }

    @Transactional(readOnly = true)
    public List<EncuestaEventoResponse> listarPorEvento(Long eventoId) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado"));
        proyectoService.requireOwnerOrAdmin(evento.getProyecto());

        return encuestaRepository.findByAsistenciaEventoIdOrderByEnviadoEnDesc(eventoId).stream()
                .map(encuesta -> {
                    Usuario estudiante = encuesta.getAsistencia().getUsuario();
                    return new EncuestaEventoResponse(
                            encuesta.getId(),
                            eventoId,
                            new UsuarioResumen(estudiante.getId(), usuarioService.nombreVisible(estudiante), usuarioService.emailVisible(estudiante)),
                            encuesta.getSatisfaccion(),
                            encuesta.getUtilidad(),
                            encuesta.getComentarios(),
                            encuesta.getEnviadoEn()
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<EncuestaResponse> buscarPorAsistencia(Long asistenciaId) {
        return encuestaRepository.findByAsistenciaId(asistenciaId).map(this::toResponse);
    }

    @Transactional
    public void eliminarPorAsistencia(Long asistenciaId) {
        encuestaRepository.findByAsistenciaId(asistenciaId).ifPresent(encuestaRepository::delete);
    }

    public EncuestaResponse toResponse(Encuesta encuesta) {
        return new EncuestaResponse(
                encuesta.getId(),
                encuesta.getSatisfaccion(),
                encuesta.getUtilidad(),
                encuesta.getComentarios(),
                encuesta.getEnviadoEn()
        );
    }
}
