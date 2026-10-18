package com.autobots.automanager.entidades;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Telefone {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(length = 2, nullable = false)
    private String ddd;
    @Column(length = 9, nullable = false)
    private String numero;
}
