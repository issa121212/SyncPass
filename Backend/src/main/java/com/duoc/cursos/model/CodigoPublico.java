package com.duoc.cursos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

/** Reserva permanente: un código visible nunca se reutiliza tras borrar el recurso. */
@Entity
@Table(name = "codigos_publicos")
@NoArgsConstructor
public class CodigoPublico {
    @Id
    @Column(length = 16, nullable = false, updatable = false)
    private String codigo;
}
