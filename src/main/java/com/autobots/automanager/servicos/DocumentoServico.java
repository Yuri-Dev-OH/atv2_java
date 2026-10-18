package com.autobots.automanager.servicos;

import java.util.List;
import com.autobots.automanager.dto.Dados.DocumentoDados;
import com.autobots.automanager.dto.Respostas.DocumentoResposta;
import com.autobots.automanager.entidades.*;
import com.autobots.automanager.erros.*;
import com.autobots.automanager.modelo.ClienteSelecionador;
import com.autobots.automanager.modelo.DocumentoAtualizador;
import com.autobots.automanager.repositorios.ClienteRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DocumentoServico {
    private final ClienteSelecionador clientes;
    private final ClienteRepositorio repositorio;
    private final DocumentoAtualizador atualizador;
    public DocumentoServico(ClienteSelecionador clientes, ClienteRepositorio repositorio, DocumentoAtualizador atualizador) {
        this.clientes = clientes; this.repositorio = repositorio; this.atualizador = atualizador;
    }
    private Documento selecionar(Cliente cliente, long id) {
        return cliente.getDocumentos().stream().filter(item -> item.getId().equals(id)).findFirst()
            .orElseThrow(() -> new RecursoNaoEncontrado("Documento não encontrado para este cliente."));
    }
    public List<DocumentoResposta> listar(long clienteId) {
        return clientes.selecionar(clienteId).getDocumentos().stream().map(DocumentoResposta::de).toList();
    }
    public DocumentoResposta obter(long clienteId, long id) {
        return DocumentoResposta.de(selecionar(clientes.selecionar(clienteId), id));
    }
    @Transactional
    public DocumentoResposta cadastrar(long clienteId, DocumentoDados dados) {
        var cliente = clientes.bloquear(clienteId);
        
        var entidade = new Documento();
        atualizador.atualizar(entidade, dados);
        cliente.getDocumentos().add(entidade);
        var salvo = repositorio.saveAndFlush(cliente);
        return DocumentoResposta.de(salvo.getDocumentos().getLast());
    }
    @Transactional
    public DocumentoResposta atualizar(long clienteId, long id, DocumentoDados dados) {
        var cliente = clientes.bloquear(clienteId);
        var entidade = selecionar(cliente, id);
        
        atualizador.atualizar(entidade, dados);
        repositorio.flush();
        return DocumentoResposta.de(entidade);
    }
    @Transactional
    public void excluir(long clienteId, long id) {
        var cliente = clientes.bloquear(clienteId);
        cliente.getDocumentos().remove(selecionar(cliente, id));
    }
}
