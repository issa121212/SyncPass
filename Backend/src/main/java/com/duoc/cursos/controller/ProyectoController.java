package com.duoc.cursos.controller;

import com.duoc.cursos.dto.ApiDtos.ProyectoRequest;
import com.duoc.cursos.dto.ApiDtos.ProyectoResponse;
import com.duoc.cursos.service.ProyectoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/proyectos")
public class ProyectoController {

    private final ProyectoService proyectoService;

    public ProyectoController(ProyectoService proyectoService) {
        this.proyectoService = proyectoService;
    }

    @GetMapping
    public List<ProyectoResponse> listar() {
        return proyectoService.listar();
    }

    @GetMapping("/{id}")
    public ProyectoResponse obtener(@PathVariable Long id) {
        return proyectoService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('DOCENTE','ADMIN')")
    public ProyectoResponse crear(@Valid @RequestBody ProyectoRequest request) {
        return proyectoService.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCENTE','ADMIN')")
    public ProyectoResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProyectoRequest request) {
        return proyectoService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('DOCENTE','ADMIN')")
    public void eliminar(@PathVariable Long id) {
        proyectoService.eliminar(id);
    }
}
