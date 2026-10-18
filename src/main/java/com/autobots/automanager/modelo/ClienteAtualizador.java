package com.autobots.automanager.modelo;

import com.autobots.automanager.dto.Dados.ClienteDados;
import com.autobots.automanager.entidades.Cliente;
import org.springframework.stereotype.Component;

@Component
public class ClienteAtualizador {
    public void atualizar(Cliente cliente, ClienteDados dados) {
        cliente.setNome(dados.nome());
        cliente.setNomeSocial(dados.nomeSocial());
        cliente.setDataNascimento(dados.dataNascimento());
    }
}
