package com.autobots.automanager.modelo;

import com.autobots.automanager.entidades.Documento;
import com.autobots.automanager.dto.Dados.DocumentoDados;
import org.springframework.stereotype.Component;

@Component
public class DocumentoAtualizador {
    public void atualizar(Documento entidade, DocumentoDados dados) {
        entidade.setTipo(dados.tipo());
        entidade.setNumero(dados.numero());
    }
}
