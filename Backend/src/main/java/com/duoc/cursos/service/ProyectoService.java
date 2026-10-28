package com.duoc.cursos.service;

import com.duoc.cursos.dto.ApiDtos.ProyectoRequest;
import com.duoc.cursos.dto.ApiDtos.ProyectoResponse;
import com.duoc.cursos.dto.ApiDtos.UsuarioResumen;
import com.duoc.cursos.model.EstadoProyecto;
import com.duoc.cursos.model.Proyecto;
import com.duoc.cursos.model.Usuario;
import com.duoc.cursos.repository.ProyectoRepository;
import com.duoc.cursos.security.CognitoRoles;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProyectoService {

    private final ProyectoRepository proyectoRepository;
    private final UsuarioService usuarioService;
    private final com.duoc.cursos.repository.EventoRepository eventoRepository;

    public ProyectoService(ProyectoRepository proyectoRepository, UsuarioService usuarioService, com.duoc.cursos.repository.EventoRepository eventoRepository) {
        this.proyectoRepository = proyectoRepository;
        this.usuarioService = usuarioService;
        this.eventoRepository = eventoRepository;
    }

    @Transactional(readOnly = true)
    public List<ProyectoResponse> listar() {
        return proyectoRepository.findAllByOrderByCreadoEnDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProyectoResponse obtener(Long id) {
        return toResponse(require(id));
    }

    @Transactional
    public ProyectoResponse crear(ProyectoRequest request) {
        Usuario actual = usuarioService.getCurrentUsuario();
        Proyecto proyecto = new Proyecto();
        aplicar(proyecto, request);
        proyecto.setResponsable(actual);
        if (proyecto.getEstado() == null) {
            proyecto.setEstado(EstadoProyecto.PLANIFICADO);
        }
        return toResponse(proyectoRepository.save(proyecto));
    }

    @Transactional
    public ProyectoResponse actualizar(Long id, ProyectoRequest request) {
        Proyecto proyecto = require(id);
        requireOwnerOrAdmin(proyecto);
        aplicar(proyecto, request);
        return toResponse(proyectoRepository.save(proyecto));
    }

    @Transactional
    public void eliminar(Long id) {
        Proyecto proyecto = requireParaModificar(id);
        requireOwnerOrAdmin(proyecto);
        if (eventoRepository.existsByProyectoId(id))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar un proyecto que tiene eventos asociados");
        proyectoRepository.delete(proyecto);
    }

    @Transactional(readOnly = true)
    public Proyecto require(Long id) {
        return proyectoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"));
    }

    public Proyecto requireParaModificar(Long id) {
        return proyectoRepository.findForUpdate(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"));
    }

    public void requireOwnerOrAdmin(Proyecto proyecto) {
        Usuario actual = usuarioService.getCurrentUsuario();
        if (actual.getRoles().contains(CognitoRoles.ADMIN)) {
            return;
        }
        if (!proyecto.getResponsable().getId().equals(actual.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el responsable del proyecto puede realizar esta acción");
        }
    }

    public ProyectoResponse toResponse(Proyecto proyecto) {
        Usuario responsable = proyecto.getResponsable();
        return new ProyectoResponse(
                proyecto.getId(),
                proyecto.getNombre(),
                proyecto.getDescripcion(),
                proyecto.getAreaTematica(),
                proyecto.getUnidadVecinal(),
                proyecto.getFechaInicio(),
                proyecto.getFechaFin(),
                proyecto.getEstado(),
                responsable == null ? null : new UsuarioResumen(responsable.getId(), usuarioService.nombreVisible(responsable), usuarioService.emailVisible(responsable)),
                proyecto.getCreadoEn(),
                proyecto.getActualizadoEn()
        );
    }

    private void aplicar(Proyecto proyecto, ProyectoRequest request) {
        if (request.fechaInicio() != null && request.fechaFin() != null &&
                request.fechaFin().isBefore(request.fechaInicio()))
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "La fecha de fin no puede ser anterior a la fecha de inicio");
        proyecto.setNombre(request.nombre().trim());
        proyecto.setDescripcion(request.descripcion());
        proyecto.setAreaTematica(request.areaTematica());
        proyecto.setUnidadVecinal(request.unidadVecinal());
        proyecto.setFechaInicio(request.fechaInicio());
        proyecto.setFechaFin(request.fechaFin());
        if (request.estado() != null) {
            proyecto.setEstado(request.estado());
        }
    }
}
