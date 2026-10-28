package com.duoc.cursos.service;

import com.duoc.cursos.dto.ApiDtos.UsuarioResponse;
import com.duoc.cursos.model.Usuario;
import com.duoc.cursos.repository.UsuarioRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;



import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;


    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;

    }

    /**
     * Consulta pura: el filtro de sesión ya sincronizó la identidad verificada.
     */
    @Transactional(readOnly = true)
    public Usuario getCurrentUsuario() {
        Jwt jwt = currentJwt();
        if (jwt.getIssuer() == null || jwt.getSubject() == null)
            throw new ResponseStatusException(UNAUTHORIZED, "Identidad incompleta");
        return usuarioRepository.findByCognitoSubAndCognitoIssuer(jwt.getSubject(), jwt.getIssuer().toString())
            .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "La sesion requiere sincronizacion"));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse getCurrentResponse() {
        return toResponse(getCurrentUsuario());
    }

    @Transactional
    public UsuarioResponse updateCurrent(String nombre) {
        Usuario usuario = getCurrentUsuario();
        usuario.setNombre(nombre.trim());
        return toResponse(usuarioRepository.save(usuario));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getCognitoSub(),
                usuario.getEmail(),
                nombreVisible(usuario),
                new LinkedHashSet<>(usuario.getRoles())
        );
    }

    public Usuario requireById(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    public com.duoc.cursos.dto.ApiDtos.QrResponse miQr() {
        return new com.duoc.cursos.dto.ApiDtos.QrResponse(qrContenido(getCurrentUsuario()));
    }

    public String qrContenido(Usuario usuario) {
        return "ESTUDIANTE_" + usuario.getId();
    }

    public String emailVisible(Usuario usuario) {
        String email = usuario.getEmail();
        return email != null && !email.endsWith("@cognito.local") ? email : "";
    }

    public String nombreVisible(Usuario usuario) {
        String nombre = usuario.getNombre();
        if (nombre == null || nombre.isBlank() || nombre.equals(usuario.getCognitoSub()) ||
                nombre.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))
            return "Nombre no disponible";
        return nombre;
    }

    private Jwt currentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            return token.getToken();
        }
        throw new ResponseStatusException(UNAUTHORIZED, "Se requiere un token de Cognito");
    }

}
