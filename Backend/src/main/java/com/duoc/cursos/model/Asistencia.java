package com.duoc.cursos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(
        name = "asistencias",
        uniqueConstraints = @UniqueConstraint(name = "uk_asistencia_evento_usuario", columnNames = {"evento_id", "usuario_id"})
)
@Getter
@Setter
@NoArgsConstructor
public class Asistencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "es_presente", nullable = false)
    private boolean esPresente;

    @Column(name = "marcado_en", nullable = false)
    private Instant marcadoEn;

    private Double latitud;

    private Double longitud;

    @Column(name = "precision_metros")
    private Double precisionMetros;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrigenAsistencia origen = OrigenAsistencia.QR;

    /** UUID generado en el dispositivo para que el reintento offline sea idempotente. */
    @Column(name = "client_uuid", unique = true, length = 64)
    private String clientUuid;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;
}
