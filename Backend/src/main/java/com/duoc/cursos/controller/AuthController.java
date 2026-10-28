package com.duoc.cursos.controller;

import com.duoc.cursos.dto.ApiDtos.ConfirmarPasswordRequest;
import com.duoc.cursos.dto.ApiDtos.LoginRequest;
import com.duoc.cursos.dto.ApiDtos.LoginResponse;
import com.duoc.cursos.dto.ApiDtos.RegistroRequest;
import com.duoc.cursos.dto.ApiDtos.SolicitudPasswordRequest;
import com.duoc.cursos.dto.ApiDtos.UsuarioResponse;

import com.duoc.cursos.service.AuthService;
import com.duoc.cursos.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UsuarioService usuarioService;

    public AuthController(AuthService authService, UsuarioService usuarioService) {
        this.authService = authService;
        this.usuarioService = usuarioService;
    }

    /**
     * Registro público. Siempre crea PARTICIPANTE: los docentes se crean desde
     * Cognito o por un administrador, nunca por este endpoint.
     */
    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegistroRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(req.email(), req.password(), req.nombre()));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req.email(), req.password()));
    }

    @GetMapping("/me")
    public UsuarioResponse me() {
        return usuarioService.getCurrentResponse();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String bearerToken) {
        authService.logout(bearerToken);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/olvide-contrasena/solicitar")
    public ResponseEntity<Void> solicitarPassword(@Valid @RequestBody SolicitudPasswordRequest req) {
        authService.solicitarRecuperacionContrasena(req.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/olvide-contrasena/confirmar")
    public ResponseEntity<Void> confirmarPassword(@Valid @RequestBody ConfirmarPasswordRequest req) {
        authService.confirmarRecuperacionContrasena(req.email(), req.codigo(), req.nuevaContrasena());
        return ResponseEntity.ok().build();
    }
}
