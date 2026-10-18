package com.autobots.automanager.entidades;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Documento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(length = 30, nullable = false)
    private String tipo;
    @Column(length = 50, nullable = false, unique = true)
    private String numero;
}
