package com.duoc.cursos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(
        name = "encuestas",
        uniqueConstraints = @UniqueConstraint(name = "uk_encuesta_asistencia", columnNames = "asistencia_id")
)
@Getter
@Setter
@NoArgsConstructor
public class Encuesta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asistencia_id", nullable = false, unique = true)
    private Asistencia asistencia;

    @Column(nullable = false)
    private Integer satisfaccion;

    @Column(nullable = false)
    private Integer utilidad;

    @Column(columnDefinition = "text")
    private String comentarios;

    @Column(name = "enviado_en", nullable = false)
    private Instant enviadoEn;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;
}
