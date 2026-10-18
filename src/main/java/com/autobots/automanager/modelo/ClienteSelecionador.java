package com.autobots.automanager.modelo;

import com.autobots.automanager.entidades.Cliente;
import com.autobots.automanager.repositorios.ClienteRepositorio;
import com.autobots.automanager.erros.RecursoNaoEncontrado;
import org.springframework.stereotype.Component;

@Component
public class ClienteSelecionador {
    private final ClienteRepositorio repositorio;
    public ClienteSelecionador(ClienteRepositorio repositorio) { this.repositorio = repositorio; }
    public Cliente selecionar(long id) {
        return repositorio.findById(id).orElseThrow(() -> new RecursoNaoEncontrado("Cliente não encontrado."));
    }
    public Cliente bloquear(long id) {
        return repositorio.bloquearPorId(id).orElseThrow(() -> new RecursoNaoEncontrado("Cliente não encontrado."));
    }
}
