package com.duoc.cursos.repository;

import com.duoc.cursos.model.Proyecto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProyectoRepository extends JpaRepository<Proyecto, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Proyecto p where p.id = :id")
    java.util.Optional<Proyecto> findForUpdate(@org.springframework.data.repository.query.Param("id") Long id);

    List<Proyecto> findAllByOrderByCreadoEnDesc();

    List<Proyecto> findByResponsableIdOrderByCreadoEnDesc(Long responsableId);
}
