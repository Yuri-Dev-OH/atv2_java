package com.autobots.automanager.modelo;

import com.autobots.automanager.entidades.Endereco;
import com.autobots.automanager.dto.Dados.EnderecoDados;
import org.springframework.stereotype.Component;

@Component
public class EnderecoAtualizador {
    public void atualizar(Endereco entidade, EnderecoDados dados) {
        entidade.setEstado(dados.estado());
        entidade.setCidade(dados.cidade());
        entidade.setBairro(dados.bairro());
        entidade.setRua(dados.rua());
        entidade.setNumero(dados.numero());
        entidade.setCodigoPostal(dados.codigoPostal());
        entidade.setInformacoesAdicionais(dados.informacoesAdicionais());
    }
}
