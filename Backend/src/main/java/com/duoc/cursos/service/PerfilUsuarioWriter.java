package com.duoc.cursos.service;

import com.duoc.cursos.model.Usuario;
import com.duoc.cursos.repository.UsuarioRepository;
import com.duoc.cursos.security.CognitoRoles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.Set;

/** Only this command writes profile data. It never calls an external provider. */
@Service
public class PerfilUsuarioWriter {
    private final UsuarioRepository repository;
    public PerfilUsuarioWriter(UsuarioRepository repository) { this.repository = repository; }

    @Transactional
    public Usuario sincronizar(String issuer, String sub, String nombre, String email, Set<String> roles) {
        if (issuer == null || issuer.isBlank() || sub == null || sub.isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identidad incompleta");
        var existing = repository.findByCognitoSubAndCognitoIssuer(sub, issuer);
        // Legacy v1.5 has no issuer. Bind only the exact subject of our already-validated pool.
        if (existing.isEmpty()) {
            var legacy = repository.findByCognitoSub(sub);
            if (legacy.isPresent() && legacy.get().getCognitoIssuer() == null) existing = legacy;
        }
        Usuario user = existing.orElseGet(Usuario::new);
        String selectedEmail = email == null || email.isBlank() ? user.getEmail() : email.trim();
        if (selectedEmail == null) selectedEmail = sub + "@cognito.local";
        var emailOwner = repository.findByEmailIgnoreCase(selectedEmail);
        if (emailOwner.isPresent() && !java.util.Objects.equals(emailOwner.get().getId(), user.getId()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo requiere una vinculacion de cuenta verificada");
        user.setCognitoIssuer(issuer); user.setCognitoSub(sub); user.setEmail(selectedEmail);
        if (nombre != null && !nombre.isBlank()) {
            if (nombre.trim().length() > 160) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "El nombre excede 160 caracteres");
            user.setNombre(nombre.trim());
        }
        if (user.getNombre() == null || user.getNombre().equals(sub)) user.setNombre("Nombre no disponible");
        // Never preserve previously privileged groups after they disappear from the token.
        user.setRoles(new java.util.LinkedHashSet<>(roles.isEmpty() ? Set.of(CognitoRoles.PARTICIPANTE) : roles));
        return repository.saveAndFlush(user);
    }
}
