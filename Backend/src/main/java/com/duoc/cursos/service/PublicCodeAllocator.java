package com.duoc.cursos.service;

import com.duoc.cursos.repository.CodigoPublicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;
import java.util.UUID;

@Service
public class PublicCodeAllocator {
    private final CodigoPublicoRepository codes;

    public PublicCodeAllocator(CodigoPublicoRepository codes) { this.codes = codes; }

    @Transactional(propagation = Propagation.MANDATORY)
    public String allocate(String prefix) {
        if (!prefix.equals("PRY") && !prefix.equals("EVT")) throw new IllegalArgumentException("Prefijo no admitido");
        for (int attempt = 0; attempt < 20; attempt++) {
            String candidate = prefix + "-" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 12).toUpperCase(Locale.ROOT);
            // ON CONFLICT no invalida la transacción si otra creación reservó el mismo código.
            if (codes.reservar(candidate) == 1) return candidate;
        }
        throw new IllegalStateException("No se pudo reservar un código público; reintenta la operación");
    }
}
