package com.duoc.cursos.repository;

import com.duoc.cursos.model.CodigoPublico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CodigoPublicoRepository extends JpaRepository<CodigoPublico, String> {
    @Modifying
    @Query(value = "INSERT INTO codigos_publicos(codigo) VALUES (:codigo) ON CONFLICT DO NOTHING", nativeQuery = true)
    int reservar(@Param("codigo") String codigo);
}
