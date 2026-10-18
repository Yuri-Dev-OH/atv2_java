package com.autobots.automanager.servicos;

import java.util.List;
import com.autobots.automanager.dto.Dados.TelefoneDados;
import com.autobots.automanager.dto.Respostas.TelefoneResposta;
import com.autobots.automanager.entidades.*;
import com.autobots.automanager.erros.*;
import com.autobots.automanager.modelo.ClienteSelecionador;
import com.autobots.automanager.modelo.TelefoneAtualizador;
import com.autobots.automanager.repositorios.ClienteRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TelefoneServico {
    private final ClienteSelecionador clientes;
    private final ClienteRepositorio repositorio;
    private final TelefoneAtualizador atualizador;
    public TelefoneServico(ClienteSelecionador clientes, ClienteRepositorio repositorio, TelefoneAtualizador atualizador) {
        this.clientes = clientes; this.repositorio = repositorio; this.atualizador = atualizador;
    }
    private Telefone selecionar(Cliente cliente, long id) {
        return cliente.getTelefones().stream().filter(item -> item.getId().equals(id)).findFirst()
            .orElseThrow(() -> new RecursoNaoEncontrado("Telefone não encontrado para este cliente."));
    }
    public List<TelefoneResposta> listar(long clienteId) {
        return clientes.selecionar(clienteId).getTelefones().stream().map(TelefoneResposta::de).toList();
    }
    public TelefoneResposta obter(long clienteId, long id) {
        return TelefoneResposta.de(selecionar(clientes.selecionar(clienteId), id));
    }
    @Transactional
    public TelefoneResposta cadastrar(long clienteId, TelefoneDados dados) {
        var cliente = clientes.bloquear(clienteId);
        verificarDuplicado(cliente, dados, null);
        var entidade = new Telefone();
        atualizador.atualizar(entidade, dados);
        cliente.getTelefones().add(entidade);
        var salvo = repositorio.saveAndFlush(cliente);
        return TelefoneResposta.de(salvo.getTelefones().getLast());
    }
    @Transactional
    public TelefoneResposta atualizar(long clienteId, long id, TelefoneDados dados) {
        var cliente = clientes.bloquear(clienteId);
        var entidade = selecionar(cliente, id);
        verificarDuplicado(cliente, dados, id);
        atualizador.atualizar(entidade, dados);
        repositorio.flush();
        return TelefoneResposta.de(entidade);
    }
    @Transactional
    public void excluir(long clienteId, long id) {
        var cliente = clientes.bloquear(clienteId);
        cliente.getTelefones().remove(selecionar(cliente, id));
    }
    private void verificarDuplicado(Cliente cliente, TelefoneDados dados, Long ignorarId) {
        boolean duplicado = cliente.getTelefones().stream().anyMatch(t ->
            !t.getId().equals(ignorarId) && t.getDdd().equals(dados.ddd()) && t.getNumero().equals(dados.numero()));
        if (duplicado) throw new Conflito("Telefone já cadastrado para este cliente.");
    }
}
