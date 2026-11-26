package com.duoc.cursos.service;

import com.duoc.cursos.model.CreacionOperacion;
import com.duoc.cursos.repository.CreacionOperacionRepository;
import com.duoc.cursos.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class CreationIdempotency {
    private final CreacionOperacionRepository operations;
    private final UsuarioRepository users;
    public CreationIdempotency(CreacionOperacionRepository operations, UsuarioRepository users) {
        this.operations = operations; this.users = users;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Long resolve(Long actor, String type, String uuid, String fingerprint, Supplier<Long> create) {
        if (uuid == null) return create.get(); // Contrato anterior de la web.
        try {
            if (!UUID.fromString(uuid).toString().equalsIgnoreCase(uuid)) throw new IllegalArgumentException();
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "clientUuid debe ser un UUID válido");
        }
        String key = uuid.toLowerCase(java.util.Locale.ROOT);
        // Serializa creaciones de la misma cuenta, incluso en distintas instancias del backend.
        users.findForCreation(actor).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        var previous = operations.findByUsuarioIdAndTipoAndClientUuid(actor, type, key);
        if (previous.isPresent()) {
            if (!previous.get().getHuella().equals(fingerprint))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El UUID ya fue utilizado con otros datos");
            return previous.get().getRecursoId();
        }
        Long id = create.get();
        var operation = new CreacionOperacion();
        operation.setUsuarioId(actor); operation.setTipo(type); operation.setClientUuid(key);
        operation.setHuella(fingerprint); operation.setRecursoId(id);
        operations.save(operation);
        return id;
    }

    public static String fingerprint(Object... values) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            for (Object value : values) {
                byte[] bytes = value == null ? null : value.toString().getBytes(StandardCharsets.UTF_8);
                digest.update(ByteBuffer.allocate(4).putInt(bytes == null ? -1 : bytes.length).array());
                if (bytes != null) digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }
}
