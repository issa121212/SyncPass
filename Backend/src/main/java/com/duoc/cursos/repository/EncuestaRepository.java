package com.duoc.cursos.repository;

import com.duoc.cursos.model.Encuesta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EncuestaRepository extends JpaRepository<Encuesta, Long> {

    Optional<Encuesta> findByAsistenciaId(Long asistenciaId);

    List<Encuesta> findByAsistenciaEventoIdOrderByEnviadoEnDesc(Long eventoId);
}
