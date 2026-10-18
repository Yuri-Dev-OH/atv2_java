package com.autobots.automanager.entidades;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Endereco {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(length = 100, nullable = true)
    private String estado;
    @Column(length = 100, nullable = false)
    private String cidade;
    @Column(length = 100, nullable = true)
    private String bairro;
    @Column(length = 150, nullable = false)
    private String rua;
    @Column(length = 20, nullable = false)
    private String numero;
    @Column(length = 8, nullable = true)
    private String codigoPostal;
    @Column(length = 255, nullable = true)
    private String informacoesAdicionais;
}
