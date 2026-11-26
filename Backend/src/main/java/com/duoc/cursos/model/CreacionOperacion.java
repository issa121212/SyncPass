package com.duoc.cursos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "creaciones_operaciones", uniqueConstraints = @UniqueConstraint(
    name = "uk_creacion_actor_tipo_uuid", columnNames = {"usuario_id", "tipo", "client_uuid"}))
@Getter @Setter
public class CreacionOperacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "usuario_id", nullable = false) private Long usuarioId;
    @Column(nullable = false, length = 16) private String tipo;
    @Column(name = "client_uuid", nullable = false, length = 64) private String clientUuid;
    @Column(nullable = false, length = 64) private String huella;
    @Column(name = "recurso_id", nullable = false) private Long recursoId;
}
