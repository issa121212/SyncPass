package com.duoc.cursos.service;

import com.duoc.cursos.dto.ApiDtos.*;
import com.duoc.cursos.integration.CognitoAuthGateway;

import com.duoc.cursos.security.CognitoRoles;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.Set;

@Service
public class AuthService {
    private final CognitoAuthGateway gateway;
    private final PerfilUsuarioWriter writer;
    private final UsuarioService usuarioService;
    private final String issuer;
    private final boolean passwordLoginEnabled;
    public AuthService(CognitoAuthGateway gateway, PerfilUsuarioWriter writer, UsuarioService usuarioService,
        @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
        @Value("${app.auth.password-login-enabled:false}") boolean passwordLoginEnabled) {
        this.gateway = gateway; this.writer = writer; this.usuarioService = usuarioService; this.issuer = issuer; this.passwordLoginEnabled = passwordLoginEnabled;
    }
    public UsuarioResponse registrar(String email, String password, String nombre) {
        var account = gateway.registrar(email, password, nombre, Set.of(CognitoRoles.PARTICIPANTE));
        return usuarioService.toResponse(writer.sincronizar(issuer, account.sub(), nombre, email, Set.of(CognitoRoles.PARTICIPANTE)));
    }
    public LoginResponse login(String email, String password) {
        if (!passwordLoginEnabled) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ruta no disponible en este entorno");
        return gateway.login(email, password);
    }
    public void logout(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer "))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Credencial requerida");
        gateway.logout(authorization.substring(7));
    }
    public void solicitarRecuperacionContrasena(String email) { gateway.solicitarRecuperacionContrasena(email); }
    public void confirmarRecuperacionContrasena(String email, String codigo, String password) {
        gateway.confirmarRecuperacionContrasena(email, codigo, password);
    }
}
