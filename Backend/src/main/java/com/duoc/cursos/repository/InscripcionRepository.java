package com.duoc.cursos.repository;

import com.duoc.cursos.model.Inscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {

    Optional<Inscripcion> findByEventoIdAndUsuarioId(Long eventoId, Long usuarioId);

    boolean existsByEventoIdAndUsuarioId(Long eventoId, Long usuarioId);

    List<Inscripcion> findByEventoIdOrderByCreadoEnAsc(Long eventoId);

    long countByEventoId(Long eventoId);
}
