package com.autobots.automanager.servicos;

import com.autobots.automanager.dto.Dados.EnderecoDados;
import com.autobots.automanager.dto.Respostas.EnderecoResposta;
import com.autobots.automanager.entidades.Cliente;
import com.autobots.automanager.entidades.Endereco;
import com.autobots.automanager.erros.Conflito;
import com.autobots.automanager.erros.RecursoNaoEncontrado;
import com.autobots.automanager.modelo.ClienteSelecionador;
import com.autobots.automanager.modelo.EnderecoAtualizador;
import com.autobots.automanager.repositorios.ClienteRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EnderecoServico {
    private final ClienteSelecionador clientes;
    private final ClienteRepositorio repositorio;
    private final EnderecoAtualizador atualizador;

    public EnderecoServico(ClienteSelecionador clientes, ClienteRepositorio repositorio, EnderecoAtualizador atualizador) {
        this.clientes = clientes; this.repositorio = repositorio; this.atualizador = atualizador;
    }
    private Endereco selecionar(Cliente cliente) {
        if (cliente.getEndereco() == null) throw new RecursoNaoEncontrado("Endereço não encontrado para este cliente.");
        return cliente.getEndereco();
    }
    public EnderecoResposta obter(long clienteId) { return EnderecoResposta.de(selecionar(clientes.selecionar(clienteId))); }

    @Transactional
    public EnderecoResposta cadastrar(long clienteId, EnderecoDados dados) {
        var cliente = clientes.bloquear(clienteId);
        if (cliente.getEndereco() != null) throw new Conflito("O cliente já possui endereço. Utilize PUT para atualizá-lo.");
        var endereco = new Endereco();
        atualizador.atualizar(endereco, dados);
        cliente.setEndereco(endereco);
        var salvo = repositorio.saveAndFlush(cliente);
        return EnderecoResposta.de(salvo.getEndereco());
    }
    @Transactional
    public EnderecoResposta atualizar(long clienteId, EnderecoDados dados) {
        var endereco = selecionar(clientes.bloquear(clienteId));
        atualizador.atualizar(endereco, dados);
        return EnderecoResposta.de(endereco);
    }
    @Transactional
    public void excluir(long clienteId) {
        var cliente = clientes.bloquear(clienteId);
        selecionar(cliente);
        cliente.setEndereco(null);
    }
}
