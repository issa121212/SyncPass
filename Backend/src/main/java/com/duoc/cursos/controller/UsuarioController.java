package com.duoc.cursos.controller;

import com.duoc.cursos.dto.ApiDtos.ActualizarPerfilRequest;
import com.duoc.cursos.dto.ApiDtos.QrResponse;
import com.duoc.cursos.dto.ApiDtos.UsuarioResponse;

import com.duoc.cursos.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/me")
    public UsuarioResponse me() {
        return usuarioService.getCurrentResponse();
    }

    @PutMapping("/me")
    public UsuarioResponse actualizar(@Valid @RequestBody ActualizarPerfilRequest request) {
        return usuarioService.updateCurrent(request.nombre());
    }

    @GetMapping("/me/qr")
    @PreAuthorize("hasRole('PARTICIPANTE')")
    public QrResponse qr() {
        return usuarioService.miQr();
    }
}
