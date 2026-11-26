package com.duoc.cursos.repository;

import com.duoc.cursos.model.CreacionOperacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CreacionOperacionRepository extends JpaRepository<CreacionOperacion, Long> {
    Optional<CreacionOperacion> findByUsuarioIdAndTipoAndClientUuid(Long usuarioId, String tipo, String uuid);
}
