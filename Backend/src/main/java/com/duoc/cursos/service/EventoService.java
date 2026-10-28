package com.duoc.cursos.service;

import com.duoc.cursos.dto.ApiDtos.AsistenciaRequest;
import com.duoc.cursos.dto.ApiDtos.AsistenciaResponse;
import com.duoc.cursos.dto.ApiDtos.AsistenteResponse;
import com.duoc.cursos.dto.ApiDtos.EncuestaResponse;
import com.duoc.cursos.dto.ApiDtos.EventoRequest;
import com.duoc.cursos.dto.ApiDtos.EventoResponse;
import com.duoc.cursos.dto.ApiDtos.InscribirseRequest;
import com.duoc.cursos.dto.ApiDtos.QrResponse;
import com.duoc.cursos.dto.ApiDtos.UsuarioResumen;
import com.duoc.cursos.model.*;
import com.duoc.cursos.repository.AsistenciaRepository;
import com.duoc.cursos.repository.EventoRepository;
import com.duoc.cursos.repository.InscripcionRepository;
import com.duoc.cursos.security.CognitoRoles;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class EventoService {

    private final EventoRepository eventoRepository;
    private final InscripcionRepository inscripcionRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final ProyectoService proyectoService;
    private final UsuarioService usuarioService;
    private final EncuestaService encuestaService;

    public EventoService(
            EventoRepository eventoRepository,
            InscripcionRepository inscripcionRepository,
            AsistenciaRepository asistenciaRepository,
            ProyectoService proyectoService,
            UsuarioService usuarioService,
            EncuestaService encuestaService
    ) {
        this.eventoRepository = eventoRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.proyectoService = proyectoService;
        this.usuarioService = usuarioService;
        this.encuestaService = encuestaService;
    }

    @Transactional(readOnly = true)
    public List<EventoResponse> listar(Long proyectoId) {
        List<Evento> eventos = proyectoId == null
                ? eventoRepository.findAllByOrderByFechaHoraAsc()
                : eventoRepository.findByProyectoIdOrderByFechaHoraAsc(proyectoId);
        Usuario actual = usuarioService.getCurrentUsuario();
        return eventos.stream().map(evento -> toResponse(evento, actual)).toList();
    }

    @Transactional(readOnly = true)
    public EventoResponse obtener(Long id) {
        Evento evento = require(id);
        return toResponse(evento, usuarioService.getCurrentUsuario());
    }

    @Transactional
    public EventoResponse crear(EventoRequest request) {
        Proyecto proyecto = proyectoService.requireParaModificar(request.proyectoId());
        proyectoService.requireOwnerOrAdmin(proyecto);

        Evento evento = new Evento();
        evento.setProyecto(proyecto);
        aplicar(evento, request);
        evento = eventoRepository.save(evento);
        evento.setQrContenido("EVENTO_" + evento.getId());
        evento = eventoRepository.save(evento);
        return toResponse(evento, usuarioService.getCurrentUsuario());
    }

    @Transactional
    public EventoResponse actualizar(Long id, EventoRequest request) {
        Evento evento = require(id);
        proyectoService.requireOwnerOrAdmin(evento.getProyecto());

        Proyecto proyecto = proyectoService.requireParaModificar(request.proyectoId());
        proyectoService.requireOwnerOrAdmin(proyecto);
        evento.setProyecto(proyecto);
        aplicar(evento, request);
        return toResponse(eventoRepository.save(evento), usuarioService.getCurrentUsuario());
    }

    @Transactional
    public void eliminar(Long id) {
        Evento initial = require(id);
        proyectoService.requireParaModificar(initial.getProyecto().getId());
        Evento evento = eventoRepository.findForAttendance(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado"));
        proyectoService.requireOwnerOrAdmin(evento.getProyecto());
        if (inscripcionRepository.countByEventoId(id) > 0 || asistenciaRepository.countByEventoIdAndEsPresenteTrue(id) > 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El evento tiene historial; debe cancelarse, no eliminarse");
        eventoRepository.delete(evento);
    }

    @Transactional(readOnly = true)
    public QrResponse qr(Long id) {
        Evento evento = require(id);
        proyectoService.requireOwnerOrAdmin(evento.getProyecto());
        return new QrResponse(evento.getQrContenido());
    }

    /**
     * Inscripción de un estudiante usando el QR del evento. Es idempotente y
     * crea una asistencia en falso para que el estudiante aparezca en la lista.
     */
    @Transactional
    public EventoResponse inscribirse(InscribirseRequest request) {
        Usuario actual = usuarioService.getCurrentUsuario();
        Long eventoId = parseQr(request.codigoQr(), "EVENTO_");
        Evento evento = eventoRepository.findForAttendance(eventoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado"));

        if (evento.getEstado() == EstadoEvento.CANCELADO || evento.getEstado() == EstadoEvento.FINALIZADO) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "El evento está cancelado");
        }

        Optional<Inscripcion> existente = inscripcionRepository.findByEventoIdAndUsuarioId(evento.getId(), actual.getId());
        if (existente.isPresent()) {
            return toResponse(evento, actual);
        }

        validarCupo(evento);
        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setEvento(evento);
        inscripcion.setUsuario(actual);
        inscripcion.setEstado(EstadoInscripcion.INSCRITO);
        inscripcionRepository.save(inscripcion);

        if (asistenciaRepository.findByEventoIdAndUsuarioId(evento.getId(), actual.getId()).isEmpty()) {
            Asistencia asistencia = new Asistencia();
            asistencia.setEvento(evento);
            asistencia.setUsuario(actual);
            asistencia.setEsPresente(false);
            asistencia.setMarcadoEn(Instant.now());
            asistencia.setOrigen(OrigenAsistencia.MANUAL);
            asistenciaRepository.save(asistencia);
        }
        return toResponse(evento, actual);
    }

    @Transactional(readOnly = true)
    public List<AsistenteResponse> asistentes(Long eventoId) {
        Evento evento = require(eventoId);
        requireScanner(evento);

        return inscripcionRepository.findByEventoIdOrderByCreadoEnAsc(eventoId).stream()
                .map(inscripcion -> {
                    Usuario estudiante = inscripcion.getUsuario();
                    Optional<Asistencia> asistencia = asistenciaRepository.findByEventoIdAndUsuarioId(eventoId, estudiante.getId());
                    EncuestaResponse encuesta = asistencia
                            .flatMap(a -> encuestaService.buscarPorAsistencia(a.getId()))
                            .orElse(null);
                    return new AsistenteResponse(
                            estudiante.getId(),
                            usuarioService.nombreVisible(estudiante),
                            usuarioService.emailVisible(estudiante),
                            true,
                            asistencia.map(Asistencia::isEsPresente).orElse(false),
                            asistencia.map(Asistencia::getMarcadoEn).orElse(null),
                            encuesta
                    );
                })
                .toList();
    }

    /**
     * Escaneo del QR personal del estudiante por parte del docente. Si el
     * estudiante no estaba inscrito, la asistencia lo inscribe.
     */
    @Transactional
    public AsistenciaResponse escanear(Long eventoId, AsistenciaRequest request) {
        Evento evento = eventoRepository.findForAttendance(eventoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado"));
        Usuario receptor = requireScanner(evento);
        Long estudianteId = participanteDelQr(eventoId, request.codigoQr());
        Usuario estudiante = usuarioService.requireById(estudianteId);
        if (!estudiante.getRoles().contains(CognitoRoles.PARTICIPANTE))
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "El QR debe pertenecer a un participante");
        if (receptor.getId().equals(estudianteId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes registrar tu propia asistencia");
        if (request.clientUuid() != null && !request.clientUuid().isBlank()) {
            var previous = asistenciaRepository.findByClientUuid(request.clientUuid());
            if (previous.isPresent()) {
                var attendance = previous.get();
                if (!attendance.getEvento().getId().equals(eventoId) ||
                        !attendance.getUsuario().getId().equals(estudianteId))
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "La operacion corresponde a otro registro");
                return toAsistenciaResponse(attendance);
            }
        }
        var existing = asistenciaRepository.findByEventoIdAndUsuarioId(eventoId, estudianteId);
        if (existing.isPresent() && existing.get().isEsPresente())
            return toAsistenciaResponse(existing.get());
        if (evento.getEstado() == EstadoEvento.CANCELADO || evento.getEstado() == EstadoEvento.FINALIZADO)
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "El evento no admite nuevas asistencias");

        Inscripcion inscripcion = inscripcionRepository.findByEventoIdAndUsuarioId(eventoId, estudianteId)
                .orElse(null);
        if (inscripcion == null) {
            validarCupo(evento);
            inscripcion = new Inscripcion();
            inscripcion.setEvento(evento);
            inscripcion.setUsuario(estudiante);
        }
        inscripcion.setEstado(EstadoInscripcion.ASISTIO);
        inscripcionRepository.save(inscripcion);

        Asistencia asistencia = asistenciaRepository.findByEventoIdAndUsuarioId(eventoId, estudianteId)
                .orElseGet(Asistencia::new);
        asistencia.setEvento(evento);
        asistencia.setUsuario(estudiante);
        asistencia.setEsPresente(true);
        asistencia.setMarcadoEn(request.marcadoEn() == null ? Instant.now() : request.marcadoEn());
        asistencia.setLatitud(request.latitud());
        asistencia.setLongitud(request.longitud());
        asistencia.setPrecisionMetros(request.precisionMetros());
        asistencia.setOrigen(OrigenAsistencia.QR);
        if (request.clientUuid() != null && !request.clientUuid().isBlank()) {
            asistencia.setClientUuid(request.clientUuid());
        }
        return toAsistenciaResponse(asistenciaRepository.save(asistencia));
    }

    @Transactional(readOnly = true)
    public AsistenciaResponse miAsistencia(Long eventoId) {
        Usuario actual = usuarioService.getCurrentUsuario();
        require(eventoId);
        Asistencia asistencia = asistenciaRepository.findByEventoIdAndUsuarioId(eventoId, actual.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No tienes asistencia registrada para este evento"));
        return toAsistenciaResponse(asistencia);
    }

    @Transactional
    public AsistenciaResponse actualizarMiAsistencia(Long eventoId, AsistenciaRequest request) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "La asistencia solo se confirma mediante el escaneo de un receptor autorizado");
    }

    @Transactional(readOnly = true)
    public Evento require(Long id) {
        return eventoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado"));
    }

    public EventoResponse toResponse(Evento evento, Usuario actual) {
        long inscritos = inscripcionRepository.countByEventoId(evento.getId());
        long presentes = asistenciaRepository.countByEventoIdAndEsPresenteTrue(evento.getId());

        boolean inscrito = false;
        boolean esPresente = false;
        boolean encuestaRespondida = false;
        if (actual != null) {
            inscrito = inscripcionRepository.existsByEventoIdAndUsuarioId(evento.getId(), actual.getId());
            Optional<Asistencia> asistencia = asistenciaRepository.findByEventoIdAndUsuarioId(evento.getId(), actual.getId());
            esPresente = asistencia.map(Asistencia::isEsPresente).orElse(false);
            encuestaRespondida = asistencia
                    .flatMap(a -> encuestaService.buscarPorAsistencia(a.getId()))
                    .isPresent();
        }

        return new EventoResponse(
                evento.getId(),
                evento.getProyecto().getId(),
                evento.getProyecto().getNombre(),
                evento.getNombre(),
                evento.getDescripcion(),
                evento.getFechaHora(),
                evento.getCupos(),
                inscritos,
                presentes,
                evento.getRecinto(),
                evento.getDireccion(),
                evento.getLatitud(),
                evento.getLongitud(),
                evento.getRadioMetros(),
                evento.getQrContenido(),
                evento.getEstado(),
                evento.getCreadoEn(),
                evento.getActualizadoEn(),
                inscrito,
                esPresente,
                encuestaRespondida
        );
    }

    public AsistenciaResponse toAsistenciaResponse(Asistencia asistencia) {
        Usuario estudiante = asistencia.getUsuario();
        EncuestaResponse encuesta = encuestaService.buscarPorAsistencia(asistencia.getId()).orElse(null);
        return new AsistenciaResponse(
                asistencia.getId(),
                asistencia.getEvento().getId(),
                new UsuarioResumen(estudiante.getId(), usuarioService.nombreVisible(estudiante), usuarioService.emailVisible(estudiante)),
                asistencia.isEsPresente(),
                asistencia.getMarcadoEn(),
                asistencia.getLatitud(),
                asistencia.getLongitud(),
                asistencia.getPrecisionMetros(),
                asistencia.getOrigen(),
                encuesta
        );
    }

    private Usuario requireScanner(Evento evento) {
        Usuario actual = usuarioService.getCurrentUsuario();
        if (actual.getRoles().contains(CognitoRoles.ADMIN) ||
                actual.getRoles().contains(CognitoRoles.VOLUNTARIO) ||
                actual.getRoles().contains(CognitoRoles.ESTUDIANTE)) return actual;
        if (actual.getRoles().contains(CognitoRoles.DOCENTE)) {
            proyectoService.requireOwnerOrAdmin(evento.getProyecto());
            return actual;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo recepcion o el docente responsable pueden escanear");
    }

    private Long participanteDelQr(Long eventoId, String qr) {
        if (qr != null && qr.startsWith("SYNC_PASS:ASISTENCIA:")) {
            String[] partes = qr.split(":");
            if (partes.length != 4) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "QR invalido");
            try {
                if (!eventoId.equals(Long.valueOf(partes[2])))
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "El QR corresponde a otro evento");
                return Long.valueOf(partes[3]);
            } catch (NumberFormatException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "QR invalido");
            }
        }
        return parseQr(qr, "ESTUDIANTE_"); // Existing personal credentials remain readable.
    }

    private void aplicar(Evento evento, EventoRequest request) {
        if ((request.latitud() == null) != (request.longitud() == null))
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Completa latitud y longitud juntas");
        if (evento.getId() != null && request.cupos() != null && request.cupos() > 0 &&
                request.cupos() < inscripcionRepository.countByEventoId(evento.getId()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El cupo no puede ser menor que las inscripciones existentes");
        evento.setNombre(request.nombre().trim());
        evento.setDescripcion(request.descripcion());
        evento.setFechaHora(request.fechaHora());
        evento.setCupos(request.cupos() == null ? 0 : request.cupos());
        evento.setRecinto(request.recinto());
        evento.setDireccion(request.direccion());
        evento.setLatitud(request.latitud());
        evento.setLongitud(request.longitud());
        evento.setRadioMetros(request.radioMetros() == null ? 200 : request.radioMetros());
        if (request.estado() != null) {
            evento.setEstado(request.estado());
        }
    }

    private void validarCupo(Evento evento) {
        if (evento.getCupos() != null && evento.getCupos() > 0
                && inscripcionRepository.countByEventoId(evento.getId()) >= evento.getCupos()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "El evento no tiene cupos disponibles");
        }
    }

    private Long parseQr(String contenido, String prefijo) {
        if (contenido == null || !contenido.startsWith(prefijo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código QR inválido. Se esperaba " + prefijo + "{id}");
        }
        try {
            return Long.parseLong(contenido.substring(prefijo.length()).trim());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El ID del código QR no es numérico");
        }
    }
}
