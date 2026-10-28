package com.duoc.cursos.repository;

import com.duoc.cursos.model.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {

    Optional<Asistencia> findByEventoIdAndUsuarioId(Long eventoId, Long usuarioId);

    Optional<Asistencia> findByClientUuid(String clientUuid);

    List<Asistencia> findByEventoIdOrderByMarcadoEnDesc(Long eventoId);

    long countByEventoIdAndEsPresenteTrue(Long eventoId);
}
