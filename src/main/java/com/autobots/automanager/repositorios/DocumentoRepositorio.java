package com.autobots.automanager.repositorios;

import com.autobots.automanager.entidades.Documento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentoRepositorio extends JpaRepository<Documento, Long> {}
