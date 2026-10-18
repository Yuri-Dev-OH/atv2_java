package com.autobots.automanager.entidades;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Cliente {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 150)
    private String nome;
    @Column(length = 150)
    private String nomeSocial;
    @Column(nullable = false)
    private LocalDate dataNascimento;
    @Column(nullable = false, updatable = false)
    private Instant dataCadastro;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "cliente_id")
    @OrderBy("id")
    private List<Documento> documentos = new ArrayList<>();
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "endereco_id", unique = true)
    private Endereco endereco;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "cliente_id")
    @OrderBy("id")
    private List<Telefone> telefones = new ArrayList<>();
}
