package com.autobots.automanager.modelo;

import com.autobots.automanager.entidades.Telefone;
import com.autobots.automanager.dto.Dados.TelefoneDados;
import org.springframework.stereotype.Component;

@Component
public class TelefoneAtualizador {
    public void atualizar(Telefone entidade, TelefoneDados dados) {
        entidade.setDdd(dados.ddd());
        entidade.setNumero(dados.numero());
    }
}
