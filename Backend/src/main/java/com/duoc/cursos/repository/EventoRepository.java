package com.duoc.cursos.repository;

import com.duoc.cursos.model.Evento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventoRepository extends JpaRepository<Evento, Long> {

    List<Evento> findAllByOrderByFechaHoraAsc();

    List<Evento> findByProyectoIdOrderByFechaHoraAsc(Long proyectoId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from Evento e where e.id = :id")
    java.util.Optional<Evento> findForAttendance(@org.springframework.data.repository.query.Param("id") Long id);

    boolean existsByProyectoId(Long proyectoId);

    boolean existsByQrContenido(String qrContenido);
}
